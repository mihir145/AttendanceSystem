package com.example.attendancesystem.principal.ui.facultylist

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.attendancesystem.R
import com.example.attendancesystem.adapter.ListAdapter
import com.example.attendancesystem.databinding.FragmentFacultyListBinding
import com.example.attendancesystem.databinding.FragmentStudentListBinding
import com.example.attendancesystem.faculty.ui.studentlist.StudentListViewModel
import com.example.attendancesystem.utils.EventObserver
import com.example.attendancesystem.utils.snackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FacultyListFragment: Fragment(R.layout.fragment_faculty_list) {

    @Inject
    lateinit var listAdapter: ListAdapter

    private lateinit var viewModel: FacultyListViewModel
    private lateinit var binding: FragmentFacultyListBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[FacultyListViewModel::class.java]
        subscribeToObserve()

        binding = FragmentFacultyListBinding.bind(view)

        setUpRecyclerView()

        listAdapter.setOnUserClickListener {

        }

        viewModel.getFacultyList()

    }

    private fun setUpRecyclerView() {
        binding.rvListStudent.apply {
            adapter = listAdapter
        }
    }

    private fun subscribeToObserve() {
        viewModel.list.observe(viewLifecycleOwner, EventObserver(
            onError = {
                binding.progressBar.isVisible = false
                snackbar(it)
            },
            onLoading = {
                binding.progressBar.isVisible = true
                listAdapter.users = listOf()
            }
        ) { faculties ->
            listAdapter.users = faculties
            binding.progressBar.isVisible = false
        })
    }


}