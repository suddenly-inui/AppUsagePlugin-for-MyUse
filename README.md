# Aware_AppUsage_Plugin

https://jitpack.io/#KarasawaTakumi0621/aware_appusage_plugin/ver1.0

## Overview
This plugin enables [UsageStatsEvent data](https://developer.android.com/reference/kotlin/android/app/usage/UsageEvents.Event) streaming, DB storage, storage on AWARE-micro server, etc. using [AWARE Framework's](https://awareframework.com/) [Core infrastructure.](https://github.com/awareframework/com.awareframework.android.core) 
This is an unofficial plugin of AWAREFramework. If you have any questions about this plugin, please contact the following Twitter account.

## Requirement
- Kotlin
- Java16
- Android 11

## Usage
### Install
We are using Jitpack, please add the following to the two files of Build.gradle.

(Root) Build.gradle
```(Root) Build.gradle
buildscript {
    repositories {
        google()
        mavenCentral()
        + maven { url 'https://jitpack.io' }
        + maven { url "https://s3.amazonaws.com/repo.commonsware.com" }
    }
~~~
}
```

(App) Build.gradle
```(App) Build.gradle
dependencies {
~~~
    + implementation 'com.github.KarasawaTakumi0621:aware_appusage_module:0.21'
}
```

Add the following to Setting.gradle.
```
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        jcenter()
        + maven { url 'https://jitpack.io' }
        + maven { url "https://s3.amazonaws.com/repo.commonsware.com" }
    }
}
```

## Example usage

```kotlin
// To start the service.
AppusageSensor.start(applicationContext, AppusageSensor.Config().apply {

    interval = 60000 //1min
    usageAppDisplaynames = mutableListOf("com.twitter.android", "com.facebook.orca", "com.facebook.katana", "com.instagram.android", "jp.naver.line.android", "com.ss.android.ugc.trill")
    usageAppEventTypes = mutableListOf(UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.ACTIVITY_RESUMED)
    
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
```

### AppUsage Data

Contains the raw sensor data.

| Field           | Type   | Description                                                         |
| ---------       | ------ | ------------------------------------------------------------------- |
| eventTimestamp  | Long   | Unix Time for starting/stopping applications, etc.                  |
| appPackageName  | String | Application name of the event that occurred                         |
| eventType       | Int    | Event ID of UsageEvent. https://developer.android.com/reference/kotlin/android/app/usage/UsageEvents.Event |
| label           | String | Customizable label. Useful for data calibration or traceability     |
| deviceId        | String | AWARE device UUID                                                   |
| label           | String | Customizable label. Useful for data calibration or traceability     |
| timestamp       | Long   | unixtime milliseconds since 1970                                    |
| timezone        | Int    | Raw timezone offset of the device                              |
| os              | String | Operating system of the device (ex. android)                        |

### AppUsage.Config
#### Fields

+ `usageAppDisplaynames: MutableList<String>`: Array of Application Display Names to be obtained
+ `usageAppEventTypes: MutableList<Int>`: Array of UsageEvent.Event IDs to be obtained
+ `awareUsageAppNotificationTitle`: Title of the notification when the Foreground Service is activated
+ `awareUsageAppNotificationDescription`: Description of the notification when the Foreground Service is activated
+ `awareUsageAppNoticationId`: ID of the notification when the Foreground Service is activated
+ `sensorObserver: AccelerometerSensor.Observer`: Callback for live data updates.
+ `interval: Int`: Data samples to collect per msec (default = 60000)
+ `debug: Boolean` enable/disable logging to `Logcat`. (default = `false`)
+ `label: String` Label for the data. (default = "")
+ `deviceId: String` Id of the device that will be associated with the events and the sensor. (default = "")
+ `dbEncryptionKey` Encryption key for the database. (default = `null`)
+ `dbType: Engine` Which db engine to use for saving data. (default = `Engine.DatabaseType.NONE`)
+ `dbPath: String` Path of the database. (default = "aware_accelerometer")
+ `dbHost: String` Host for syncing the database. (default = `null`)


## Reference
- https://awareframework.com/
- https://github.com/awareframework/com.awareframework.android.sensor.accelerometer

## Author

[twitter](https://twitter.com/TappunFox)
