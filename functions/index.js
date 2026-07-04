const crypto = require("crypto");
const { onValueCreated } = require("firebase-functions/v2/database");
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { defineSecret, defineString } = require("firebase-functions/params");
const { initializeApp } = require("firebase-admin/app");
const { getDatabase } = require("firebase-admin/database");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

// OCI API 인증 정보 (functions/.env + Secret Manager)
const ociTenancyOcid = defineString("OCI_TENANCY_OCID");
const ociUserOcid = defineString("OCI_USER_OCID");
const ociKeyFingerprint = defineString("OCI_KEY_FINGERPRINT");
const ociRegion = defineString("OCI_REGION");
const ociNamespace = defineString("OCI_NAMESPACE");
const ociBucket = defineString("OCI_BUCKET");
const ociPrivateKey = defineSecret("OCI_PRIVATE_KEY");

/**
 * OCI REST API 요청에 draft-cavage HTTP Signature 서명을 붙인다.
 * https://docs.oracle.com/en-us/iaas/Content/API/Concepts/signingrequests.htm
 */
function signOciRequest({ method, host, path, body, keyId, privateKeyPem }) {
  const date = new Date().toUTCString();
  const bodyStr = JSON.stringify(body);
  const contentSha256 = crypto
    .createHash("sha256")
    .update(bodyStr)
    .digest("base64");
  const contentLength = Buffer.byteLength(bodyStr);

  const signingString = [
    `(request-target): ${method.toLowerCase()} ${path}`,
    `host: ${host}`,
    `date: ${date}`,
    `x-content-sha256: ${contentSha256}`,
    `content-type: application/json`,
    `content-length: ${contentLength}`,
  ].join("\n");

  const signature = crypto
    .createSign("RSA-SHA256")
    .update(signingString)
    .sign(privateKeyPem, "base64");

  const authorization =
    `Signature version="1",keyId="${keyId}",algorithm="rsa-sha256",` +
    `headers="(request-target) host date x-content-sha256 content-type content-length",` +
    `signature="${signature}"`;

  return {
    headers: {
      date: date,
      host: host,
      "x-content-sha256": contentSha256,
      "content-type": "application/json",
      "content-length": String(contentLength),
      authorization: authorization,
    },
    bodyStr,
  };
}

// 업로드를 허용할 오브젝트 경로: <종류>/<groupId>/<파일명>.<확장자>
const OBJECT_NAME_PATTERN =
  /^(voices|images|videos|diaries)\/([A-Za-z0-9_-]{1,64})\/([A-Za-z0-9_-]{1,64})\.(m4a|jpg|jpeg|png|webp|mp4|webm|3gp)$/;

/**
 * 단일 오브젝트에 대해 10분짜리 쓰기 전용 PAR(업로드 URL)을 발급한다.
 * 호출자가 해당 그룹의 멤버인지 확인한 뒤 발급하므로,
 * 앱에 쓰기 권한 PAR을 내장하지 않아도 된다.
 */
exports.createUploadUrl = onCall(
  // maxInstances: 폭주 호출 시에도 동시 서버 수를 제한해 비용 상한을 만든다
  { region: "asia-northeast3", secrets: [ociPrivateKey], maxInstances: 5 },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "로그인이 필요합니다.");
    }

    const objectName = request.data && request.data.objectName;
    if (typeof objectName !== "string" || !OBJECT_NAME_PATTERN.test(objectName)) {
      throw new HttpsError("invalid-argument", "허용되지 않는 오브젝트 이름입니다.");
    }

    // 오브젝트 경로의 groupId에 대해 그룹 멤버십 검증
    const groupId = objectName.split("/")[1];
    const uid = request.auth.uid;
    const memberSnap = await getDatabase()
      .ref(`groups/${groupId}/members/${uid}`)
      .once("value");
    if (!memberSnap.exists()) {
      throw new HttpsError("permission-denied", "그룹 멤버가 아닙니다.");
    }

    const host = `objectstorage.${ociRegion.value()}.oraclecloud.com`;
    const path = `/n/${ociNamespace.value()}/b/${ociBucket.value()}/p/`;
    const body = {
      name: `upload-${uid}-${Date.now()}`,
      objectName: objectName,
      accessType: "ObjectWrite",
      timeExpires: new Date(Date.now() + 10 * 60 * 1000).toISOString(),
    };
    const keyId = `${ociTenancyOcid.value()}/${ociUserOcid.value()}/${ociKeyFingerprint.value()}`;

    const { headers, bodyStr } = signOciRequest({
      method: "POST",
      host: host,
      path: path,
      body: body,
      keyId: keyId,
      privateKeyPem: ociPrivateKey.value(),
    });

    const response = await fetch(`https://${host}${path}`, {
      method: "POST",
      headers: headers,
      body: bodyStr,
    });

    if (!response.ok) {
      const text = await response.text();
      console.error(`PAR 생성 실패: ${response.status} ${text}`);
      throw new HttpsError("internal", "업로드 URL 생성에 실패했습니다.");
    }

    const par = await response.json();
    return { uploadUrl: `https://${host}${par.accessUri}` };
  }
);

/**
 * 발신자를 제외한 그룹 멤버들의 FCM 토큰을 수집한다.
 * @returns {Promise<Array<{uid: string, token: string}>>}
 */
async function collectMemberTokens(db, groupId, senderId) {
  const membersSnap = await db.ref(`groups/${groupId}/members`).once("value");
  const members = membersSnap.val();
  if (!members) return [];

  const entries = await Promise.all(
    Object.keys(members)
      .filter((uid) => uid !== senderId)
      .map(async (uid) => {
        const tokenSnap = await db.ref(`users/${uid}/fcmToken`).once("value");
        return { uid, token: tokenSnap.val() };
      })
  );
  return entries.filter((entry) => entry.token);
}

/**
 * 멤버들에게 푸시를 발송하고, 만료된 토큰은 DB에서 정리한다.
 */
async function sendToMembers(db, memberTokens, payload) {
  if (memberTokens.length === 0) return;

  const messaging = getMessaging();
  const response = await messaging.sendEachForMulticast({
    tokens: memberTokens.map((entry) => entry.token),
    ...payload,
  });

  const cleanups = [];
  response.responses.forEach((resp, idx) => {
    if (
      resp.error &&
      (resp.error.code === "messaging/invalid-registration-token" ||
        resp.error.code === "messaging/registration-token-not-registered")
    ) {
      const { uid } = memberTokens[idx];
      cleanups.push(db.ref(`users/${uid}/fcmToken`).remove());
    }
  });
  await Promise.all(cleanups);

  return response;
}

/**
 * 새 메시지가 생성되면 같은 그룹의 다른 멤버들에게 푸시 알림을 보낸다.
 */
exports.onNewMessage = onValueCreated(
  {
    ref: "/messages/{groupId}/{messageId}",
    // RTDB 트리거는 DB 인스턴스와 같은 리전이어야 함 (RTDB는 서울 미지원 → 싱가포르)
    region: "asia-southeast1",
    maxInstances: 5,
  },
  async (event) => {
    const message = event.data.val();
    const groupId = event.params.groupId;
    const senderId = message.senderId;
    const senderName = message.senderName || "가족";
    const messageType = message.type || "text";
    const content = message.content || "";

    // 알림 본문 생성 (미디어 타입은 URL 대신 안내 문구)
    let body;
    switch (messageType) {
      case "voice":
        body = "음성 메시지를 보냈어요";
        break;
      case "image":
        body = "사진을 보냈어요";
        break;
      case "video":
        body = "영상을 보냈어요";
        break;
      default:
        body = content.length > 50 ? content.substring(0, 50) + "..." : content;
    }

    const db = getDatabase();
    const memberTokens = await collectMemberTokens(db, groupId, senderId);

    const response = await sendToMembers(db, memberTokens, {
      notification: {
        title: "가족 채팅",
        body: `${senderName}: ${body}`,
      },
      data: {
        title: "가족 채팅",
        body: messageType === "text" ? content : body,
        senderName: senderName,
        messageType: messageType,
        groupId: groupId,
      },
    });

    if (response) {
      console.log(
        `Sent ${response.successCount} notifications for group ${groupId}`
      );
    }
  }
);

/**
 * 새 영상일기가 올라오면 같은 그룹의 다른 멤버들에게 푸시 알림을 보낸다.
 */
exports.onNewDiary = onValueCreated(
  {
    ref: "/diaries/{groupId}/{diaryId}",
    // RTDB 트리거는 DB 인스턴스와 같은 리전이어야 함
    region: "asia-southeast1",
    maxInstances: 5,
  },
  async (event) => {
    const diary = event.data.val();
    const groupId = event.params.groupId;
    const senderId = diary.senderId;
    const senderName = diary.senderName || "가족";

    const db = getDatabase();
    const memberTokens = await collectMemberTokens(db, groupId, senderId);

    const response = await sendToMembers(db, memberTokens, {
      notification: {
        title: "영상일기",
        body: `${senderName}님이 영상일기를 올렸어요`,
      },
      data: {
        title: "영상일기",
        body: "영상일기를 올렸어요",
        senderName: senderName,
        messageType: "diary",
        groupId: groupId,
      },
    });

    if (response) {
      console.log(
        `Sent ${response.successCount} diary notifications for group ${groupId}`
      );
    }
  }
);
