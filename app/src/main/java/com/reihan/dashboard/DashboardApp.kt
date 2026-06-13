package com.reihan.dashboard

import android.app.Application
import com.reihan.dashboard.data.network.SessionManager

class DashboardApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SessionManager.init(this)
    }
}
