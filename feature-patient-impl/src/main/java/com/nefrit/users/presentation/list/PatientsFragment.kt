package com.nefrit.users.presentation.list

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.itis.users.R
import com.nefrit.common.base.BaseFragment
import com.nefrit.common.di.FeatureUtils
import com.nefrit.feature_user_api.di.PatientFeatureApi
import com.nefrit.users.di.PatientFeatureComponent
import kotlinx.android.synthetic.main.alert_dialog.view.*
import kotlinx.android.synthetic.main.fragment_patients.*

class PatientsFragment : BaseFragment<PatientsViewModel>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_patients, container, false)
    }

    override fun inject() {
        FeatureUtils.getFeature<PatientFeatureComponent>(this, PatientFeatureApi::class.java)
            .patientsComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun initViews() {
        toolbar.setTitle(getString(R.string.users_title))

        usersRv.layoutManager = LinearLayoutManager(activity!!)
        usersRv.setHasFixedSize(true)

        alertDialog()
    }

    override fun subscribe(viewModel: PatientsViewModel) {
        viewModel.usersLiveData.observe(this, Observer {
            if (usersRv.adapter == null) {
                usersRv.adapter = PatientAdapter { viewModel.userClicked(it) }
            }
            (usersRv.adapter as PatientAdapter).submitList(it)
        })

        viewModel.getUsers()
    }

    private fun alertDialog() {
        val li = LayoutInflater.from(requireContext())
        val promptsView: View = li.inflate(R.layout.alert_dialog, null)
        val alertDialogBuilder: AlertDialog.Builder = AlertDialog.Builder(
            requireContext()
        )
        val userInput = promptsView.findViewById<View>(R.id.editText) as EditText
        promptsView.save.setOnClickListener {
            viewModel.addNew(userInput.text.toString())

        }
        alertDialogBuilder.setView(promptsView)
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }
}