package com.example.localisation2;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.BreakIterator;

public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 0;
    public static String OPENWEATHER_WEATHER_QUERY = "https://api.openweathermap.org/data/2.5/weather?lat=%s&lon=%s&mode=html&appid=4526d487f12ef78b82b7a7d113faea64";
    private TextView latTextView;
    private TextView lonTextView;
    private TextView cityTextView;
    private WebView weatherWebView;
    LocationManager locationManager;
    private LocationProvider locationProvider;



    private static class MyHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;


        MyHandler(MainActivity activity) {
            mActivity = new WeakReference<MainActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = mActivity.get();
            String latitude = msg.getData().getString("lat");
            String longitude = msg.getData().getString("lon");
            String web = msg.getData().getString("web");
            String city = msg.getData().getString("city");
            activity.latTextView.setText(latitude);
            activity.lonTextView.setText(longitude);
            activity.cityTextView.setText(city);
            activity.weatherWebView.loadDataWithBaseURL(null, web, "text/html", "utf-8", null);
        }
    }

    Handler myHandler = new MyHandler(this);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        latTextView = findViewById(R.id.latTextView);
        lonTextView = findViewById(R.id.lonTextView);
        cityTextView = findViewById(R.id.cityTextView);
        weatherWebView = findViewById(R.id.weatherWebView);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    accessLocation();
                }
            }
        }
    }

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            final double latitude = location.getLatitude();
            final double longitude = location.getLongitude();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    updateWeather(latitude, longitude);
                }
            }).start();
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }

    };

    @Override
    protected void onStart() {
        super.onStart();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);
        } else {
            accessLocation();
        }
    }
    @Override
    protected void onStop() {
        super.onStop();
        if (locationProvider != null) {
            Toast.makeText(this, "Location listener unregistered!", Toast.LENGTH_SHORT).show();
            try {
                this.locationManager.removeUpdates(this.locationListener);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, "LocationProvider is not avilable at the moment!", Toast.LENGTH_SHORT).show();
        }
    }


    private void accessLocation() {
        this.locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        this.locationProvider = this.locationManager.getProvider(LocationManager.GPS_PROVIDER);
        if (locationProvider != null) {
            System.out.println("Location listener registered!");
            Toast.makeText(this, "Location listener registered!", Toast.LENGTH_SHORT).show();
            try {
                this.locationManager.requestLocationUpdates(locationProvider.getName(), 0, 1, this.locationListener);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, "LocationProvider is not avaible eat the moment!", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateWeather(double latitude, double longitude) {
        String weather = getContentFromUrl(String.format(OPENWEATHER_WEATHER_QUERY, latitude,longitude) );
        Document doc = Jsoup.parse(weather);
        Element link = doc.select("div").first();
        String city = link.text();
        Message message = myHandler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putString("lat", String.valueOf(latitude));
        bundle.putString("lon", String.valueOf(longitude));
        bundle.putString("web", weather);
        bundle.putString("city", city);
        message.setData(bundle);
        myHandler.sendMessage(message);
    }
    
    public String getContentFromUrl(String address) {
        String content = null;

        Log.v("[GEO WEATHER ACTIVITY]", address);
        HttpURLConnection urlConnection = null;
        URL url = null;
        try {
            url = new URL(address);
            urlConnection = (HttpURLConnection) url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));

            StringBuilder stringBuilder = new StringBuilder();
            String line = null;
            while ((line = in.readLine()) != null)
            {
                stringBuilder.append(line + "\n");
            }

            content = stringBuilder.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(urlConnection!= null) urlConnection.disconnect();
        }
        System.out.println(content);
        return content;
    }
}

