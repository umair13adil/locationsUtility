package com.embraceit.batchdrawgeolocations

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btn_ray_cast.setOnClickListener {
            startActivity(Intent(this, RayCastingActivity::class.java))
        }

        btn_maps.setOnClickListener {
            startActivity(Intent(this, MapsActivity::class.java))
        }

        btn_draw_markers.setOnClickListener {
            startActivity(Intent(this, CoordinatesMapperActivity::class.java))
        }

        btn_geo_fence.setOnClickListener {
            startActivity(Intent(this, GeoFenceActivity::class.java))
        }
    }
}
