package com.example.attendancesystem.authorization.authfragments.ui.apppinscreen

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.attendancesystem.R
import com.example.attendancesystem.databinding.FragmentAppPinBinding
import com.example.attendancesystem.faculty.FacultyActivity
import com.example.attendancesystem.principal.PrincipalActivity
import com.example.attendancesystem.utils.EventObserver
import com.example.attendancesystem.utils.hideKeyboard
import com.example.attendancesystem.utils.showProgress
import com.example.attendancesystem.utils.snackbar

class AppPinFragment : Fragment(R.layout.fragment_app_pin) {

    private lateinit var binding: FragmentAppPinBinding
    private lateinit var viewModel: AppPinViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[AppPinViewModel::class.java]
        subscribeToObserve()

        binding = FragmentAppPinBinding.bind(view)

        binding.apply {
            fabLogin.setOnClickListener {
                hideKeyboard(requireActivity())
                viewModel.login(pinView.text.toString())
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
        ) { role ->
            showProgress(
                activity = requireActivity(),
                bool = false,
                parentLayout = binding.parentLayout,
                cvProgress = binding.cvProgress
            )
            binding.apply {
                pinView.setText("")
            }
            snackbar("Logged in successfully!!")
            if (role == "principal") {
                Intent(requireActivity(), PrincipalActivity::class.java).also { intent ->
                    startActivity(intent)
                    requireActivity().finish()
                }
            } else {
                Intent(requireActivity(), FacultyActivity::class.java).also { intent ->
                    startActivity(intent)
                    requireActivity().finish()
                }
            }
        })
    }

}