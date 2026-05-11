package com.hiddenspot.app.activities;

import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.hiddenspot.app.R;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    public static final String EXTRA_ADDRESS    = "map_address";
    public static final String EXTRA_PLACE_NAME = "map_place_name";

    private GoogleMap googleMap;
    private String address;
    private String placeName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        address   = getIntent().getStringExtra(EXTRA_ADDRESS);
        placeName = getIntent().getStringExtra(EXTRA_PLACE_NAME);

        // Set place name in header
        TextView tvTitle = findViewById(R.id.tv_map_title);
        if (tvTitle != null && placeName != null) tvTitle.setText(placeName);

        // Back button
        ImageButton btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // Init map fragment
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        if (address != null && !address.trim().isEmpty()) {
            geocodeAndPin(address);
        } else {
            Toast.makeText(this, "Address not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void geocodeAndPin(String addressText) {
        // Run geocoding on a background thread — Geocoder is blocking
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> results = geocoder.getFromLocationName(addressText, 1);
                if (results != null && !results.isEmpty()) {
                    Address loc = results.get(0);
                    LatLng latLng = new LatLng(loc.getLatitude(), loc.getLongitude());
                    runOnUiThread(() -> pinOnMap(latLng, addressText));
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(this,
                                    "Could not find location on map.\nTry opening in Google Maps.",
                                    Toast.LENGTH_LONG).show());
                }
            } catch (IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Geocoding error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void pinOnMap(LatLng latLng, String snippet) {
        if (googleMap == null) return;
        googleMap.clear();
        googleMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title(placeName != null ? placeName : "Hidden Spot")
                .snippet(snippet)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));
    }
}
