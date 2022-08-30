package com.example.attendancesystem.authorization.authfragments.ui.facultyregister

import android.content.Context
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendancesystem.authorization.repository.AuthRepository
import com.example.attendancesystem.data.entity.User
import com.example.attendancesystem.utils.BitmapUtils
import com.example.attendancesystem.utils.Events
import com.example.attendancesystem.utils.Resource
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject


@HiltViewModel
class FacultyRegisterViewModel @Inject constructor(
    private val context: Context,
    private val repository: AuthRepository
) : ViewModel() {

    private val principal = FirebaseFirestore.getInstance().collection("principal")

    private val _registerStatus = MutableLiveData<Events<Resource<User>>>()
    val registerStatus: LiveData<Events<Resource<User>>> = _registerStatus

    private val _curImageUri = MutableLiveData<Uri>()
    val curImageUri: LiveData<Uri> = _curImageUri

    fun setCurrentImageUri(uri: Uri) {
        _curImageUri.postValue(uri)
    }

    fun register(
        email: String,
        phone: String,
        name: String,
        enrolNo: String,
        pin: String,
        schoolId: String
    ) {
        val error = if (email.isEmpty()) {
            "emptyEmail"
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            "email"
        } else if (phone.isEmpty()) {
            "emptyPhone"
        } else if (!Patterns.PHONE.matcher(phone).matches()) {
            "phone"
        } else if (name.isEmpty()) {
            "name"
        } else if (enrolNo.isEmpty()) {
            "emptyEnrol"
        } else if (enrolNo.contains(" ")) {
            "enrol"
        } else if (pin.length != 6) {
            "pin"
        } else if (curImageUri.value == Uri.EMPTY || curImageUri.value == null) {
            "uri"
        } else if (schoolId.isEmpty()) {
            "emptySchoolID"
        } else null

        error?.let {
            _registerStatus.postValue(Events(Resource.Error(error)))
            return
        }

        _registerStatus.postValue(Events(Resource.Loading()))

        val bitmap = BitmapUtils.getBitmapFromUri(context.contentResolver, curImageUri.value!!)

        viewModelScope.launch(Dispatchers.Main) {

            val principal = principal.whereEqualTo("school_id", schoolId).get().await()
                .toObjects(User::class.java)

            if (principal.isEmpty()) {
                _registerStatus.postValue(Events(Resource.Error("Principal has not registered for this school")))
            } else {
                val result = repository.registerFaculty(
                    profilePicUrl = curImageUri.value!!,
                    bitmap = bitmap,
                    email = email,
                    phone = phone,
                    name = name,
                    enrolNo = enrolNo,
                    pin = pin,
                    schoolId = schoolId,
                    schoolLatitude = principal[0].schoolLatitude,
                    schoolLongitude = principal[0].schoolLongitude
                )
                _registerStatus.postValue(Events(result))
            }
        }
    }

}