package com.example.geoevent.domain

import android.content.Context
import android.view.ViewGroup
/**
 * Interface qui définit toutes les opérations possibles sur la carte.
 * Grâce à cette interface, l'application ne dépend pas d'une librairie de carte spécifique.
 * Actuellement on utilise OpenStreetMap (osmdroid), mais on pourrait remplacer
 * par Google Maps en créant une classe "GoogleMapProvider" qui implémente cette interface,
 * sans toucher à une seule ligne de MapActivity ou du domain.
 *
 * C'est le principe du "découplage" : la carte peut changer, le reste ne bouge pas.
 */
interface MapProvider {

    // Initialise la configuration de la carte
    // Doit être appelé avant showMap()
    fun initialize(context: Context)
    // Crée et affiche la vue de la carte dans le conteneur fourni (FrameLayout)
    // Le conteneur est défini dans activity_main.xml
    fun showMap(container: ViewGroup)
    fun centerOn(lat: Double, lng: Double, zoom: Double)
    //ajoute un marker pour un event donné
    fun addMarker(lat: Double, lng: Double, title: String, size: Int = 1): Any
    // supprime un marker d'un event
    fun removeAllMarkers()
    //utilisé pour creer l'event a l'endroit precis et definit ce qui se passe quand l'utilisateur tape sur la carte
    fun setOnMapClickListener(listener: (Double, Double) -> Unit)
}