package com.example.attendancesystem.adapter

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.example.attendancesystem.data.entity.User
import com.example.attendancesystem.databinding.ItemListBinding
import com.google.android.material.imageview.ShapeableImageView
import javax.inject.Inject


class ListAdapter @Inject constructor(
    private val glide: RequestManager
) : RecyclerView.Adapter<ListAdapter.ListViewHolder>() {

    private val diffCallback = object : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.uid == newItem.uid
        }

    }

    private val differ = AsyncListDiffer(this, diffCallback)

    var users: List<User>
        get() = differ.currentList
        set(value) = differ.submitList(value)

    inner class ListViewHolder(binding: ItemListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val ivProfilePic: ShapeableImageView = binding.ivProfilePictureList
        val tvEnrolNo: TextView = binding.tvEnrolNoList
        val tvName: TextView = binding.tvName
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
        val binding = ItemListBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
        val user = users[position]
        holder.apply {
            val decodedByteArray: ByteArray = Base64.decode(user.byteArray, Base64.DEFAULT)
            glide.load(decodedByteArray).into(ivProfilePic)
            tvEnrolNo.text = "Enrolment No.: ${user.enrollment}"
            tvName.text = "Name: ${user.name}"

            itemView.setOnClickListener {
                onUserClickListener?.let { click ->
                    click(user)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return users.size
    }

    private var onUserClickListener: ((User) -> Unit)? = null

    fun setOnUserClickListener(listener: (User) -> Unit) {
        onUserClickListener = listener
    }

}