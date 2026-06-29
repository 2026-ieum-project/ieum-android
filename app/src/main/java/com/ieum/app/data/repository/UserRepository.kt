package com.ieum.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

data class UserData(
    val uid: String = "",
    val name: String = "",
    val role: String = "",
    val gender: String = "female",
    val groupId: String = ""
)

class UserRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseDatabase = FirebaseDatabase.getInstance()
) {
    val currentUid: String? get() = auth.currentUser?.uid

    val isLoggedIn: Boolean get() = auth.currentUser != null

    suspend fun login(email: String, password: String): Result<String> = try {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        val uid = result.user?.uid ?: throw IllegalStateException("UID is null")
        Result.success(uid)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun register(
        name: String,
        email: String,
        password: String,
        role: String,
        gender: String
    ): Result<String> = try {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val uid = result.user?.uid ?: throw IllegalStateException("UID is null")
        val userData = mapOf(
            "name" to name,
            "role" to role,
            "gender" to gender,
            "groupId" to ""
        )
        db.reference.child("users").child(uid).setValue(userData).await()
        Result.success(uid)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = try {
        auth.sendPasswordResetEmail(email).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun logout() {
        auth.signOut()
    }

    suspend fun getUserData(uid: String): Result<UserData> = try {
        val snapshot = db.reference.child("users").child(uid).get().await()
        val data = UserData(
            uid = uid,
            name = snapshot.child("name").getValue(String::class.java) ?: "",
            role = snapshot.child("role").getValue(String::class.java) ?: "",
            gender = snapshot.child("gender").getValue(String::class.java) ?: "female",
            groupId = snapshot.child("groupId").getValue(String::class.java) ?: ""
        )
        Result.success(data)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getUserRole(uid: String): Result<String> = try {
        val snapshot = db.reference.child("users").child(uid).child("role").get().await()
        Result.success(snapshot.getValue(String::class.java) ?: "")
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getUserName(uid: String): Result<String> = try {
        val snapshot = db.reference.child("users").child(uid).child("name").get().await()
        Result.success(snapshot.getValue(String::class.java) ?: "")
    } catch (e: Exception) {
        Result.failure(e)
    }
}
