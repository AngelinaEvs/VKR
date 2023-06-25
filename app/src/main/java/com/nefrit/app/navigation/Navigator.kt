package com.nefrit.app.navigation

import android.content.Context
import androidx.navigation.NavController
import com.nefrit.app.app.MainActivity
import com.nefrit.app.R
import com.nefrit.app.app.HelloArActivity
import com.nefrit.splash.SplashRouter
import com.nefrit.users.UsersRouter
import com.nefrit.users.presentation.details.PatientFragment

class Navigator : UsersRouter, SplashRouter {

    private var navController: NavController? = null

    fun attachNavController(navController: NavController, graph: Int) {
        navController.setGraph(graph)
        this.navController = navController
    }

    fun detachNavController(navController: NavController) {
        if (this.navController == navController) {
            this.navController = null
        }
    }

    override fun openUser(userId: Int) {
        navController?.navigate(R.id.userFragment, PatientFragment.createBundle(userId))
    }

    override fun returnToUsers() {
        navController?.popBackStack()
    }

    override fun openMain(context: Context) {
        HelloArActivity.start(context)
    }
}