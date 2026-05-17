package com.vikasyadavnsit.cdc.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.utils.FirebaseUtils;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LiveLocationFragment extends Fragment {

    private static final double DEFAULT_LAT = 20.5937;
    private static final double DEFAULT_LNG = 78.9629;
    private static final double DEFAULT_ZOOM = 5.0;
    private static final double LOCATION_ZOOM = 16.0;

    private MapView mapView;
    private Marker locationMarker;
    private TextView coordsText, accuracyText, timestampText, liveBadge;
    private View emptyState, infoCard;

    private DatabaseReference locationRef;
    private ValueEventListener locationListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
        Configuration.getInstance().setOsmdroidBasePath(requireContext().getCacheDir());
        Configuration.getInstance().setOsmdroidTileCache(new File(requireContext().getCacheDir(), "osmdroid"));

        View view = inflater.inflate(R.layout.fragment_live_location, container, false);
        mapView       = view.findViewById(R.id.live_location_map);
        coordsText    = view.findViewById(R.id.live_location_coords);
        accuracyText  = view.findViewById(R.id.live_location_accuracy);
        timestampText = view.findViewById(R.id.live_location_timestamp);
        liveBadge     = view.findViewById(R.id.live_location_live_badge);
        emptyState    = view.findViewById(R.id.live_location_empty_state);
        infoCard      = view.findViewById(R.id.live_location_info_card);

        view.findViewById(R.id.live_location_back).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        setupMap();
        attachFirebaseListener();
        return view;
    }

    private void setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getZoomController().setVisibility(
                org.osmdroid.views.CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT);
        mapView.getController().setZoom(DEFAULT_ZOOM);
        mapView.getController().setCenter(new GeoPoint(DEFAULT_LAT, DEFAULT_LNG));
    }

    private void attachFirebaseListener() {
        locationRef = FirebaseUtils.getLiveLocationRef();
        locationListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                if (!snapshot.exists()) { showEmptyState(true); return; }
                Double lat = snapshot.child("lat").getValue(Double.class);
                Double lng = snapshot.child("lng").getValue(Double.class);
                if (lat == null || lng == null) { showEmptyState(true); return; }
                Object rawAcc = snapshot.child("accuracy").getValue(Object.class);
                Long timestamp = snapshot.child("timestamp").getValue(Long.class);
                float accuracy = 0f;
                if (rawAcc instanceof Double) accuracy = ((Double) rawAcc).floatValue();
                else if (rawAcc instanceof Long) accuracy = ((Long) rawAcc).floatValue();
                final float finalAccuracy = accuracy;
                requireActivity().runOnUiThread(() ->
                        updateMap(lat, lng, finalAccuracy, timestamp != null ? timestamp : 0));
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        locationRef.addValueEventListener(locationListener);
    }

    private void updateMap(double lat, double lng, float accuracy, long timestamp) {
        showEmptyState(false);
        GeoPoint point = new GeoPoint(lat, lng);
        if (locationMarker == null) {
            locationMarker = new Marker(mapView);
            locationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            mapView.getOverlays().clear();
            mapView.getOverlays().add(locationMarker);
        }
        locationMarker.setPosition(point);
        locationMarker.setTitle(String.format(Locale.getDefault(), "%.5f, %.5f", lat, lng));
        mapView.getController().animateTo(point);
        mapView.getController().setZoom(LOCATION_ZOOM);
        mapView.invalidate();

        coordsText.setText(String.format(Locale.getDefault(), "%.5f,  %.5f", lat, lng));
        accuracyText.setText(String.format(Locale.getDefault(), "±%.0fm", accuracy));
        if (timestamp > 0) {
            timestampText.setText("Updated: " +
                    new SimpleDateFormat("dd MMM, HH:mm:ss", Locale.getDefault()).format(new Date(timestamp)));
        }
    }

    private void showEmptyState(boolean show) {
        emptyState.setVisibility(show ? View.VISIBLE : View.GONE);
        infoCard.setVisibility(show ? View.GONE : View.VISIBLE);
        liveBadge.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    @Override public void onResume() { super.onResume(); mapView.onResume(); }
    @Override public void onPause()  { super.onPause();  mapView.onPause(); }

    @Override
    public void onDestroyView() {
        if (locationRef != null && locationListener != null)
            locationRef.removeEventListener(locationListener);
        mapView.onDetach();
        super.onDestroyView();
    }
}
