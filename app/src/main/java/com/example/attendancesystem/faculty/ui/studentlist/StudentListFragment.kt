package com.example.attendancesystem.faculty.ui.studentlist

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.attendancesystem.R
import com.example.attendancesystem.adapter.ListAdapter
import com.example.attendancesystem.databinding.FragmentStudentListBinding
import com.example.attendancesystem.utils.EventObserver
import com.example.attendancesystem.utils.snackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class StudentListFragment : Fragment(R.layout.fragment_student_list) {
    @Inject
    lateinit var listAdapter: ListAdapter

    private lateinit var viewModel: StudentListViewModel
    private lateinit var binding: FragmentStudentListBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[StudentListViewModel::class.java]
        subscribeToObserve()

        binding = FragmentStudentListBinding.bind(view)

        setUpRecyclerView()

        listAdapter.setOnUserClickListener {

        }

        viewModel.getStudentsList()

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
        ) { users ->
            listAdapter.users = users
            binding.progressBar.isVisible = false
        })
    }

}