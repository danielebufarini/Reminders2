package com.danielebufarini.reminders2.ui;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v13.app.ActivityCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.SeekBar;

import com.danielebufarini.reminders2.R;
import com.danielebufarini.reminders2.model.GTask;
import com.danielebufarini.reminders2.services.GeofenceBroadcastReceiver;
import com.danielebufarini.reminders2.util.ApplicationCache;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import static com.danielebufarini.reminders2.ui.Reminders.LOGV;

public class LocationBasedReminderFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = "LocationReminderFragm";

    private AutoCompleteTextView autoCompleteTextView;
    private GoogleApiClient googleApiClient;
    private PlaceAutocompleteAdapter adapter;
    private GoogleMap map;
    private Circle circle;
    private GeofencingClient geofencingClient;
    private PendingIntent geofencePendingIntent;
    private OnReminderPlaceChangedListener listener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.tab_fragment_location_based_reminder, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);
        googleApiClient = new GoogleApiClient.Builder(getActivity()).addApi(Places.GEO_DATA_API).build();
        autoCompleteTextView = getActivity().findViewById(R.id.mapAddress);
        autoCompleteTextView.setOnItemClickListener(autocompleteClickListener);
        adapter = new PlaceAutocompleteAdapter(getActivity(), googleApiClient, null, null);
        autoCompleteTextView.setAdapter(adapter);

        // Set up the 'clear text' button that clears the text in the autocomplete view
        ImageButton clearButton = getActivity().findViewById(R.id.button_clear);
        clearButton.setOnClickListener(v -> autoCompleteTextView.setText(""));

        SeekBar seekBar = getActivity().findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);

        MapFragment mapFragment = (MapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        geofencingClient = LocationServices.getGeofencingClient(getActivity());
    }

    @Override
    public void onStart() {

        super.onStart();
        googleApiClient.connect();
    }

    @Override
    public void onStop() {

        super.onStop();
        googleApiClient.disconnect();
    }

    @Override
    public void onAttach(Context context) {

        super.onAttach(context);
        attachListener((Activity) context);
    }

    @Override
    public void onAttach(Activity activity) {

        super.onAttach(activity);
        attachListener(activity);
    }

    @Override
    public void onDetach() {

        super.onDetach();
        listener = null;
    }

    private void attachListener(Activity activity) {

        try {
            listener = (OnReminderPlaceChangedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement " + OnReminderPlaceChangedListener.class.getSimpleName());
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {

        map = googleMap;
        double latitude, longitude;
        String title;
        GTask task = ApplicationCache.INSTANCE.getTask();
        if (task == null || (Double.compare(task.longitude, 0d) == 0 || Double.compare(task.latitude, 0d) == 0)) {
            latitude = -34;
            longitude = -151;
            title = "Marker in Sydney";
        } else {
            latitude = task.latitude;
            longitude = task.longitude;
            title = task.title;
        }
        LatLng place = new LatLng(latitude, longitude);
        map.addMarker(new MarkerOptions().position(place).title(title));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(place, 10));
        map.animateCamera(CameraUpdateFactory.zoomTo(18), 2000, null);
    }

    private SeekBar.OnSeekBarChangeListener onSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            if (circle != null && progress > 0) {
                int step = 200 / seekBar.getMax();
                circle.setRadius(progress * step);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };

    private AdapterView.OnItemClickListener autocompleteClickListener
            = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            /*
             Retrieve the place ID of the selected item from the Adapter.
             The adapter stores each Place suggestion in a AutocompletePrediction from which we
             read the place ID and title.
              */
            final AutocompletePrediction item = adapter.getItem(position);
            final String placeId = item.getPlaceId();
            final CharSequence primaryText = item.getPrimaryText(null);

            Log.i(TAG, "Autocomplete item selected: " + primaryText);

            /*
             Issue a request to the Places Geo Data API to retrieve a Place object with additional
             details about the place.
              */
            PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                    .getPlaceById(googleApiClient, placeId);
            placeResult.setResultCallback(updatePlaceDetailsCallback);
            Log.i(TAG, "Called getPlaceById to get Place details for " + placeId);
        }
    };

    /**
     * Callback for results from a Places Geo Data API query that shows the first place result in
     * the details view on screen.
     */
    @SuppressWarnings("MissingPermission")
    private ResultCallback<PlaceBuffer> updatePlaceDetailsCallback
            = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(PlaceBuffer places) {

            if (!places.getStatus().isSuccess()) {
                // Request did not complete successfully
                Log.e(TAG, "Place query did not complete. Error: " + places.getStatus().toString());
                places.release();
                return;
            }

            View view = getActivity().getCurrentFocus();
            if (view != null) {
                InputMethodManager imm =
                        (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }

            Place place = places.get(0);
            if (LOGV) Log.i(TAG, "Place details received: " + place.getName());
            listener.onReminderPlaceChanged(place.getLatLng().latitude, place.getLatLng().longitude, place.getAddress());
            GTask task = ApplicationCache.INSTANCE.getTask();
            task.latitude = place.getLatLng().latitude;
            task.longitude = place.getLatLng().longitude;
            task.locationTitle = place.getAddress().toString();

            map.addMarker(new MarkerOptions().position(place.getLatLng()).title(place.getName().toString()));
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(), 10));
            map.animateCamera(CameraUpdateFactory.zoomTo(18), 2000, null);

            if (circle == null) {
                double radiusInMeters = 100.0;
                int strokeColor = 0xFF0000FF;
                int shadeColor = 0x110000FF;
                CircleOptions circleOptions = new CircleOptions().center(place.getLatLng())
                        .radius(radiusInMeters).fillColor(shadeColor).strokeColor(strokeColor)
                        .strokeWidth(4);
                circle = map.addCircle(circleOptions);
            }
            Geofence geofence = new Geofence.Builder()
                    .setRequestId(String.valueOf(task.id))
                    .setCircularRegion(
                            place.getLatLng().latitude, place.getLatLng().longitude, (float) circle.getRadius())
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                            Geofence.GEOFENCE_TRANSITION_EXIT)
                    .build();
            if (checkPermissions()) {
                geofencingClient.addGeofences(getGeofencingRequest(geofence), getGeofencePendingIntent());
            }
            places.release();
        }
    };

    private GeofencingRequest getGeofencingRequest(Geofence geofence) {

        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
        // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
        // is already inside that geofence.
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_DWELL);
        // Add the geofences to be monitored by geofencing service.
        builder.addGeofence(geofence);
        // Return a GeofencingRequest.
        return builder.build();
    }

    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (geofencePendingIntent != null) {
            return geofencePendingIntent;
        }
        Intent intent = new Intent(getContext(), GeofenceBroadcastReceiver.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        geofencePendingIntent = PendingIntent.getBroadcast(getContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return geofencePendingIntent;
    }

    private boolean checkPermissions() {

        int permissionState = ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    public interface OnReminderPlaceChangedListener {

        void onReminderPlaceChanged(double latitude, double longitude, CharSequence locationTitle);
    }
}
