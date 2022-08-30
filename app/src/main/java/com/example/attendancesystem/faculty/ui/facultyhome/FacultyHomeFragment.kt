package com.example.attendancesystem.faculty.ui.facultyhome

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.RequestManager
import com.example.attendancesystem.R
import com.example.attendancesystem.authorization.AuthActivity
import com.example.attendancesystem.data.entity.User
import com.example.attendancesystem.databinding.FragmentFacultyHomeBinding
import com.example.attendancesystem.utils.Constants
import com.example.attendancesystem.utils.Constants.ALLOWED_RADIUS
import com.example.attendancesystem.utils.EventObserver
import com.example.attendancesystem.utils.showProgress
import com.example.attendancesystem.utils.snackbar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class FacultyHomeFragment : Fragment(R.layout.fragment_faculty_home) {

    @Inject
    lateinit var glide: RequestManager
    private lateinit var binding: FragmentFacultyHomeBinding
    private lateinit var viewModel: FacultyHomeViewModel
    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener
    private var inCampus = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[FacultyHomeViewModel::class.java]
        subscribeToObserve()

        binding = FragmentFacultyHomeBinding.bind(view)

        binding.apply {

            btnTakeAttendance.isEnabled = false
            btnRegisterNewStudent.isEnabled = false
            btnSignout.isEnabled = false
            btnStudentList.isEnabled = false

            viewModel.getUser()

            btnRegisterNewStudent.setOnClickListener {
                findNavController().navigate(
                    FacultyHomeFragmentDirections
                        .actionFacultyHomeFragmentToStudentRegisterFragment()
                )
            }

            btnTakeAttendance.setOnClickListener {
                if (!it.isActivated) {
                    snackbar("You Should be in campus to access this feature")
                } else {
                    findNavController().navigate(
                        FacultyHomeFragmentDirections
                            .actionFacultyHomeFragmentToVerifyFacultyFragment()
                    )
                }
            }

            btnSignout.setOnClickListener {
                Firebase.auth.signOut()
                val sharedPreferences: SharedPreferences =
                    requireActivity().getSharedPreferences(
                        Constants.SHARED_PREF_FILE,
                        Context.MODE_PRIVATE
                    )

                val editor: SharedPreferences.Editor = sharedPreferences.edit()

                editor.apply {
                    putLong(Constants.TIME_KEY, 0L)
                    putString(Constants.LATITUDE_KEY, "0")
                    putString(Constants.LONGITUDE_KEY, "0")
                    apply()
                    commit()
                }
                Intent(requireActivity(), AuthActivity::class.java).also { intent ->
                    startActivity(intent)
                    requireActivity().finish()
                }
            }

            btnStudentList.setOnClickListener {
                findNavController().navigate(
                    FacultyHomeFragmentDirections
                        .actionFacultyHomeFragmentToStudentListFragment()
                )
            }

        }

    }

    private fun subscribeToObserve() {
        viewModel.userStatus.observe(viewLifecycleOwner, EventObserver(
            onError = { error ->
                showProgress(
                    activity = requireActivity(),
                    bool = false,
                    parentLayout = binding.parentLayout,
                    cvProgress = binding.cvProgress
                )
                snackbar(error)
            },
            onLoading = {
                showProgress(
                    activity = requireActivity(),
                    bool = true,
                    parentLayout = binding.parentLayout,
                    cvProgress = binding.cvProgress
                )
            }
        ) { user ->
            updateUI(user)
        })
    }

    private fun updateUI(user: User) {
        getLocation()
        binding.apply {
            tvEnrNo.text = user.enrollment
            val decodedByteArray: ByteArray = Base64.decode(user.byteArray, Base64.DEFAULT)
            glide.load(decodedByteArray).into(ivImage)
            val sdf = SimpleDateFormat("dd/M/yyyy hh:mm:ss")
            val currentDate = sdf.format(Date())
            tvCurTime.text = "Current date time: $currentDate"
        }
    }

    private fun requestLocationPermission() {
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                getLocation()
            } else {
                val alertDialog = AlertDialog.Builder(requireContext()).apply {
                    setTitle("Location Permission")
                    setMessage("The app couldn't function without the location permission.")
                    setCancelable(false)
                    setPositiveButton("ALLOW") { dialog, _ ->
                        dialog.dismiss()
                        requestLocationPermission()
                    }
                    setNegativeButton("CLOSE") { dialog, _ ->
                        dialog.dismiss()
                        requireActivity().finish()
                    }
                    create()
                }
                alertDialog.show()
            }

        }

    private fun getLocation() {

        val sharedPreferences: SharedPreferences =
            requireActivity().getSharedPreferences(
                Constants.SHARED_PREF_FILE,
                Context.MODE_PRIVATE
            )

        locationManager =
            requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermission()
        } else {
            locationListener = LocationListener { location ->
                val latitude = location.latitude
                val longitude = location.longitude
                val accuracy = location.accuracy

                binding.tvLocation.text = "Latitude: $latitude \nLongitude: $longitude"
                binding.tvAccuracy.text = "Accuracy: $accuracy"

                val results = FloatArray(1)

                val schoolLatitude = sharedPreferences.getString(Constants.LATITUDE_KEY, "0")
                val schoolLongitude = sharedPreferences.getString(Constants.LONGITUDE_KEY, "0")

                Location.distanceBetween(
                    schoolLatitude!!.toDouble(),
                    schoolLongitude!!.toDouble(),
                    latitude,
                    longitude,
                    results
                )
                val distanceInMeters = results[0]
                inCampus = distanceInMeters < ALLOWED_RADIUS

                binding.apply {
                    tvInCampus.text = if (inCampus) "In Campus" else "Outside campus"
                    btnTakeAttendance.isActivated = inCampus
                    btnTakeAttendance.alpha = if (inCampus) 1F else 0.5F
                    btnRegisterNewStudent.isEnabled = true
                    btnTakeAttendance.isEnabled = true
                    btnSignout.isEnabled = true
                    btnStudentList.isEnabled = true
                }
            }

            try {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    0L,
                    0f,
                    locationListener
                )
            } catch (e: Exception) {
                Log.d("TAG_ERROR", "getLocation: ${e.message}")
            }
        }

        showProgress(
            activity = requireActivity(),
            bool = false,
            parentLayout = binding.parentLayout,
            cvProgress = binding.cvProgress
        )

    }

    override fun onDestroyView() {
        super.onDestroyView()
        locationManager.removeUpdates(locationListener)
    }

}