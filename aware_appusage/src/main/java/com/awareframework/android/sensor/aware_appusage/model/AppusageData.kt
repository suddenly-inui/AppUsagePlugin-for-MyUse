package com.awareframework.android.sensor.aware_appusage.model


import com.awareframework.android.core.model.AwareObject
import com.google.gson.Gson

/**
 * Contains the raw sensor data.
 *
 * @author  sercant
 * @date 20/08/2018
 */
data class AppusageData(
    var eventTimestamp: Long = 0L,
    var appPackageName: String = "",
    var eventType:Int = 0,

) : AwareObject(jsonVersion = 1) {
    companion object {
        const val TABLE_NAME = "AppusageData"
    }

    override fun toString(): String = Gson().toJson(this)
}