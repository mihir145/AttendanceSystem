package com.example.attendancesystem.faculty.ui.takeattendance

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendancesystem.data.entity.User
import com.example.attendancesystem.faculty.repository.FacultyRepository
import com.example.attendancesystem.utils.Events
import com.example.attendancesystem.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class TakeAttendanceViewModel @Inject constructor(
    private val repository: FacultyRepository
) : ViewModel() {

//    private val _takeAttendance = MutableLiveData<Events<Resource<Boolean>>>()
//    val takeAttendance: LiveData<Events<Resource<Boolean>>> = _takeAttendance

    private val _curImageUri = MutableLiveData<Uri>()
    val curImageUri: LiveData<Uri> = _curImageUri

    fun setCurrentImageUri(uri: Uri) {
        _curImageUri.postValue(uri)
    }

    private val _userStatus = MutableLiveData<Events<Resource<User>>>()
    val userStatus: LiveData<Events<Resource<User>>> = _userStatus

    fun getUser() {
        _userStatus.postValue(Events(Resource.Loading()))

        viewModelScope.launch(Dispatchers.IO) {
            val user = repository.getUser()
            _userStatus.postValue(Events(user))
        }

    }

//    fun takeAttendance() {
//
//        val error = if (curImageUri.value == null) {
//            "uri"
//        } else null
//
//        error?.let {
//            _takeAttendance.postValue(Events(Resource.Error(error)))
//            return
//        }
//
//        _takeAttendance.postValue(Events(Resource.Loading()))
//
//        viewModelScope.launch(Dispatchers.IO) {
//
//            val bitmap = if (Build.VERSION.SDK_INT < 28) {
//                MediaStore.Images.Media.getBitmap(
//                    context.contentResolver,
//                    curImageUri.value
//                )
//            } else {
//                val source = ImageDecoder.createSource(context.contentResolver, curImageUri.value!!)
//                ImageDecoder.decodeBitmap(source)
//            }
//
//            val result = repository.attendance(bitmap)
//            _takeAttendance.postValue(Events(result))
//        }
//
//    }

//    fun removeObservers() {
//        _takeAttendance.value = null
//        _takeAttendance.postValue(null)
//    }

}