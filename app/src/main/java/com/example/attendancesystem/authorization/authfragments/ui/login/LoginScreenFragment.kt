package com.example.attendancesystem.authorization.authfragments.ui.login

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.attendancesystem.R
import com.example.attendancesystem.databinding.FragmentLoginBinding
import com.example.attendancesystem.utils.*

class LoginScreenFragment : Fragment(R.layout.fragment_login) {

    private lateinit var binding: FragmentLoginBinding
    private lateinit var viewModel: LoginViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[LoginViewModel::class.java]
        subscribeToObserve()

        binding = FragmentLoginBinding.bind(view)

        binding.apply {

            btnLogin.setOnClickListener {
                hideKeyboard(requireActivity())
                viewModel.login(etEmail.text?.trim().toString(), pinView.text.toString())
            }

        }

    }

    private fun subscribeToObserve() {
        viewModel.loginStatus.observe(viewLifecycleOwner, EventObserver(
            onError = { error ->
                showProgress(
                    activity = requireActivity(),
                    bool = false,
                    parentLayout = binding.parentLayout,
                    cvProgress = binding.cvProgress
                )
                when (error) {
                    "emptyEmail" -> {
                        binding.etEmail.error = "Email cannot be empty"
                    }
                    "email" -> {
                        binding.etEmail.error = "Enter a valid email"
                    }
                    "pin" -> {
                        snackbar("Pin should be of 6 length")
                    }
                    else -> snackbar(error)
                }
            },
            onLoading = {
                showProgress(
                    activity = requireActivity(),
                    bool = true,
                    parentLayout = binding.parentLayout,
                    cvProgress = binding.cvProgress
                )
            }
        ) { result ->
            val sharedPreferences: SharedPreferences =
                requireActivity().getSharedPreferences(
                    Constants.SHARED_PREF_FILE,
                    Context.MODE_PRIVATE
                )

            val editor: SharedPreferences.Editor = sharedPreferences.edit()

            val user = result.second

            editor.apply {
                putString(Constants.LATITUDE_KEY, user.schoolLatitude.toString())
                putString(Constants.LONGITUDE_KEY, user.schoolLongitude.toString())
                apply()
                commit()
            }
            showProgress(
                activity = requireActivity(),
                bool = false,
                parentLayout = binding.parentLayout,
                cvProgress = binding.cvProgress
            )
            binding.apply {
                etEmail.setText("")
                pinView.setText("")
            }
            snackbar("Logged in successfully!!")
            findNavController().navigate(
                LoginScreenFragmentDirections
                    .actionLoginScreenFragmentToFaceLoginFragment(role = result.first)
            )
        })
    }

}