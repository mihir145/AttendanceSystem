package com.example.attendancesystem.authorization.repository

import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import com.example.attendancesystem.data.entity.User
import com.example.attendancesystem.utils.Constants
import com.example.attendancesystem.utils.Resource
import com.example.attendancesystem.utils.safeCall
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream


class DefaultAuthRepository : AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val principal = FirebaseFirestore.getInstance().collection("principal")
    private val faculty = FirebaseFirestore.getInstance().collection("faculty")

    override suspend fun registerFaculty(
        profilePicUrl: Uri,
        bitmap: Bitmap,
        email: String,
        phone: String,
        name: String,
        enrolNo: String,
        pin: String,
        schoolId: String,
        schoolLatitude: Double,
        schoolLongitude: Double
    ): Resource<User> {
        return withContext(Dispatchers.IO) {
            safeCall {
                val result = auth.createUserWithEmailAndPassword(email, pin).await()
                auth.currentUser?.updateProfile(
                    userProfileChangeRequest {
                        displayName = name
                        photoUri = profilePicUrl
                    }
                )

                val baos = ByteArrayOutputStream()

                val resizedBitmap = Bitmap.createScaledBitmap(
                    bitmap,
                    Constants.DST_WIDTH,
                    Constants.DST_HEIGHT, true
                )

                resizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
                val byteArray: String =
                    Base64.encodeToString(
                        baos.toByteArray(),
                        Base64.DEFAULT
                    )

                val user = User(
                    enrollment = enrolNo,
                    name = name,
                    mobile = phone,
                    email = email,
                    uid = result.user!!.uid,
                    school_id = schoolId,
                    byteArray = byteArray,
                    schoolLatitude = schoolLatitude,
                    schoolLongitude = schoolLongitude
                )

                faculty.document(result.user!!.uid).set(user).await()

                Resource.Success(user)
            }
        }
    }

    override suspend fun registerPrincipal(
        profilePicUrl: Uri,
        bitmap: Bitmap,
        email: String,
        phone: String,
        name: String,
        enrolNo: String,
        pin: String,
        schoolId: String,
        curLatitude: Double,
        curLongitude: Double
    ): Resource<User> {
        return withContext(Dispatchers.IO) {
            safeCall {
                val result = auth.createUserWithEmailAndPassword(email, pin).await()

                auth.currentUser?.updateProfile(
                    userProfileChangeRequest {
                        displayName = name
                        photoUri = profilePicUrl
                    }
                )

                val baos = ByteArrayOutputStream()

                val resizedBitmap = Bitmap.createScaledBitmap(
                    bitmap,
                    Constants.DST_WIDTH,
                    Constants.DST_HEIGHT, true
                )

                resizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
                val byteArray: String =
                    Base64.encodeToString(
                        baos.toByteArray(),
                        Base64.DEFAULT
                    )

                val user = User(
                    enrollment = enrolNo,
                    name = name,
                    mobile = phone,
                    email = email,
                    uid = result.user!!.uid,
                    school_id = schoolId,
                    byteArray = byteArray,
                    schoolLatitude = curLatitude,
                    schoolLongitude = curLongitude
                )

                principal.document(result.user!!.uid).set(user).await()

                Resource.Success(user)
            }
        }
    }

    override suspend fun loginUser(email: String, pin: String): Resource<Pair<String, User>> {
        return withContext(Dispatchers.IO) {
            safeCall {
                val result = auth.signInWithEmailAndPassword(email, pin).await()
                val uid = result.user?.uid!!
                val principal =
                    principal.document(uid).get().await().toObject(User::class.java)
                if (principal == null) {
                    val faculty = faculty.document(uid).get().await().toObject(User::class.java)!!
                    Resource.Success(Pair("faculty", faculty))
                } else {
                    Resource.Success(Pair("principal", principal))
                }
            }
        }
    }

    override suspend fun login(email: String, pin: String): Resource<String> {
        return withContext(Dispatchers.IO) {
            safeCall {
                val result = auth.signInWithEmailAndPassword(email, pin).await()
                val uid = result.user?.uid!!
                val principal =
                    principal.document(uid).get().await().toObject(User::class.java)
                if (principal == null) {
                    Resource.Success("faculty")
                } else {
                    Resource.Success("principal")
                }
            }
        }
    }

}