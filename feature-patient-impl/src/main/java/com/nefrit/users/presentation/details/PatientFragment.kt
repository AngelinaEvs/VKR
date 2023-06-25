package com.nefrit.users.presentation.details

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.itis.users.R
import com.jjoe64.graphview.DefaultLabelFormatter
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import com.nefrit.common.base.BaseFragment
import com.nefrit.common.di.FeatureUtils
import com.nefrit.feature_user_api.di.PatientFeatureApi
import com.nefrit.users.di.PatientFeatureComponent
import kotlinx.android.synthetic.main.fragment_patient.*

class PatientFragment : BaseFragment<PatientViewModel>() {

    companion object {
        private const val KEY_PATIENT_ID = "patient_id"

        fun createBundle(id: Int): Bundle {
            return Bundle().apply { putInt(KEY_PATIENT_ID, id) }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_patient, container, false)
    }

    override fun inject() {
        val id = arguments!!.getInt(KEY_PATIENT_ID, 0)

        FeatureUtils.getFeature<PatientFeatureComponent>(this, PatientFeatureApi::class.java)
            .patientComponentFactory()
            .create(this, id)
            .inject(this)
    }

    override fun initViews() {
        toolbar.setTitle("Динамика")
        toolbar.setHomeButtonListener { viewModel.backClicked() }
        toolbar.showHomeButton()

        photosList.adapter = PhotoAdapter { }

        graph.viewport.setScalableY(true);
        graph.viewport.isScalable = true;

        graph.gridLabelRenderer.labelFormatter = object : DefaultLabelFormatter() {

            override fun formatLabel(value: Double, isValueX: Boolean): String {
                if (isValueX) {
                    if (value == 0.0) return ""
                    return super.formatLabel(value, isValueX) + " день"
                }
                return super.formatLabel(value, isValueX) + " см²"
            }
        }
    }

    override fun subscribe(viewModel: PatientViewModel) {
        viewModel.patientLiveData.observe(this, Observer {
            graph.addSeries(
                LineGraphSeries(
                    it.standart.map { DataPoint(it.first, it.second) }.toTypedArray()
                ).apply { color = Color.RED }
            )
            graph.addSeries(
                LineGraphSeries(
                    it.byClass.map { DataPoint(it.first, it.second) }.toTypedArray()
                )
            )
            graph.addSeries(
                LineGraphSeries(
                    it.dynamics.map { DataPoint(it.first, it.second) }.toTypedArray()
                ).apply { color = Color.GRAY }
            )
            (photosList.adapter as PhotoAdapter).submitList(
                PhotoDataModel(
                    it.photoData.uri,
                    it.photoData.dayNumber
                )
            )
        })
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == android.R.id.home) {
            viewModel.backClicked()
        }
        return super.onOptionsItemSelected(menuItem)
    }
}