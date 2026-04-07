package com.ivoryapp.nurseflow.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ivoryapp.nurseflow.databinding.ItemColleagueBinding

data class Colleague(
    val uid: String,
    val name: String,
    val userCode: String,
    val photoUrl: String? = null
)

class ColleagueAdapter(
    private val onClick: (Colleague) -> Unit,
    private val onLongClick: (Colleague) -> Unit = {}
) : ListAdapter<Colleague, ColleagueAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(private val binding: ItemColleagueBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(colleague: Colleague, onClick: (Colleague) -> Unit, onLongClick: (Colleague) -> Unit) {
            binding.tvColleagueName.text = colleague.name
            binding.tvColleagueCode.text = colleague.userCode
            
            Glide.with(binding.ivColleagueProfile.context)
                .load(colleague.photoUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .circleCrop()
                .into(binding.ivColleagueProfile)

            binding.root.setOnClickListener { onClick(colleague) }
            binding.root.setOnLongClickListener {
                onLongClick(colleague)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemColleagueBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onClick, onLongClick)
    }

    object DiffCallback : DiffUtil.ItemCallback<Colleague>() {
        override fun areItemsTheSame(oldItem: Colleague, newItem: Colleague) = oldItem.uid == newItem.uid
        override fun areContentsTheSame(oldItem: Colleague, newItem: Colleague) = oldItem == newItem
    }
}
