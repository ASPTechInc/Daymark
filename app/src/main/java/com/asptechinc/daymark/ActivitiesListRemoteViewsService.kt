package com.asptechinc.daymark

import android.content.Intent
import android.widget.RemoteViewsService

class ActivitiesListRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory = ActivitiesListRemoteViewsFactory(this.applicationContext)
}
