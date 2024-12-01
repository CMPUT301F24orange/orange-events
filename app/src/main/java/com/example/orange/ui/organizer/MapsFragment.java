package com.example.orange.ui.organizer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.orange.R;
import com.example.orange.data.model.Event;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.Map;

public class MapsFragment extends Fragment implements OnMapReadyCallback {
    private GoogleMap googleMap;
    private Event event;  // Event that contains user locations

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve the event object passed from the previous fragment
        if (getArguments() != null) {
            event = getArguments().getParcelable("event");  // Assuming Event is Parcelable
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_maps, container, false);

        // Initialize the map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        return view;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;

        // Add markers for all users in the event's location map
        if (event != null && event.getLocation() != null) {
            addMarkersForUsers(event.getLocation());
        }
    }

    /**
     * Add markers for all users on the map based on the event's location data.
     */
    private void addMarkersForUsers(Map<String, Map<String, Object>> locationMap) {
        if (googleMap != null && locationMap != null) {
            for (Map.Entry<String, Map<String, Object>> entry : locationMap.entrySet()) {
                String userId = entry.getKey();
                Map<String, Object> userLocation = entry.getValue();

                if (userLocation != null && userLocation.containsKey("latitude") && userLocation.containsKey("longitude")) {
                    Double latitude = (Double) userLocation.get("latitude");
                    Double longitude = (Double) userLocation.get("longitude");

                    LatLng userLatLng = new LatLng(latitude, longitude);
                    googleMap.addMarker(new MarkerOptions()
                            .position(userLatLng)
                            .title("User: " + userId));  // You can modify to use user names if available
                }
            }

            // move camera to the first marker (or the center of the map)
            if (!locationMap.isEmpty()) {
                Map.Entry<String, Map<String, Object>> firstEntry = locationMap.entrySet().iterator().next();
                Map<String, Object> firstUserLocation = firstEntry.getValue();
                Double latitude = (Double) firstUserLocation.get("latitude");
                Double longitude = (Double) firstUserLocation.get("longitude");
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 10f));
            }
        }
    }
}
