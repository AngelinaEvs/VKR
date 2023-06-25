package com.nefrit.users.presentation.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.itis.users.R
import com.nefrit.common.Patient
import kotlinx.android.synthetic.main.item_patient.view.*

class PatientAdapter(
    private val clickListener: (Patient) -> Unit
) : ListAdapter<Patient, PatientViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_patient, parent, false)
        return PatientViewHolder(view)
    }

    override fun onBindViewHolder(holder: PatientViewHolder, position: Int) {
        holder.bind(getItem(position), clickListener)
    }
}

class PatientViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    fun bind(patient: Patient, clickListener: (Patient) -> Unit) {
        with(itemView) {
            firstNameTv.text = patient.id.toString()

            setOnClickListener {
                clickListener(patient)
            }
        }
    }
}

object DiffCallback : DiffUtil.ItemCallback<Patient>() {
    override fun areItemsTheSame(oldItem: Patient, newItem: Patient): Boolean {
        return false
    }

    override fun areContentsTheSame(oldItem: Patient, newItem: Patient): Boolean {
        return false
    }
}