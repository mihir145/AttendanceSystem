package com.example.attendancesystem.authorization.authfragments.ui.login

import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendancesystem.authorization.repository.AuthRepository
import com.example.attendancesystem.data.entity.User
import com.example.attendancesystem.utils.Events
import com.example.attendancesystem.utils.Resource
import com.google.firebase.auth.AuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _loginStatus = MutableLiveData<Events<Resource<Pair<String, User>>>>()
    val loginStatus: LiveData<Events<Resource<Pair<String, User>>>> = _loginStatus

    fun login(email: String, pin: String) {
        val error = if (email.isEmpty()) {
            "emptyEmail"
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            "email"
        } else if (pin.length != 6) {
            "pin"
        } else null

        error?.let {
            _loginStatus.postValue(Events(Resource.Error(error)))
            return
        }

        _loginStatus.postValue(Events(Resource.Loading()))

        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.loginUser(email, pin)
            _loginStatus.postValue(Events(result))
        }

    }

}