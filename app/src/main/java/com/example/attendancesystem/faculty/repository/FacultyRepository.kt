package com.example.attendancesystem.faculty.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.attendancesystem.data.entity.User
import com.example.attendancesystem.utils.Resource
import com.google.firebase.auth.AuthResult

interface FacultyRepository {

    suspend fun addStudent(
        context: Context,
        profilePicUrl: Uri,
        bitmap: Bitmap,
        email: String,
        name: String,
        enrolNo: String
    ): Resource<User>

    suspend fun getUser(): Resource<User>

    suspend fun getUsersList(): Resource<List<User>>

}