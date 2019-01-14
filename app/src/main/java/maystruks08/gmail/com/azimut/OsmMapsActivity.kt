package maystruks08.gmail.com.azimut

import android.Manifest
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import android.location.LocationManager
import kotlinx.android.synthetic.main.activity_main.*
import org.osmdroid.views.CustomZoomButtonsController
import android.location.Location
import android.location.LocationListener
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.DialogOnDeniedPermissionListener
import com.karumi.dexter.listener.single.PermissionListener
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.infowindow.BasicInfoWindow
import kotlin.math.cos
import kotlin.math.sin


class OsmMapsActivity : AppCompatActivity() {

    private var MAP_DEFAULT_LATITUDE = 46.484579
    private var MAP_DEFAULT_LONGITUDE = 30.732597

    private lateinit var marker: Marker


    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance()
            .load(applicationContext, PreferenceManager.getDefaultSharedPreferences(applicationContext))
        setContentView(R.layout.activity_main)

        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        map.controller.setZoom(10.0)
        map.controller.setCenter(GeoPoint(MAP_DEFAULT_LATITUDE, MAP_DEFAULT_LONGITUDE))
        setCompassOverlay()
        marker = Marker(map)
        setMarkerPosition(GeoPoint(MAP_DEFAULT_LATITUDE, MAP_DEFAULT_LONGITUDE))

        initViews()
    }


    private fun initViews() {

        checkboxLocation.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                checkLocationPermission()
            } else {
                etLat.visibility = View.VISIBLE
                etLon.visibility = View.VISIBLE
            }
        }

        btnComplete.setOnClickListener {
            if (checkboxLocation.isChecked) {

                if (etAzimut.text.toString() != "") {
                    findAzimutPoint()
                } else {
                    Toast.makeText(applicationContext, "Field input", Toast.LENGTH_SHORT)
                        .show()
                }


            } else {
                if (etAzimut.text.toString() != "" && etLat.text.toString() != "" && etLon.text.toString() != "") {
                    findAzimutPointWithoutGps()
                } else Toast.makeText(applicationContext, "Field input", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }


    private fun findAzimutPointWithoutGps() {
        val currentLocationPoint = GeoPoint(etLat.text.toString().toDouble(), etLon.text.toString().toDouble())
        setMarkerPosition(currentLocationPoint)

        val length = 10
        val x = etLat.text.toString().toDouble() + length * cos(Math.toRadians(etAzimut.text.toString().toDouble()))
        val y = etLon.text.toString().toDouble() + length * sin(Math.toRadians(etAzimut.text.toString().toDouble()))

        val endLocationPoint = GeoPoint(x, y)
        val points = listOf(currentLocationPoint, endLocationPoint)

        val line = Polyline(map)
        line.width = 5f
        line.setPoints(points)
        line.isGeodesic = true
        line.infoWindow = BasicInfoWindow(R.layout.bonuspack_bubble, map)
        map.overlayManager.add(line)
        line.setOnClickListener { polyline, mapView, eventPos -> map.overlayManager.remove(line) }

        map.invalidate()
    }


    private fun findAzimutPoint() {
        val currentLocationPoint = GeoPoint(MAP_DEFAULT_LATITUDE, MAP_DEFAULT_LONGITUDE)
        setMarkerPosition(currentLocationPoint)

        val length = 10
        val x = MAP_DEFAULT_LATITUDE + length * cos(Math.toRadians(etAzimut.text.toString().toDouble()))
        val y = MAP_DEFAULT_LONGITUDE + length * sin(Math.toRadians(etAzimut.text.toString().toDouble()))

        val endLocationPoint = GeoPoint(x, y)
        val points = listOf(currentLocationPoint, endLocationPoint)

        val line = Polyline(map)
        line.width = 5f
        line.setPoints(points)
        line.isGeodesic = true
        line.infoWindow = BasicInfoWindow(R.layout.bonuspack_bubble, map)
        map.overlayManager.add(line)
        line.setOnClickListener { polyline, mapView, eventPos -> map.overlayManager.remove(line) }

        map.invalidate()
    }


    private fun findLocation() {

        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

            val locationListener = object : LocationListener {

                override fun onLocationChanged(location: Location) {
                    setMarkerPosition(GeoPoint(location.latitude, location.longitude))
                }

                override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
                }

                override fun onProviderEnabled(provider: String) {
                }

                override fun onProviderDisabled(provider: String) {
                }
            }

            try {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0f, locationListener)
            } catch (e: SecurityException) {
                Log.e("location", e.localizedMessage)
            }

        } else {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("GPS disable")
            builder.setMessage("Turn on GPS?")
            builder.setPositiveButton("YES") { _, _ ->

                checkboxLocation.isChecked = false
                Toast.makeText(applicationContext, "Go to settings and turn on GPS", Toast.LENGTH_SHORT).show()
            }
            builder.setNegativeButton("No") { _, _ ->
                checkboxLocation.isChecked = false
                Toast.makeText(applicationContext, "Забивай значения руцями раз такой ленивый", Toast.LENGTH_SHORT)
                    .show()
            }
            builder.show()
        }
    }


    fun setMarkerPosition(point: GeoPoint) {
        marker.position = point
        marker.icon = getDrawable(R.drawable.marker_default_focused_base)
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.isDraggable = true
        map.overlays.add(marker)
        MAP_DEFAULT_LATITUDE = point.latitude
        MAP_DEFAULT_LONGITUDE = point.longitude
        map.controller.setCenter(point)


        marker.setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
            override fun onMarkerDragEnd(marker: Marker?) {
                if (marker != null) {

                    MAP_DEFAULT_LATITUDE = marker.position.latitude
                    MAP_DEFAULT_LONGITUDE = marker.position.longitude
                    map.controller.setCenter(marker.position)

                    etLat.setText(marker.position.latitude.toString())
                    etLon.setText(marker.position.longitude.toString())
                }
            }

            override fun onMarkerDragStart(marker: Marker?) {
            }

            override fun onMarkerDrag(marker: Marker?) {
            }
        })
    }


    private fun setCompassOverlay() {
        val compass = CompassOverlay(this, InternalCompassOrientationProvider(this), map)
        compass.enableCompass()
        map.overlays.add(compass)
        compass.setCompassCenter(30f, 500f)
    }

    public override fun onResume() {
        super.onResume()
        map.onResume()
    }

    public override fun onPause() {
        super.onPause()
        map.onPause()
    }

    private fun checkLocationPermission() {
        Dexter.withActivity(this)
            .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse) {

                    findLocation()
                    etLat.visibility = View.GONE
                    etLon.visibility = View.GONE
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse) {

                    etLat.visibility = View.VISIBLE
                    etLon.visibility = View.VISIBLE

                    DialogOnDeniedPermissionListener.Builder
                        .withContext(applicationContext)
                        .withTitle("Location permission")
                        .withMessage("Find location permission is needed to take coordinate you city")
                        .withButtonText(android.R.string.ok)
                        .withIcon(R.mipmap.ic_launcher)
                        .build()
                }

                override fun onPermissionRationaleShouldBeShown(
                    permission: PermissionRequest,
                    token: PermissionToken
                ) {
                }
            }).check()

    }
}
