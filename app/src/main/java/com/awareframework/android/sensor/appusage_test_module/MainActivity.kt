package com.awareframework.android.sensor.appusage_test_module

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import androidx.annotation.RequiresApi
import android.app.usage.UsageEvents
import com.awareframework.android.core.db.Engine
import com.awareframework.android.sensor.aware_appusage.AppusageSensor
import com.awareframework.android.sensor.aware_appusage.model.AppusageData

class MainActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if(!checkReadStatsPermission()){
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

        // To start the service.
        AppusageSensor.start(applicationContext, AppusageSensor.Config().apply {

            interval = 60000//1分
            usageAppDisplaynames = mutableListOf("com.twitter.android", "com.facebook.orca", "com.facebook.katana", "com.instagram.android", "jp.naver.line.android", "com.ss.android.ugc.trill")
            usageAppEventTypes = mutableListOf(UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.ACTIVITY_RESUMED)
            dbHost = ""
            deviceId = "app_usage_test_Id"
            label = "android_deviceid_label_test"

            awareUsageAppNotificationTitle = "studying now"
            awareUsageAppNotificationDescription = "App usage history is being retrieved."
            awareUsageAppNoticationId = "appusage_notification"

            dbType = Engine.DatabaseType.ROOM

            sensorObserver = object : AppusageSensor.Observer {
                override fun onDataChanged(datas: MutableList<AppusageData>?) {
                    println("ondatachanged in mainActivity ${datas}")
                }
            }
        })

        setContentView(R.layout.activity_main)
    }

    override fun onStop() {
        super.onStop()
        Intent().also { intent ->
            intent.action = AppusageSensor.ACTION_AWARE_APPUSAGE_SYNC
            sendBroadcast(intent)
        }
    }


    //TODO:このパーミッションチェックは、ライブラリ側に持たせたい
    private fun checkReadStatsPermission():Boolean{
        var aom: AppOpsManager = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        var mode:Int = aom.checkOp(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
        if(mode == AppOpsManager.MODE_DEFAULT){
            return checkPermission("android.permission.PACKAGE_USAGE_STATS", android.os.Process.myPid(), android.os.Process.myUid()) == PackageManager.PERMISSION_GRANTED
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }
}