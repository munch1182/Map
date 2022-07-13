package com.munch.lib.amap

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener
import com.munch.lib.AppHelper
import com.munch.lib.helper.ARSHelper
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.security.auth.Destroyable
import kotlin.coroutines.resume

/**
 * 获取定位
 * https://lbs.amap.com/api/android-location-sdk/guide/android-location/getlocation
 */
class LocationHelper(private val context: Context = AppHelper.ctx) : AMapLocationListener,
    Destroyable, ARSHelper<OnLocationUpdateListener>() {

    private var client: AMapLocationClient? = null
    private var isKeepLocation = false

    /**
     * 获取当前定位, 即定位一次
     */
    suspend fun requireNow(): Location? = suspendCancellableCoroutine {
        add(object : OnLocationUpdateListener {
            override fun onLocationUpdate(location: Location?) {
                remove(this)
                it.resume(location)
            }
        })
        start(false, AMapLocationClientOption().apply {
            isOnceLocation = true
            locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
        })
    }

    fun requireKeep(interval: Long = 1000L) = start(true, AMapLocationClientOption().apply {
        isOnceLocation = false
        setInterval(interval)
        locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
    })

    fun start(keep: Boolean = true, option: AMapLocationClientOption) {
        if (isKeepLocation) { //如果正在持续定位, 则不需要再开启
            return
        }
        if (client == null) {
            client = AMapLocationClient(context)
            val client = client ?: return
            client.setLocationListener(this)
            client.enableBackgroundLocation(712, getNotification())
        }
        client?.setLocationOption(option)
        isKeepLocation = keep
        client?.startLocation()
    }

    private fun getNotification(): Notification {

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = AppHelper.ctx.packageName
            val channel =
                NotificationChannel(channelId, channelId, NotificationManager.IMPORTANCE_HIGH)
            (AppHelper.ctx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager?)
                ?.createNotificationChannel(channel)
            Notification.Builder(AppHelper.ctx, channelId)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(AppHelper.ctx)
        }
        builder.setSmallIcon(androidx.core.R.drawable.notification_icon_background)
            .setContentTitle("LOCATION")
            .setContentText("正在运行")
            .setWhen(System.currentTimeMillis())
        return builder.build()
    }

    override fun onLocationChanged(p0: AMapLocation?) {
        notifyUpdate { it.onLocationUpdate(if ((p0?.errorCode ?: 0) != 0) null else p0?.into()) }
        if (!isKeepLocation) {
            stop()
        }
    }

    fun stop() {
        isKeepLocation = false
        client?.stopLocation()
    }

    override fun destroy() {
        super.destroy()
        client?.onDestroy()
        client = null
    }

    private fun AMapLocation.into(): Location? {
        if (this.errorCode != 0) {
            return null
        }
        val laLng = LaLng(this.latitude, this.longitude, this.altitude)
        val code = AddressCode(this.cityCode, this.adCode)
        val desc = AddressDesc(this.country, this.province, this.city, this.district, this.street)
        return Location(laLng, Address(code, desc))
    }

}