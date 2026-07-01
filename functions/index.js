const { onValueCreated } = require("firebase-functions/v2/database");
const { initializeApp } = require("firebase-admin/app");
const { getDatabase } = require("firebase-admin/database");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

/**
 * 새 메시지가 생성되면 같은 그룹의 다른 멤버들에게 푸시 알림을 보낸다.
 */
exports.onNewMessage = onValueCreated(
  { ref: "/messages/{groupId}/{messageId}", region: "asia-northeast3" },
  async (event) => {
    const message = event.data.val();
    const groupId = event.params.groupId;
    const senderId = message.senderId;
    const senderName = message.senderName || "가족";
    const messageType = message.type || "text";
    const content = message.content || "";

    // 알림 본문 생성
    let body;
    switch (messageType) {
      case "voice":
        body = "음성 메시지를 보냈어요";
        break;
      case "image":
        body = "사진을 보냈어요";
        break;
      default:
        body = content.length > 50 ? content.substring(0, 50) + "..." : content;
    }

    const db = getDatabase();

    // 그룹 멤버 조회
    const membersSnap = await db
      .ref(`groups/${groupId}/members`)
      .once("value");
    const members = membersSnap.val();
    if (!members) return;

    // 발신자를 제외한 멤버의 FCM 토큰 수집
    const tokens = [];
    for (const uid of Object.keys(members)) {
      if (uid === senderId) continue;

      const tokenSnap = await db
        .ref(`users/${uid}/fcmToken`)
        .once("value");
      const token = tokenSnap.val();
      if (token) tokens.push(token);
    }

    if (tokens.length === 0) return;

    // 푸시 알림 발송
    const messaging = getMessaging();
    const payload = {
      notification: {
        title: "가족 채팅",
        body: `${senderName}: ${body}`,
      },
      data: {
        title: "가족 채팅",
        body: content,
        senderName: senderName,
        messageType: messageType,
        groupId: groupId,
      },
    };

    const response = await messaging.sendEachForMulticast({
      tokens: tokens,
      ...payload,
    });

    // 만료된 토큰 정리
    response.responses.forEach((resp, idx) => {
      if (
        resp.error &&
        (resp.error.code === "messaging/invalid-registration-token" ||
          resp.error.code === "messaging/registration-token-not-registered")
      ) {
        // 유효하지 않은 토큰은 DB에서 삭제
        for (const uid of Object.keys(members)) {
          db.ref(`users/${uid}/fcmToken`).once("value", (snap) => {
            if (snap.val() === tokens[idx]) {
              snap.ref.remove();
            }
          });
        }
      }
    });

    console.log(
      `Sent ${response.successCount} notifications for group ${groupId}`
    );
  }
);
