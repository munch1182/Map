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
import com.munch.lib.extend.postUI
import com.munch.lib.fast.base.BaseFastActivity
import com.munch.lib.helper.NetHelper
import com.munch.lib.helper.ThreadHelper
import com.munch.project.map.AddressRecord.Companion.into
import com.munch.project.map.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

class MainActivity : BaseFastActivity(), OnLocationUpdateListener {

    private val bind by bind<ActivityMainBinding>()
    private val loc by lazy { LocationHelper() }
    private val netHelper by lazy { NetHelper.getInstance(ctx) }
    private val executor by lazy { ThreadHelper.newCachePool() }

    private var sportId = 0
    private var isLocation = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind.init()
        AppHelper.init(application)

        netHelper.observe(this) { showNetDesc() }
        bind.modeNet.setOnCheckedChangeListener { _, _ -> showNetDesc() }
        bind.modeDef.setOnCheckedChangeListener { _, _ -> showNetDesc() }

        bind.count.setOnClickListener {
            SportDialog().show(supportFragmentManager, null)
        }

        loc.set(this, this)

        bind.start.setOnClickListener {
            lifecycleScope.launch {
                PermissionHelper.updatePrivacy()
                if (PermissionHelper.checkOrRequest(this@MainActivity)) {
                    bind.start.isEnabled = true
                    lifecycleScope.launch(Dispatchers.IO) a@{
                        val b = isLocation

                        if (b) {
                            postUI {
                                bind.start.text = "START"
                                bind.desc.text = "stop location"
                            }

                            loc.stop()
                            index.set(0)
                            val sport = Record.getSportIdById(sportId)
                            if (sport != null) {
                                sport.count = Record.queryAddressCountBy(sport.id)
                                sport.endTime = System.currentTimeMillis()
                                Record.updateSport(sport)
                            }
                        } else {
                            postUI {
                                bind.start.text = "STOP"
                                bind.desc.text = "start location"
                            }

                            sportId = Record.getSportID() + 1
                            Record.addSport(SportIdRecord(sportId))
                            loc.start(true, generateOpt())
                         }
                        isLocation = !isLocation

                        withContext(Dispatchers.Main) { bind.count.isEnabled = !isLocation }
                    }
                }
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            Record.getSportIDCount()
                .collectLatest {
                    postUI {
                        bind.count.text = "当前共有 $it 次运动."
                        bind.count.isEnabled = it > 0 && !isLocation
                    }
                }
        }
    }

    private fun showNetDesc() {
        postUI {
            if (bind.modeNet.isChecked || bind.modeDef.isChecked) {
                val it = netHelper.curr
                if (it == null) {
                    bind.descNet.text = "当前没有网络连接"
                } else {
                    bind.descNet.text = "当前正在使用 ${netHelper.getName(it)}"
                }
            } else {
                bind.descNet.text = ""
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

    override fun updateViewColor() {
        /*super.updateViewColor()*/
    }
}
