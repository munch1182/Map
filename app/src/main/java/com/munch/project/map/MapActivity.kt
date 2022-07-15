package com.munch.project.map

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.MyLocationStyle
import com.amap.api.maps.model.PolylineOptions
import com.munch.lib.extend.bind
import com.munch.lib.extend.lazy
import com.munch.lib.fast.base.BaseFastActivity
import com.munch.lib.fast.view.ISupportActionBar
import com.munch.project.map.databinding.ActivityMapBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MapActivity : BaseFastActivity(), ISupportActionBar {

    companion object {

        private const val KEY_SPORT_ID = "sport_id"

        fun show(context: Context, id: Int) {
            context.startActivity(Intent(context, MapActivity::class.java).apply {
                putExtra(KEY_SPORT_ID, id)
            })
        }
    }

    private val bind by bind<ActivityMapBinding>()
    private val map by lazy { bind.map }
    private val amap by lazy { bind.map.map }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        map.onCreate(savedInstanceState)

        amap.myLocationStyle = MyLocationStyle().apply {
            amap.isMyLocationEnabled = false
            myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER)
        }

        val id = intent?.getIntExtra(KEY_SPORT_ID, -1)?.takeIf { it != -1 } ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            val list = Record.queryAllById(id)

            val laLng = list.map { LatLng(it.latitude, it.longitude) }
                .takeIf { it.isNotEmpty() }
                ?: return@launch
            val l = laLng.map { MarkerOptions().position(it) }

            amap.addPolyline(PolylineOptions().addAll(laLng).color(Color.RED))
            amap.addMarkers(ArrayList<MarkerOptions?>().apply { addAll(l) }, false)
            amap.moveCamera(CameraUpdateFactory.newLatLng(laLng.first()))
            amap.moveCamera(CameraUpdateFactory.zoomTo(16f))
        }
    }

    override fun onDestroy() {
        super<BaseFastActivity>.onDestroy()
        map.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        map.onSaveInstanceState(outState)
    }

    override fun updateViewColor() {
        /*super.updateViewColor()*/
    }
}