package com.example.geoevent.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.view.MotionEvent
import android.view.ViewGroup
import com.example.geoevent.domain.LocationUseCase
import com.example.geoevent.domain.MapMarkerItem
import com.example.geoevent.domain.MapProvider
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import kotlin.math.cos
import kotlin.math.pow
//ce fichier implemente mapProvider avec cette carte specifique (OpenStreetMap) et c'est la
//qu'on peut changer le fichier si on voulait la carte google map
class OsmMapProvider : MapProvider {
    private lateinit var mapView: MapView

    // Réutilise la formule de distance déjà écrite pour le filtrage par rayon (Haversine),
    // pour savoir si deux événements sont "proches" et doivent être regroupés en cluster.
    private val locationUseCase = LocationUseCase()

    // Markers d'événements/clusters actuellement affichés (pas l'overlay d'écoute des
    // clics, ni le marker de sélection) pour pouvoir les retirer sans casser le reste de la carte.
    private val eventMarkers = mutableListOf<Marker>()

    // Marker temporaire affiché quand l'utilisateur tape sur la carte pour choisir un emplacement.
    private var selectionMarker: Marker? = null

    // Derniers events reçus + callback de clic, mémorisés pour pouvoir
    // recalculer le clustering à chaque changement de zoom.
    private var lastItems: List<MapMarkerItem> = emptyList()
    private var lastOnMarkerClick: ((String) -> Unit)? = null
    private var zoomListenerRegistered = false

    // Rayon de regroupement en pixels écran : deux events à moins de cette distance
    // visuelle l'un de l'autre sont fusionnés en un seul cluster.
    private val clusterRadiusPx = 70

    override fun initialize(context: Context) {
        Configuration.getInstance().userAgentValue = context.packageName
    }

    override fun showMap(container: ViewGroup) {
        mapView = MapView(container.context)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        container.addView(mapView)
    }

    override fun centerOn(lat: Double, lng: Double, zoom: Double) {
        val point = GeoPoint(lat, lng)
        mapView.controller.setZoom(zoom)
        mapView.controller.setCenter(point)
    }

    override fun zoomIn() {
        mapView.controller.zoomIn()
    }

    override fun zoomOut() {
        mapView.controller.zoomOut()
    }

    override fun addMarker(lat: Double, lng: Double, title: String, size: Int): Any {
        val marker = Marker(mapView)
        marker.position = GeoPoint(lat, lng)
        marker.title = title
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.icon = BitmapDrawable(mapView.context.resources, createSizedMarkerBitmap(size))
        mapView.overlays.add(marker)
        eventMarkers.add(marker)
        mapView.invalidate()
        return marker
    }

    /**
     * Dessine un disque dont le diamètre grandit avec "size" (= nombre de likes + 1).
     * On dessine nous-mêmes le bitmap plutôt que de redimensionner l'icône par défaut
     * d'osmdroid, pour ne pas dépendre de ses ressources internes.
     */
    private fun createSizedMarkerBitmap(size: Int): Bitmap {
        val density = mapView.context.resources.displayMetrics.density
        val baseDp = 28f
        val growthPerLikeDp = 5f
        val maxDp = 90f

        val likeCount = (size - 1).coerceAtLeast(0) // size = likes + 1
        val diameterDp = (baseDp + likeCount * growthPerLikeDp).coerceIn(baseDp, maxDp)
        val diameterPx = (diameterDp * density).toInt().coerceAtLeast(1)

        return drawCircleBitmap(diameterPx, "#E53935", null)
    }

    /**
     * Dessine le marker d'un cluster : un disque d'une autre couleur que les markers
     * d'événements, avec le nombre d'événements regroupés écrit au centre.
     */
    private fun createClusterBitmap(count: Int): Bitmap {
        val density = mapView.context.resources.displayMetrics.density
        val diameterPx = (50f * density).toInt().coerceAtLeast(1)
        return drawCircleBitmap(diameterPx, "#1E88E5", count.toString())
    }

    private fun drawCircleBitmap(diameterPx: Int, colorHex: String, centerText: String?): Bitmap {
        val density = mapView.context.resources.displayMetrics.density
        val bitmap = Bitmap.createBitmap(diameterPx, diameterPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val radius = diameterPx / 2f
        val strokeWidth = 3f * density

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(colorHex)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(radius, radius, radius - strokeWidth, fillPaint)

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
        }
        canvas.drawCircle(radius, radius, radius - strokeWidth, strokePaint)

        if (centerText != null) {
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = diameterPx * 0.42f
                textAlign = Paint.Align.CENTER
                isFakeBoldText = true
            }
            val textY = radius - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(centerText, radius, textY, textPaint)
        }

        return bitmap
    }

    override fun removeAllMarkers() {
        // On retire uniquement les markers d'événements/clusters qu'on a nous-mêmes ajoutés,
        // pas l'overlay d'écoute des clics ni le marker de sélection, sinon ils
        // disparaissent dès le premier rafraîchissement de la liste d'events.
        eventMarkers.forEach { mapView.overlays.remove(it) }
        eventMarkers.clear()
        mapView.invalidate()
    }

    override fun setOnMapClickListener(listener: (Double, Double) -> Unit) {
        val overlay = object : Overlay() {
            override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView): Boolean {
                val point = mapView.projection.fromPixels(e.x.toInt(), e.y.toInt())
                listener(point.latitude, point.longitude)
                return true
            }
        }
        mapView.overlays.add(overlay)
    }

    override fun setEventMarkers(items: List<MapMarkerItem>, onMarkerClick: (String) -> Unit) {
        lastItems = items
        lastOnMarkerClick = onMarkerClick
        registerZoomListenerOnce()
        redrawEventMarkers()
    }

    /**
     * On ne recalcule le clustering que sur changement de zoom (pas sur chaque
     * scroll) puisque le rayon de regroupement dépend uniquement du niveau de zoom,
     * pas de la position visible de la carte.
     */
    private fun registerZoomListenerOnce() {
        if (zoomListenerRegistered) return
        zoomListenerRegistered = true
        mapView.addMapListener(object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean = false
            override fun onZoom(event: ZoomEvent?): Boolean {
                redrawEventMarkers()
                return false
            }
        })
    }

    private fun redrawEventMarkers() {
        eventMarkers.forEach { mapView.overlays.remove(it) }
        eventMarkers.clear()

        val groups = buildClusters(lastItems, mapView.zoomLevelDouble)
        groups.forEach { group ->
            val marker = if (group.items.size == 1) {
                createEventMarker(group.items[0])
            } else {
                createClusterMarker(group)
            }
            mapView.overlays.add(marker)
            eventMarkers.add(marker)
        }
        mapView.invalidate()
    }

    private fun createEventMarker(item: MapMarkerItem): Marker {
        val marker = Marker(mapView)
        marker.position = GeoPoint(item.lat, item.lng)
        marker.title = item.title
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.icon = BitmapDrawable(mapView.context.resources, createSizedMarkerBitmap(item.size))
        marker.setOnMarkerClickListener { _, _ ->
            lastOnMarkerClick?.invoke(item.id)
            true
        }
        return marker
    }

    private fun createClusterMarker(group: ClusterGroup): Marker {
        val marker = Marker(mapView)
        marker.position = GeoPoint(group.centerLat, group.centerLng)
        marker.title = "${group.items.size} événements"
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        marker.icon = BitmapDrawable(mapView.context.resources, createClusterBitmap(group.items.size))
        marker.setOnMarkerClickListener { _, _ ->
            // Au tap sur un cluster, on zoome dessus pour faire apparaître
            // les markers individuels plutôt que d'ouvrir un détail d'event.
            val newZoom = (mapView.zoomLevelDouble + 2.0).coerceAtMost(19.0)
            centerOn(group.centerLat, group.centerLng, newZoom)
            redrawEventMarkers()
            true
        }
        return marker
    }

    private data class ClusterGroup(
        val centerLat: Double,
        val centerLng: Double,
        val items: List<MapMarkerItem>
    )

    /**
     * Algorithme de clustering "glouton" basé sur la distance, comme le fait
     * la RadiusMarkerClusterer d'osmdroid (osmbonuspack) : on prend un point,
     * on rassemble tous ceux à moins de "rayon" de lui dans le même cluster,
     * on retire le tout de la liste, et on recommence jusqu'à épuisement.
     */
    private fun buildClusters(items: List<MapMarkerItem>, zoom: Double): List<ClusterGroup> {
        if (items.isEmpty()) return emptyList()

        val avgLat = items.map { it.lat }.average()
        val radiusKm = (metersPerPixel(zoom, avgLat) * clusterRadiusPx) / 1000.0

        val remaining = items.toMutableList()
        val groups = mutableListOf<ClusterGroup>()

        while (remaining.isNotEmpty()) {
            val seed = remaining.removeAt(0)
            val group = mutableListOf(seed)

            val iterator = remaining.iterator()
            while (iterator.hasNext()) {
                val candidate = iterator.next()
                val distance = locationUseCase.distanceKm(seed.lat, seed.lng, candidate.lat, candidate.lng)
                if (distance <= radiusKm) {
                    group.add(candidate)
                    iterator.remove()
                }
            }

            groups.add(
                ClusterGroup(
                    centerLat = group.map { it.lat }.average(),
                    centerLng = group.map { it.lng }.average(),
                    items = group
                )
            )
        }
        return groups
    }

    /**
     * Distance réelle (en mètres) représentée par un pixel écran, à un niveau de
     * zoom et une latitude donnés (formule standard de la projection Web Mercator).
     * Plus on dézoome, plus un pixel représente une grande distance, donc plus
     * le rayon de regroupement en km augmente automatiquement.
     */
    private fun metersPerPixel(zoom: Double, latDeg: Double): Double {
        val earthCircumferenceM = 40075016.686
        return earthCircumferenceM * cos(Math.toRadians(latDeg)) / (256.0 * 2.0.pow(zoom))
    }

    /**
     * Affiche (ou déplace) un marker temporaire à l'endroit tapé par l'utilisateur,
     * pour qu'il garde une trace visuelle du dernier point choisi sur la carte.
     */
    fun showSelectionMarker(lat: Double, lng: Double) {
        selectionMarker?.let { mapView.overlays.remove(it) }
        val marker = Marker(mapView)
        marker.position = GeoPoint(lat, lng)
        marker.title = "Position sélectionnée"
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        mapView.overlays.add(marker)
        selectionMarker = marker
        mapView.invalidate()
    }

    fun onResume() = mapView.onResume()
    fun onPause() = mapView.onPause()
}