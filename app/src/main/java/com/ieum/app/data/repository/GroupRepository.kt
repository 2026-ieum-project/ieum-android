package com.ieum.app.data.repository

import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom

data class GroupMemberInfo(
    val grandparentName: String? = null,
    val childName: String? = null,
    val grandchildName: String? = null,
    val hasGrandparent: Boolean = false,
    val hasChild: Boolean = false,
    val hasGrandchild: Boolean = false
)

class GroupRepository(
    private val db: FirebaseDatabase = FirebaseDatabase.getInstance()
) {
    suspend fun getInviteCode(groupId: String): Result<String> = try {
        val snap = db.reference.child("groups").child(groupId).child("inviteCode").get().await()
        Result.success(snap.getValue(String::class.java) ?: "")
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getGroupMembers(groupId: String): Result<GroupMemberInfo> = try {
        val membersSnap = db.reference.child("groups").child(groupId).child("members").get().await()

        var grandparentUid: String? = null
        var childUid: String? = null
        var grandchildUid: String? = null

        for (child in membersSnap.children) {
            when (child.getValue(String::class.java)) {
                "grandparent" -> if (grandparentUid == null) grandparentUid = child.key
                "child" -> if (childUid == null) childUid = child.key
                "grandchild" -> if (grandchildUid == null) grandchildUid = child.key
            }
        }

        val grandparentName = grandparentUid?.let { uid ->
            db.reference.child("users").child(uid).child("name").get().await()
                .getValue(String::class.java)
        }
        val childName = childUid?.let { uid ->
            db.reference.child("users").child(uid).child("name").get().await()
                .getValue(String::class.java)
        }
        val grandchildName = grandchildUid?.let { uid ->
            db.reference.child("users").child(uid).child("name").get().await()
                .getValue(String::class.java)
        }

        Result.success(
            GroupMemberInfo(
                grandparentName = grandparentName,
                childName = childName,
                grandchildName = grandchildName,
                hasGrandparent = grandparentUid != null,
                hasChild = childUid != null,
                hasGrandchild = grandchildUid != null
            )
        )
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun createGroup(uid: String): Result<String> = try {
        // 보안 규칙이 멤버 역할 == users/{uid}/role 일치를 검증하므로 실제 역할을 사용
        val role = db.reference.child("users").child(uid).child("role").get().await()
            .getValue(String::class.java) ?: "child"

        val groupsRef = db.reference.child("groups")
        val newGroupRef = groupsRef.push()
        val groupId = newGroupRef.key ?: throw IllegalStateException("그룹 ID 생성 실패")

        // 사용 중이지 않은 초대 코드 확보 (경합 시에는 보안 규칙의 !data.exists()가 최종 방어)
        var code = generateInviteCode()
        var attempts = 0
        while (db.reference.child("inviteCodes").child(code).get().await().exists()) {
            if (++attempts >= 5) throw IllegalStateException("초대 코드 생성에 실패했습니다. 다시 시도해주세요")
            code = generateInviteCode()
        }

        val groupData = mapOf(
            "inviteCode" to code,
            "createdBy" to uid,
            "members" to mapOf(uid to role)
        )
        newGroupRef.setValue(groupData).await()
        db.reference.child("inviteCodes").child(code).setValue(groupId).await()
        db.reference.child("users").child(uid).child("groupId").setValue(groupId).await()

        Result.success(code)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun joinGroup(uid: String, role: String, code: String): Result<Unit> = try {
        val ref = db.reference
        val groupId = ref.child("inviteCodes").child(code).get().await()
            .getValue(String::class.java)

        if (groupId.isNullOrEmpty()) {
            throw IllegalArgumentException("유효하지 않은 초대 코드입니다")
        }

        ref.child("groups").child(groupId).child("members").child(uid).setValue(role).await()
        ref.child("users").child(uid).child("groupId").setValue(groupId).await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun generateInviteCode(): String {
        // 노인 사용자가 혼동하기 쉬운 문자(0/O, 1/I/L) 제외
        val chars = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars[secureRandom.nextInt(chars.length)] }.joinToString("")
    }

    companion object {
        private val secureRandom = SecureRandom()
    }
}
