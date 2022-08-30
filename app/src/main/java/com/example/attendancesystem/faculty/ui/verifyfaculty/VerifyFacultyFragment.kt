package com.example.attendancesystem.faculty.ui.verifyfaculty

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.attendancesystem.R
import com.example.attendancesystem.databinding.FragmentVerifyFacultyBinding
import com.example.attendancesystem.principal.ui.verifyuser.VerifyPrincipalFragmentDirections
import com.example.attendancesystem.utils.EventObserver
import com.example.attendancesystem.utils.snackbar
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit


@AndroidEntryPoint
class VerifyFacultyFragment : Fragment(R.layout.fragment_verify_faculty) {

    private lateinit var binding: FragmentVerifyFacultyBinding
    private lateinit var viewModel: VerifyFacultyViewModel
    private lateinit var verificationID: String
    private lateinit var token: ForceResendingToken
    private lateinit var phone: String

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[VerifyFacultyViewModel::class.java]
        subscribeToObserve()

        binding = FragmentVerifyFacultyBinding.bind(view)

        viewModel.getUser()

        binding.apply {
            btnVerifyOtp.setOnClickListener {
                verifyCode(etOtp.text?.trim().toString())
            }
            tvResendOtp.setOnClickListener {
                disableResendButton()
                resendVerificationCode(phone = phone, token = token)
            }
        }

    }

    private fun subscribeToObserve() {

        viewModel.userStatus.observe(viewLifecycleOwner, EventObserver(
            onError = { error ->
                binding.progressBar.isVisible = false
                snackbar(error)
            },
            onLoading = {
                binding.progressBar.isVisible = true
            }
        ) { user ->
            binding.progressBar.isVisible = false
            phone = user.mobile.trim()
            sendVerificationCode(phone = phone)
        })

    }

    private fun disableResendButton() {
        binding.tvResendOtp.apply {
            setTextColor(ContextCompat.getColor(requireContext(), R.color.gray))
            isEnabled = false
        }

        Handler(Looper.getMainLooper())
            .postDelayed({
                binding.tvResendOtp.apply {
                    isEnabled = true
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                }
            }, 30000)
    }

    private fun sendVerificationCode(phone: String) {
        val options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
            .setPhoneNumber("+91$phone")
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(requireActivity())
            .setCallbacks(mCallbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private val mCallbacks =
        object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            override fun onVerificationCompleted(p0: PhoneAuthCredential) {
                val code = p0.smsCode
                code?.let {
                    verifyCode(it)
                }
            }

            override fun onVerificationFailed(p0: FirebaseException) {
                disableResendButton()
                snackbar(p0.message.toString())
            }

            override fun onCodeSent(p0: String, p1: ForceResendingToken) {
                verificationID = p0
                token = p1
                snackbar("Code sent")
                disableResendButton()
                binding.apply {
                    btnVerifyOtp.isEnabled = true
                    tlOtp.isEnabled = true
                    progressBar.isVisible = false
                }
            }
        }

    private fun verifyCode(code: String) {
        val credential = PhoneAuthProvider.getCredential(verificationID, code)

        FirebaseAuth.getInstance().currentUser?.updatePhoneNumber(credential)
            ?.addOnCompleteListener {
                if (it.isSuccessful)
                    findNavController().navigate(
                        VerifyFacultyFragmentDirections
                            .actionVerifyFacultyFragmentToTakeAttendanceFragment()
                    )
                else
                    snackbar(it.exception?.message.toString())
            }
    }

    private fun resendVerificationCode(
        phone: String,
        token: ForceResendingToken
    ) {
        val options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
            .setPhoneNumber("+91$phone")
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(requireActivity())
            .setForceResendingToken(token)
            .setCallbacks(mCallbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

}