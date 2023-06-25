package com.nefrit.users.presentation.details

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.itis.users.R
import com.nefrit.common.utils.getFile
import kotlinx.android.synthetic.main.item_image.view.*

class PhotoAdapter(
    private val clickListener: (PhotoDataModel) -> Unit
) : ListAdapter<PhotoDataModel, PhotoHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_image, parent, false)
        return PhotoHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoHolder, position: Int) {
        holder.bind(getItem(position), clickListener)
    }
}

class PhotoHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    fun bind(photo: PhotoDataModel, clickListener: (PhotoDataModel) -> Unit) {
        with(itemView) {
            img.setImageDrawable(
                getFile(photo.uri)
            )
            number.text = photo.day + " день"

            setOnClickListener {
                clickListener(photo)
            }
        }
    }
}

object DiffCallback : DiffUtil.ItemCallback<PhotoDataModel>() {
    override fun areItemsTheSame(oldItem: PhotoDataModel, newItem: PhotoDataModel): Boolean {
        return false
    }

    override fun areContentsTheSame(oldItem: PhotoDataModel, newItem: PhotoDataModel): Boolean {
        return false
    }
}

data class PhotoDataModel(
    val uri: String,
    val day: String
)