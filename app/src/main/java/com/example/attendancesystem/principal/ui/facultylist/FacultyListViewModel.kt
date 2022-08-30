package com.example.attendancesystem.principal.ui.facultylist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendancesystem.data.entity.User
import com.example.attendancesystem.faculty.repository.FacultyRepository
import com.example.attendancesystem.principal.repository.PrincipalRepository
import com.example.attendancesystem.utils.Events
import com.example.attendancesystem.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FacultyListViewModel @Inject constructor(
    private val repository: PrincipalRepository
) : ViewModel() {

    private val _list = MutableLiveData<Events<Resource<List<User>>>>()
    val list: LiveData<Events<Resource<List<User>>>> = _list

    fun getFacultyList() {
        _list.postValue(Events(Resource.Loading()))
        viewModelScope.launch {
            val result = repository.getUsersList()
            _list.postValue(Events(result))
        }
    }
}