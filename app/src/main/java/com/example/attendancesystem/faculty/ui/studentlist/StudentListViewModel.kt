package com.example.attendancesystem.faculty.ui.studentlist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendancesystem.data.entity.User
import com.example.attendancesystem.faculty.repository.FacultyRepository
import com.example.attendancesystem.utils.Events
import com.example.attendancesystem.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudentListViewModel @Inject constructor(
    private val repository: FacultyRepository
) : ViewModel() {

    private val _list = MutableLiveData<Events<Resource<List<User>>>>()
    val list: LiveData<Events<Resource<List<User>>>> = _list

    fun getStudentsList() {
        _list.postValue(Events(Resource.Loading()))
        viewModelScope.launch {
            val result = repository.getUsersList()
            _list.postValue(Events(result))
        }
    }
}