package at.com.kmldemo;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.mapbox.mapboxsdk.MapboxAccountManager;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.bonuspack.kml.KmlDocument;
import org.osmdroid.views.overlay.FolderOverlay;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;



public class MainActivity extends AppCompatActivity {
    private MapView mapView;
    private MapboxMap map;
    private KmlDocument kmlDocument;
    private FolderOverlay folderOverlay;
    private DrawGeoJson drawGeoJson;
    private boolean fCreated;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MapboxAccountManager.start(getApplicationContext(), getString(R.string.access_token));

        mapView = (MapView) findViewById(R.id.mapview);
        mapView.onCreate(savedInstanceState);
        mapView.setStyleUrl(getString(R.string.map_url));


        //here we are going to convert and save KML file from row folder to Sdcard
        InputStream ins = getResources().openRawResource(
                getResources().getIdentifier("pmc_election_ward_boundaries", "raw", getPackageName()));


        kmlDocument = new KmlDocument();
        kmlDocument.parseKMLStream(ins, null);
        fCreated = kmlDocument.saveAsGeoJSON(new File(Environment.getExternalStorageDirectory() + "/sample.geojson"));

        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                map = mapboxMap;
                map.setMyLocationEnabled(true);

                if (fCreated) {
                    if (drawGeoJson != null) drawGeoJson.cancel(true);
                    drawGeoJson = new DrawGeoJson();
                    drawGeoJson.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            }
        });
    }

    private class DrawGeoJson extends AsyncTask<Void, Void, HashMap<Integer, List<LatLng>>> {
        private HashMap<Integer, List<LatLng>> integerListHashMap;
        private String sLattitude,sLongitude;
        @Override
        protected HashMap<Integer, List<LatLng>> doInBackground(Void... voids) {
            integerListHashMap = new HashMap<>();

            try {
                // Load GeoJSON file
                InputStream inputStream = new FileInputStream(new File(Environment.getExternalStorageDirectory() + "/sample.geojson"));
                BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("UTF-8")));
                StringBuilder sb = new StringBuilder();
                int cp;
                while ((cp = rd.read()) != -1) {
                    sb.append((char) cp);
                }

                inputStream.close();

                // Parse JSON
                JSONObject json = new JSONObject(sb.toString());
                JSONArray features = json.getJSONArray("features");
                for (int featurePos = 0; featurePos < features.length(); featurePos++) {
                    ArrayList<LatLng> points = new ArrayList<>();
                    JSONObject feature = features.getJSONObject(featurePos);
                    JSONObject geometry = feature.getJSONObject("geometry");
                    Log.e("Tag", "geometry found " + geometry);
                    if (geometry != null) {
                        String type = geometry.getString("type");//this is type of draw area
                        Log.e("Tag", "Type found " + type);
                        // Our GeoJSON only has one feature: a line string
                        if (!TextUtils.isEmpty(type) && type.equalsIgnoreCase("GeometryCollection")) {
                            JSONArray geometrieJsonArray = geometry.getJSONArray("geometries");
                            // Get the Coordinates
                            JSONObject coordinate = geometrieJsonArray.getJSONObject(0);
                            JSONArray polygoneArray = coordinate.getJSONArray("coordinates");
                            JSONArray jsonObjectMainCord = polygoneArray.getJSONArray(0);
                            for (int lc = 0; lc < jsonObjectMainCord.length(); lc++) {
                                JSONArray coord = jsonObjectMainCord.getJSONArray(lc);
                                LatLng latLng = new LatLng(coord.getDouble(1), coord.getDouble(0));
                                sLattitude = String.valueOf(latLng.getLatitude());
                                sLongitude = String.valueOf(latLng.getLongitude());
                                points.add(latLng);
                            }
                        }
                    }
                    integerListHashMap.put(featurePos, points);
                }
            } catch (Exception exception) {
                Log.e("", "Exception Loading GeoJSON: " + exception.toString());
            }

            return integerListHashMap;
        }

        @Override
        protected void onPostExecute(HashMap<Integer, List<LatLng>> integerListHashMap) {
            super.onPostExecute(integerListHashMap);

            Log.e("Tag", "point found " + integerListHashMap.size());
            if (integerListHashMap.size() > 0) {
            //I HAVE MAINTAIN THE HASHMAP FOR STORING THE MULTIPLE POLYGON VALUE
                for (int i = 0; i < integerListHashMap.size(); i++) {
                    List<LatLng> points = integerListHashMap.get(i);
                    map.addPolyline(new PolylineOptions()
                            .addAll(points)
                            .color(Color.parseColor("#e60707"))
                            .width(2));
                }
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(new LatLng(Double.parseDouble(sLattitude), Double.parseDouble(sLongitude)))
                        .zoom(12)
                        .build();

                map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 2000);
                Toast.makeText(MainActivity.this, "Area draw", Toast.LENGTH_SHORT).show();
                // Draw polyline on map

            }
        }
    }

}
