const { onValueCreated } = require("firebase-functions/v2/database");
const { initializeApp } = require("firebase-admin/app");
const { getDatabase } = require("firebase-admin/database");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

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
