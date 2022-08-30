package com.example.attendancesystem.authorization.authfragments.ui.launcherscreen

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.attendancesystem.R
import com.example.attendancesystem.data.entity.User
import com.example.attendancesystem.databinding.FragmentLauncherScreenBinding
import com.example.attendancesystem.utils.Constants.SHARED_PREF_FILE
import com.example.attendancesystem.utils.Constants.TIME_KEY
import com.example.attendancesystem.utils.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@AndroidEntryPoint
class LauncherScreenFragment : Fragment(R.layout.fragment_launcher_screen) {

    private lateinit var binding: FragmentLauncherScreenBinding
    private val principal = FirebaseFirestore.getInstance().collection("principal")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentLauncherScreenBinding.bind(view)

        if (FirebaseAuth.getInstance().currentUser != null) {

            val sharedPreferences: SharedPreferences =
                requireActivity().getSharedPreferences(SHARED_PREF_FILE, Context.MODE_PRIVATE)

            if (System.currentTimeMillis() < sharedPreferences.getLong(TIME_KEY, 0L)) {
                findNavController().navigate(
                    LauncherScreenFragmentDirections
                        .actionLauncherScreenFragmentToAppPinFragment()
                )
            } else {
                CoroutineScope(Dispatchers.Main).launch {
                    val principal =
                        principal.whereEqualTo("uid", Firebase.auth.currentUser?.uid).get().await()
                            .toObjects(User::class.java)
                    val role = if (principal.isEmpty()) {
                        "faculty"
                    } else {
                        "principal"
                    }
                    findNavController().navigate(
                        LauncherScreenFragmentDirections
                            .actionLauncherScreenFragmentToFaceLoginFragment(role = role)
                    )
                }
            }
        }

        binding.apply {

            btnRegisterAsFaculty.setOnClickListener {
                findNavController().navigate(
                    LauncherScreenFragmentDirections
                        .actionLauncherScreenFragmentToFacultyRegisterFragment()
                )
            }

            btnRegisterAsPrincipal.setOnClickListener {
                findNavController().navigate(
                    LauncherScreenFragmentDirections
                        .actionLauncherScreenFragmentToPrincipalRegisterFragment()
                )
            }

            btnLogin.setOnClickListener {
                findNavController().navigate(
                    LauncherScreenFragmentDirections
                        .actionLauncherScreenFragmentToLoginScreenFragment()
                )
            }

        }

    }

}