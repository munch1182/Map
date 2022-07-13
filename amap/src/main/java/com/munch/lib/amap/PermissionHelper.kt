package com.munch.lib.amap

import android.Manifest
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.provider.Settings
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.amap.api.location.AMapLocationClient
import com.munch.lib.AppHelper
import com.munch.lib.extend.isPermissionGranted
import com.munch.lib.result.ResultHelper
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object PermissionHelper {

    fun check(context: Context = AppHelper.ctx): Boolean {
        return context.isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    fun updatePrivacy(context: Context = AppHelper.ctx) {
        AMapLocationClient.updatePrivacyShow(context, true, true)
        AMapLocationClient.updatePrivacyAgree(context, true)
    }

    suspend fun checkOrRequest(fragment: Fragment): Boolean {
        return suspendCancellableCoroutine {
            ResultHelper.with(fragment)
                .contact(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                .contact({
                    (it.getSystemService(Context.LOCATION_SERVICE) as? LocationManager)?.isProviderEnabled(
                        LocationManager.GPS_PROVIDER
                    ) ?: false
                }, { Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS) })
                .start { res ->
                    it.resume(res)
                }
        }
    }

    suspend fun checkOrRequest(activity: FragmentActivity): Boolean {
        return suspendCancellableCoroutine {
            ResultHelper.with(activity)
                .contact(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                )
                .contact({ it.isGPSEnable() }, { Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS) })
                .start { res ->
                    it.resume(res)
                }
        }
    }

    fun Context.isGPSEnable() =
        (getSystemService(Context.LOCATION_SERVICE) as? LocationManager)
            ?.isProviderEnabled(LocationManager.GPS_PROVIDER) ?: false

}