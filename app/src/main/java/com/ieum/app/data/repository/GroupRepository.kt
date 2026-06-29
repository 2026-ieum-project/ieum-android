package com.ieum.app.data.repository

import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

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
        val code = generateInviteCode()
        val groupsRef = db.reference.child("groups")
        val newGroupRef = groupsRef.push()
        val groupId = newGroupRef.key ?: throw IllegalStateException("그룹 ID 생성 실패")

        val groupData = mapOf(
            "inviteCode" to code,
            "createdBy" to uid,
            "members" to mapOf(uid to "child")
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
        // inviteCodes에서 그룹 ID 조회
        var groupId = ref.child("inviteCodes").child(code).get().await()
            .getValue(String::class.java)

        if (groupId.isNullOrEmpty()) {
            // 구버전 호환: groups에서 직접 검색
            val groupSnapshot = ref.child("groups")
                .orderByChild("inviteCode").equalTo(code).get().await()
            if (!groupSnapshot.exists()) {
                throw IllegalArgumentException("유효하지 않은 초대 코드입니다")
            }
            val groupEntry = groupSnapshot.children.first()
            groupId = groupEntry.key ?: throw IllegalStateException("그룹 정보를 읽을 수 없습니다")
            // 이후 조회를 위해 inviteCodes에도 저장
            ref.child("inviteCodes").child(code).setValue(groupId).await()
        }

        ref.child("groups").child(groupId).child("members").child(uid).setValue(role).await()
        ref.child("users").child(uid).child("groupId").setValue(groupId).await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun generateInviteCode(): String {
        val chars = ('A'..'Z') + ('0'..'9')
        return (1..6).map { chars.random() }.joinToString("")
    }
}
