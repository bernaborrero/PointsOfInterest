package com.bernabeborrero.pointsofinterest.controller;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import com.bernabeborrero.pointsofinterest.R;
import com.bernabeborrero.pointsofinterest.model.PointOfInterest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements View.OnClickListener {

    private static final String API_URL = "http://infobosccoma.net/pmdm/pois.php";

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private EditText txtSearchLocation;
    private Button btnSearchLocation;
    private ArrayList<PointOfInterest> pointsOfInterest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setupGUI();
    }

    private void setupGUI() {
        txtSearchLocation = (EditText) findViewById(R.id.txtSearchLocation);
        btnSearchLocation = (Button) findViewById(R.id.btnSearchLocation);
        btnSearchLocation.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMap.setMyLocationEnabled(false);
    }

    /**
     * Set up the map
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    private void setUpMap() {
        mMap.setMyLocationEnabled(true);
        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        setUpMarkers();
    }

    private void setUpMarkers() {
        mMap.clear();

        if(pointsOfInterest != null) {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();

            for(PointOfInterest point : pointsOfInterest) {
                LatLng pos = new LatLng((double) point.getLatitude(), (double) point.getLongitude());

                mMap.addMarker(new MarkerOptions().position(pos)
                        .title(point.getName()));

                builder.include(pos);
            }

            LatLngBounds bounds = builder.build();
            mMap.animateCamera(
                    CameraUpdateFactory.newLatLngBounds(bounds, 150), 2000, null
            );
        }
    }

    @Override
    public void onClick(View v) {
        InputMethodManager imm = (InputMethodManager)getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(txtSearchLocation.getWindowToken(), 0);

        new GetPointsFromPlace().execute(txtSearchLocation.getText().toString());
    }

    private class GetPointsFromPlace extends AsyncTask<String, Void, ArrayList<PointOfInterest>> {

        @Override
        protected ArrayList<PointOfInterest> doInBackground(String... params) {
            ArrayList<PointOfInterest> points = null;

            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(API_URL);
            HttpResponse httpResponse = null;
            try {
                if(params[0].length() > 0) {
                    List<NameValuePair> parameters = new ArrayList<>();
                    parameters.add(new BasicNameValuePair("city", params[0]));
                    httpPost.setEntity(new UrlEncodedFormEntity(parameters));
                }

                httpResponse = httpClient.execute(httpPost);
                String response = EntityUtils.toString(httpResponse.getEntity());
                points = dataToJSON(response);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if(points == null || points.size() == 0) {
                return null;
            } else {
                return points;
            }
        }

        @Override
        protected void onPostExecute(ArrayList<PointOfInterest> points) {
            pointsOfInterest = points;
            setUpMarkers();
        }

        private ArrayList<PointOfInterest> dataToJSON(String json) {
            Gson converter = new Gson();
            return converter.fromJson(json, new TypeToken<ArrayList<PointOfInterest>>(){}.getType());
        }
    }
}
