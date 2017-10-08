package ca.ubc.cs.cpsc210.mindthegap.ui;

/*
 * Copyright 2015-2016 Department of Computer Science UBC
 */

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import ca.ubc.cs.cpsc210.mindthegap.R;
import ca.ubc.cs.cpsc210.mindthegap.model.Branch;
import ca.ubc.cs.cpsc210.mindthegap.model.Line;
import ca.ubc.cs.cpsc210.mindthegap.model.Station;
import ca.ubc.cs.cpsc210.mindthegap.model.StationManager;
import ca.ubc.cs.cpsc210.mindthegap.util.LatLon;
import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer;
import org.osmdroid.bonuspack.overlays.MapEventsOverlay;
import org.osmdroid.bonuspack.overlays.MapEventsReceiver;
import org.osmdroid.bonuspack.overlays.Marker;
import org.osmdroid.bonuspack.overlays.Polyline;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.OverlayManager;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.lang.reflect.Array;
import java.util.*;

// Manages overlays used to render TfL data
public class TfLOverlayManager implements MapEventsReceiver {
    /** the activity */
    private MapDisplayFragment mapFragment;
    /** map view on which overlays are to be placed */
    private MapView mapView;
    /** overlay used to show station markers */
    private RadiusMarkerClusterer stnClusterer;
    /** window displayed when user selects a station */
    private StationInfoWindow stnInfoWindow;
    /** overlay that listens for user initiated events on map */
    private MapEventsOverlay eventsOverlay;
    /** overlay used to display text on a layer above the map */
    private TextOverlay textOverlay;
    /** overlays used to plot tube lines */
    private List<Polyline> tubeLineOverlays;
    /** overlay used to display location of user */
    private MyLocationNewOverlay locOverlay;
    /** TEST FIELD */
    private List<Polyline> copiedPolyLines;

    /**
     * Constructor
     *
     * @param mapFragment  the fragment that displays the map view
     * @param mapView  the map view
     * @param locnProvider  user location provider
     */
    public TfLOverlayManager(MapDisplayFragment mapFragment, MapView mapView, GpsMyLocationProvider locnProvider) {
        this.mapFragment = mapFragment;
        this.mapView = mapView;
        tubeLineOverlays = new ArrayList<>();
        locOverlay = new MyLocationNewOverlay(mapFragment.getActivity(), locnProvider, mapView);
        eventsOverlay = new MapEventsOverlay(mapFragment.getActivity(), this);
        stnClusterer = new RadiusMarkerClusterer(mapFragment.getActivity());
        stnInfoWindow = new StationInfoWindow((StationSelectionListener) mapFragment.getActivity(), mapView);
        Drawable clusterIconD = mapFragment.getResources().getDrawable(R.drawable.stn_cluster);
        Bitmap clusterIcon = ((BitmapDrawable) clusterIconD).getBitmap();
        stnClusterer.setIcon(clusterIcon);
        createTextOverlay();
        markStations();
        updateOverlays();
        copiedPolyLines = new ArrayList<>();
    }

    /**
     * Clear overlays and add route, station, location and events overlays
     */
    public void updateOverlays() {
        OverlayManager om = mapView.getOverlayManager();
        om.clear();
        om.addAll(tubeLineOverlays);
        om.add(stnClusterer);
        om.add(locOverlay);
        om.add(textOverlay);
        om.add(eventsOverlay);

        mapView.invalidate();
    }

    /**
     * Update marker of nearest station (called when user's location has changed).  If nearest is null,
     * no station is marked as the nearest station.
     *
     * @param nearest   station nearest to user's location (null if no station within StationManager.RADIUS metres
     */
    public void updateMarkerOfNearest(Station nearest) {
        Drawable stnIconDrawable = mapFragment.getResources().getDrawable(R.drawable.stn_icon);
        Drawable closestStnIconDrawable = mapFragment.getResources().getDrawable(R.drawable.closest_stn_icon);

        if(nearest != null) {
            setMarkerIcons(closestStnIconDrawable, stnIconDrawable, nearest);
        }
        else {
            setOtherIcons(stnIconDrawable);
            }
        }


    /** Helper Function for updateMarkerOfNearest **/
    public void setMarkerIcons(Drawable closestIcon, Drawable stnIcon, Station nearest) {
        GeoPoint location = new GeoPoint(nearest.getLocn().getLatitude(), nearest.getLocn().getLongitude());
        for (Marker m : stnClusterer.getItems()) {
            if(m.getPosition().equals(location)) {
                m.setIcon(closestIcon);
            }
            else {
                m.setIcon(stnIcon);
            }
        }
    }

    /** Helper Function for updateMarkerOfNearest **/
    public void setOtherIcons(Drawable stnIcon) {
        for (Marker m : stnClusterer.getItems()) {
            m.setIcon(stnIcon);
        }
    }

    /**
     * Replot tube lines onto map
     */
    public void replotTubeLines() {
        tubeLineOverlays.clear();
        plotLines();
        updateOverlays();
    }

    /**
     * Resume user location plotting
     */
    public void resumeLocnPlotting() {
       locOverlay.enableMyLocation();
    }

    /**
     * Disable user location plotting
     */
    public void disableLocnPlotting() {
       locOverlay.disableMyLocation();
    }

    /**
     * Handle long-press on station marker
     * Plot lines running through long-pressed station onto map
     * @param longPressedStn  the station corresponding to the marker that was long-pressed
     */
    public void onStationMarkerLongPress(Station longPressedStn) {
        for (Line a : longPressedStn.getLines()) {
            List<GeoPoint> geopoints = new ArrayList<>();
            Polyline polyline = new Polyline(mapView.getResourceProxy());
            polyline.setColor(a.getColour());
            Set<Branch> branches = a.getBranches();

            addBranches(branches, geopoints);
            polylineSetAttributes(geopoints, polyline);
        }
    }

    /** Helper function for onStationMarkerLongPress **/
    public void addBranches(Set<Branch> branches, List<GeoPoint> geopoints) {
        for(Branch b : branches) {
            for(LatLon locn : b.getPoints()) {
                GeoPoint geopoint = new GeoPoint(locn.getLatitude(), locn.getLongitude());
                geopoints.add(geopoint);
            }
        }
    }

    /** Helper function for onStationMarkerLongPress **/
    public void polylineSetAttributes(List<GeoPoint> geopoints, Polyline polyline) {
        polyline.setPoints(geopoints);
        polyline.setWidth(getLineWidth(mapView.getZoomLevel()));
        tubeLineOverlays.add(polyline);
        copiedPolyLines.add(polyline);
        plotLines();
    }

    /**
     * Close info windows when user taps map.
     * @param geoPoint  point at which long-press occurred
     */
    @Override
    public boolean singleTapConfirmedHelper(GeoPoint geoPoint) {
        StationInfoWindow.closeAllInfoWindowsOn(mapView);
        return false;
    }

    /**
     * Clear tube lines plotted onto map when user long-presses map
     * @param geoPoint  point at which long-press occurred
     */
    @Override
    public boolean longPressHelper(GeoPoint geoPoint) {
        tubeLineOverlays.clear();
        copiedPolyLines.clear();
        updateOverlays();
        return false;
    }

    /**
     * Create text overlay to display credit to TfL
     */
    private void createTextOverlay() {
        ResourceProxy rp = new DefaultResourceProxyImpl(mapFragment.getActivity());
        textOverlay = new TextOverlay(rp, mapFragment.getResources().getString(R.string.tfl_open_data));
    }

    /**
     * Mark all stations in station manager onto map.
     */
    private void markStations() {
        Drawable stnIconDrawable = mapFragment.getResources().getDrawable(R.drawable.stn_icon);
        for (Station next : StationManager.getInstance()) {
            StationMarker stnMarker = new StationMarker(mapView, this);
            GeoPoint location = new GeoPoint(next.getLocn().getLatitude(), next.getLocn().getLongitude());
            stnMarker.setIcon(stnIconDrawable);
            stnMarker.setTitle(next.getName());
            stnMarker.setPosition(location);
            stnMarker.setInfoWindow(stnInfoWindow);
            stnMarker.setRelatedObject(next);
            stnClusterer.add(stnMarker);
        }
}

    /**
     * Plot selected lines on map
     */
    private void plotLines() {
        if(copiedPolyLines != null) {
            tubeLineOverlays.addAll(copiedPolyLines);
        }
        updateOverlays();
    }

    /**
     * Get width of line used to plot tube line based on zoom level
     * @param zoomLevel   the zoom level of the map
     * @return            width of line used to plot tube line
     */
    private float getLineWidth(int zoomLevel) {
        if(zoomLevel > 14)
            return 15.0f;
        else if(zoomLevel > 10)
            return 7.5f;
        else
            return 3.0f;
    }
}
