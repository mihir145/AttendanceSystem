package com.example.attendancesystem.faculty

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.attendancesystem.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FacultyActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_faculty)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_container_faculty) as NavHostFragment

        appBarConfiguration = AppBarConfiguration.Builder(
            setOf(
                R.id.facultyHomeFragment
            )
        ).build()

        navController = navHostFragment.findNavController()
        setupActionBarWithNavController(navController, appBarConfiguration)
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)

    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}