package com.munch.project.map

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.amap.api.location.AMapLocationClientOption
import com.munch.lib.AppHelper
import com.munch.lib.amap.Location
import com.munch.lib.amap.LocationHelper
import com.munch.lib.amap.OnLocationUpdateListener
import com.munch.lib.amap.PermissionHelper
import com.munch.lib.extend.bind
import com.munch.lib.extend.init
import com.munch.lib.extend.lazy
import com.munch.lib.fast.base.BaseFastActivity
import com.munch.project.map.AddressRecord.Companion.into
import com.munch.project.map.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

class MainActivity : BaseFastActivity(), OnLocationUpdateListener {

    private val bind by bind<ActivityMainBinding>()
    private val loc by lazy { LocationHelper() }
    private var sportId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind.init()
        AppHelper.init(application)

        lifecycleScope.launch {
            PermissionHelper.updatePrivacy()
            if (PermissionHelper.checkOrRequest(this@MainActivity)) {
                loc.add(this@MainActivity)
                bind.start.tag = false
                bind.start.setOnClickListener {
                    val b = it.tag as? Boolean == true
                    if (b) {
                        bind.start.text = "START"
                        bind.desc.text = "stop location"
                        loc.stop()
                        Record.nextSportID()
                        index.set(0)
                    } else {
                        bind.start.text = "STOP"
                        sportId = Record.getSportID()
                        bind.desc.text = "start location"
                        loc.start(true, generateOpt())
                    }
                    bind.start.tag = !b
                }
            }
        }
    }

    private fun generateOpt(): AMapLocationClientOption {
        val opt = AMapLocationClientOption()
        opt.locationMode = when {
            bind.modeGps.isChecked -> AMapLocationClientOption.AMapLocationMode.Device_Sensors
            bind.modeNet.isChecked -> AMapLocationClientOption.AMapLocationMode.Battery_Saving
            else -> AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
        }
        opt.interval = when {
            bind.interval1.isChecked -> 1000L
            bind.interval3.isChecked -> 3000L
            else -> 5000L
        }
        return opt
    }

    override fun setContentView(view: View?) {
        super.setContentView(view)
        super.onBar()
    }

    override fun onBar() {
    }

    private val index = AtomicInteger()

    override fun onLocationUpdate(location: Location?) {
        lifecycleScope.launch {
            val l = location?.into(sportId) ?: return@launch
            Record.add(l)
            withContext(Dispatchers.Main) {
                bind.desc.text =
                    "${index.getAndIncrement()}: (${l.longitude}, ${l.latitude}, ${l.altitude})"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        loc.destroy()
    }

}
