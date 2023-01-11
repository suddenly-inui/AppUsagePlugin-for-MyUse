package com.awareframework.android.sensor.aware_appusage

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.IBinder
import android.os.Build;
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import java.util.Calendar

import android.app.PendingIntent
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.*
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat

import com.awareframework.android.core.AwareSensor
import com.awareframework.android.core.model.SensorConfig
import com.awareframework.android.sensor.aware_appusage.model.AppusageData


/**
 **
 **/


class AppusageSensor : AwareSensor(){

    companion object {
        const val TAG = "AWARE::Appusage"
        const val CHANNEL_ID = "appusage_notification"

        const val ACTION_AWARE_APPUSAGE = "ACTION_AWARE_APPUSAGE"

        const val ACTION_AWARE_APPUSAGE_START = "com.awareframework.android.sensor.aware_appusage.SENSOR_START"
        const val ACTION_AWARE_APPUSAGE_STOP = "com.awareframework.android.sensor.aware_appusage.SENSOR_STOP"

        const val ACTION_AWARE_APPUSAGE_SET_LABEL = "com.awareframework.android.sensor.aware_appusage.ACTION_AWARE_APPUSAGE_SET_LABEL"
        const val EXTRA_LABEL = "label"

        const val ACTION_AWARE_APPUSAGE_SYNC = "com.awareframework.android.sensor.aware_appusage.SENSOR_SYNC"

        val CONFIG = Config()

        @RequiresApi(Build.VERSION_CODES.O)
        fun start(context: Context, config: Config? = null) {
            if (config != null)
                CONFIG.replaceWith(config)

            context.startForegroundService(Intent(context, AppusageSensor::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AppusageSensor::class.java))
        }
    }

    private lateinit var sensorThread: HandlerThread
    private lateinit var sensorHandler: Handler
    private var lastEvents:MutableList<AppusageData> = mutableListOf<AppusageData>()
    lateinit var sharedPref: SharedPreferences


    val runnable = object : Runnable {
        override fun run() {
            var data:MutableList<AppusageData> = readAppUsingTimingToday() ?: return

            //lastEvents(既に取得保存したデータ)を除いたデータがonDataChangedに流すべき新しいデータ
            var correct_data:MutableList<AppusageData> = data.subtract(lastEvents).toMutableList()

            if(!correct_data.isNullOrEmpty()){
                saveBuffer(correct_data)
                CONFIG.sensorObserver?.onDataChanged(correct_data)
            }

            lastEvents = data
            sensorHandler.postDelayed(this, CONFIG.interval.toLong())
        }
    }

    fun saveBuffer(dataBuffer: MutableList<AppusageData>) {
        val data: Array<AppusageData> = dataBuffer.toTypedArray()
        dbEngine?.save(data, AppusageData.TABLE_NAME)
    }

    private val appusageReceiver = object: BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return
            when(intent.action){
                ACTION_AWARE_APPUSAGE_SET_LABEL -> {
                    intent.getStringExtra(EXTRA_LABEL)?.let{
                        CONFIG.label = it
                    }
                }
                ACTION_AWARE_APPUSAGE_SYNC -> onSync(intent)
            }
        }
    }

    //~~~~~lifecycles~~~
    override fun onCreate() {
        super.onCreate()

        sharedPref = getSharedPreferences("appusage_sharedpref", Context.MODE_PRIVATE)

        createNotificationChannel()
        initializeDbEngine(CONFIG)

        sensorThread = HandlerThread(TAG)
        sensorThread.start()
        sensorHandler = Handler(sensorThread.looper)
        registerReceiver(appusageReceiver, IntentFilter().apply {
            addAction(ACTION_AWARE_APPUSAGE_SET_LABEL)
            addAction(ACTION_AWARE_APPUSAGE_SYNC)
        })

        logd("Appusage service created.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        val openIntent = Intent(this, AppusageSensor::class.java).let {
            PendingIntent.getActivity(this, 0, it, 0)
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_aware_accessibility_white)
            .setContentTitle(CONFIG.awareUsageAppNotificationTitle)
            .setContentText(CONFIG.awareUsageAppNotificationDescription)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openIntent)
            .build()
        startForeground(8000, notification)

        sensorHandler.post(runnable)

        sendBroadcast(Intent(ACTION_AWARE_APPUSAGE))

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CONFIG.awareUsageAppNoticationId,
                CONFIG.awareUsageAppNotificationTitle,
                NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = CONFIG.awareUsageAppNotificationDescription
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        dbEngine?.close()
        unregisterReceiver(appusageReceiver)
        sensorHandler.removeCallbacks(runnable)
        logd("Appusage service terminated...")
    }

    override fun onSync(intent: Intent?) {
        println("sync started!!!")
        dbEngine?.startSync(AppusageData.TABLE_NAME)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    interface Observer {
        fun onDataChanged(datas: MutableList<AppusageData>?)
    }

    private fun getUsageStatsEventsObject():UsageEvents?{
        var usageStatsManager:UsageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        var calendar:Calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
            return usageStatsManager.queryEvents(calendar.timeInMillis, System.currentTimeMillis())
        }else{
            println("return null in getUsageStatsEventsObject")
            return null
        }
    }

    private fun readAppUsingTimingToday():MutableList<AppusageData>?{
        var event:UsageEvents.Event = UsageEvents.Event()
        var mtl = mutableListOf<AppusageData>()

        var eventStats:UsageEvents = getUsageStatsEventsObject() ?: run{
            println("eventStatsがnullです。 in readAppUsingTimingToday")
            return mtl
        }

        while(eventStats.hasNextEvent()){
            eventStats.getNextEvent(event)
            var data = AppusageData().apply {
                appPackageName = event.packageName
                timestamp = event.timeStamp
                eventTimestamp = event.timeStamp
                eventType = event.eventType
                deviceId = CONFIG.deviceId
                label = CONFIG.label
            }
            if(CONFIG.usageAppDisplaynames.contains(data.appPackageName) &&
                CONFIG.usageAppEventTypes.contains(data.eventType)){
                mtl.add(data)
            }

        }
        return mtl
    }

    data class Config(

        var sensorObserver: Observer? = null,
        var interval: Int = 60000,
        var usageAppDisplaynames:MutableList<String> = mutableListOf<String>(""),
        var usageAppEventTypes:MutableList<Int> = mutableListOf<Int>(),
        var awareUsageAppNotificationTitle: String = "",
        var awareUsageAppNotificationDescription: String = "",
        var awareUsageAppNoticationId:  String = "",

    ) : SensorConfig(dbPath = "aware_appusage") {

        override fun <T : SensorConfig> replaceWith(config: T) {
            super.replaceWith(config)

            if (config is Config) {
                sensorObserver = config.sensorObserver
                interval = config.interval
                usageAppDisplaynames = config.usageAppDisplaynames
                usageAppEventTypes = config.usageAppEventTypes
                awareUsageAppNotificationTitle = config.awareUsageAppNotificationTitle
                awareUsageAppNotificationDescription = config.awareUsageAppNotificationDescription
                awareUsageAppNoticationId = config.awareUsageAppNoticationId
            }
        }
    }

    class AppusageSensorBroadcastReceiver : AwareSensor.SensorBroadcastReceiver() {

        @RequiresApi(Build.VERSION_CODES.O)
        override fun onReceive(context: Context?, intent: Intent?) {
            context ?: return

            logd("Sensor broadcast received. action: " + intent?.action)

            when (intent?.action) {
                SENSOR_START_ENABLED -> {
                    logd("Sensor enabled: " + CONFIG.enabled)

                    if (CONFIG.enabled) {
                        start(context)
                    }
                }

                ACTION_AWARE_APPUSAGE_STOP,
                SENSOR_STOP_ALL -> {
                    logd("Stopping sensor.")
                    stop(context)
                }

                ACTION_AWARE_APPUSAGE_START -> {
                    start(context)
                }
            }
        }
    }
}


private fun logd(text: String) {
    if (AppusageSensor.CONFIG.debug) Log.d(AppusageSensor.TAG, text)
}

private fun logw(text: String) {
    Log.w(AppusageSensor.TAG, text)
}