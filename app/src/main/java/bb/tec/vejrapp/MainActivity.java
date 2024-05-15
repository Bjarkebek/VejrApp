package bb.tec.vejrapp;

import static com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.CancellationToken;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import bb.tec.vejrapp.Database.DBContract;
import bb.tec.vejrapp.Database.DBHelper;
import bb.tec.vejrapp.Model.UserLocation;

public class MainActivity extends AppCompatActivity {

    // weather url to get JSON
    public String weather_url = "";

    // api id for url
    public String api_id1 = "57df91a4373044bb8ef0aac7ef46123e";

    public static List<UserLocation> locations;


    SQLiteDatabase db;
    private DBHelper dbHelper;
    Cursor cursor;


    Location location;
    FusedLocationProviderClient flpc;


    TextView tv_city, tv_temperature, tv_weather_drawable;
    EditText et_citySearch;
    Spinner sp_citySelect;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // to ask for permissions
        permissionsList = new ArrayList<>();
        permissionsList.addAll(Arrays.asList(permissionsStr));
        askForPermissions(permissionsList);


        dbHelper = new DBHelper(this);

        initGui();
    }


    private void initGui() {
        tv_city = findViewById(R.id.tv_city);
        tv_temperature = findViewById(R.id.tv_temperature);
        tv_weather_drawable = findViewById(R.id.tv_weather_drawable);
        et_citySearch = findViewById(R.id.et_citySearch);
        sp_citySelect = findViewById(R.id.sp_citySelect);

        et_citySearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                fillSpinner();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchSpinner(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });


        findViewById(R.id.btn_register).setOnClickListener(v -> {
            if (location != null) {
                saveUserLocation(new UserLocation(
                        location.getLatitude(),
                        location.getLongitude()
                ));
            }
        });

        sp_citySelect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String city = parent.getItemAtPosition(pos).toString();
                if (city == "Select city") {
                    getWeatherFromCurrentLocation();
                }
                getWeatherFromSearchLocation(city);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                getWeatherFromCurrentLocation();
            }
        });


        fillSpinner();
    }

    private void searchSpinner(String text) {
        List<String> filteredList = new ArrayList<>(); // new List
        for (int i = 0; i < sp_citySelect.getCount(); i++) { // for-loop goes through every city
            String item = sp_citySelect.getItemAtPosition(i).toString(); // selects item
            if (item.toLowerCase().contains(text.toLowerCase())) { // if item contains input -> add item to new list
                filteredList.add(item);
            }
        }

        // make adapter with new list and puts into spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, filteredList);
        sp_citySelect.setAdapter(adapter);
    }


    private void fillSpinner() {
        // Fill spinner with data from database via ConnectionHelper
        try {

            // gets db and opens it
            db = this.dbHelper.getReadableDatabase();
            cursor = db.rawQuery("SELECT * FROM cities", null);
            ArrayList<String> data = new ArrayList<>();
            String cityName;

            // make the first spinner option 'default'
            data.add("Select city");

            // takes every item and puts cityname into the arrList<>data
            while (cursor.moveToNext()) {
                cityName = cursor.getString(1);
                data.add(cityName);
            }

            // makes an adapter from arrList<>data and puts adapter into spinner
            ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, data);
            sp_citySelect.setAdapter(adapter);
            db.close();

        } catch (Exception e) {
            Log.e("Spinner error: ", e.getMessage());
        }
    }


    @SuppressLint("MissingPermission")
    public void getWeatherFromCurrentLocation() {
        // checks permissions
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            // Permission not granted, handle it here or request permission again
            return;
        }

        // gets location and creates weather_url
        flpc = LocationServices.getFusedLocationProviderClient(this);
        try {
            flpc.getLastLocation().addOnSuccessListener(location -> {
                weather_url = "https://api.weatherbit.io/v2.0/current?" + "lat=" + location.getLatitude() + "&lon=" + location.getLongitude() + "&key=" + api_id1;

                requestQueue();
            });
        } catch (Exception e) {
            Log.e("error: ", e.getMessage());
        }
    }

    public void getWeatherFromSearchLocation(String searchInput) {
        // creates the weather_url from searchInput
        weather_url = "https://api.weatherbit.io/v2.0/current?" + "&city=" + searchInput + "&key=" + api_id1;

        requestQueue();
    }

    public void requestQueue() {
        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest stringReq = new StringRequest(Request.Method.GET, weather_url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject obj = new JSONObject(response);
                            JSONArray arr = obj.getJSONArray("data");

                            // Takes first or only item (there is only one item)
                            JSONObject weather = arr.getJSONObject(0);


                            // see https://www.weatherbit.io/api/weather-current under Example Response (JSON)
                            String city = weather.getString("city_name");
                            String temperature = Double.toString(weather.getDouble("temp"));

                            tv_temperature.setText(temperature + " °C");
                            tv_city.setText(city);


                            // see https://www.weatherbit.io/api/codes
                            JSONObject weatherJSONObject = weather.getJSONObject("weather");
                            int code = weatherJSONObject.getInt("code");

                            // if rain
                            if (200 <= code && code <= 522) {
                                tv_weather_drawable.setBackgroundResource(R.drawable.baseline_rain);
                            }
                            // if snow
                            if (600 <= code && code <= 623) {
                                tv_weather_drawable.setBackgroundResource(R.drawable.baseline_frosty);
                            }
                            // if foggy
                            if (700 <= code && code <= 751) {
                                tv_weather_drawable.setBackgroundResource(R.drawable.baseline_foggy);
                            }
                            // if clear
                            if (800 <= code && code <= 801) {
                                // switch case to determine if it's day or night (pod: "part of day")
                                switch (weather.getString("pod")) {
                                    case "d":
                                        tv_weather_drawable.setBackgroundResource(R.drawable.baseline_sunny);
                                        break;
                                    case "n":
                                        tv_weather_drawable.setBackgroundResource(R.drawable.baseline_clear_night);
                                        break;
                                }
                            }
                            // if cloudy
                            if (802 <= code && code <= 804) {
                                tv_weather_drawable.setBackgroundResource(R.drawable.baseline_cloudy);
                            }
                        } catch (Exception e) {
                            Log.e("error: ", e.getMessage());
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
            }
        });
        queue.add(stringReq);
    }


    // Method to save location to SharedPreferences
    public void saveUserLocation(UserLocation location) {
        SharedPreferences sharedPreferences = this.getSharedPreferences("locations", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Retrieve the existing list of ToiletLocations from SharedPreferences
        List<UserLocation> existingLocations = getUserLocations();

        // Append the new location to the existing list
        existingLocations.add(location);

        // Convert list of locations to JSON string
        Gson gson = new Gson();
        String updatedLocationsJson = gson.toJson(existingLocations);

        // Save the location string
        editor.putString("location", updatedLocationsJson);
        editor.apply();
    }

    // Method to retrieve list of locations from SharedPreferences
    public List<UserLocation> getUserLocations() {
        SharedPreferences sharedPreferences = this.getSharedPreferences("locations", Context.MODE_PRIVATE);

        // Retrieve the JSON string of locations from SharedPreferences
        String locationsJson = sharedPreferences.getString("location", null);

        // If locationsJson is not null, parse it and return the list of locations
        if (locationsJson != null) {
            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<UserLocation>>() {
            }.getType();
            return gson.fromJson(locationsJson, type);
        } else {
            // If the data is null or empty, return an empty list
            return new ArrayList<>();
        }
    }


    //region Permissions
    ArrayList<String> permissionsList;
    String[] permissionsStr = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.INTERNET
    };
    int permissionsCount = 0;
    ActivityResultLauncher<String[]> permissionsLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), new ActivityResultCallback<Map<String, Boolean>>() {
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onActivityResult(Map<String, Boolean> result) {
            ArrayList<Boolean> list = new ArrayList<>(result.values());
            permissionsList = new ArrayList<>();
            permissionsCount = 0;
            for (int i = 0; i < list.size(); i++) {
                if (shouldShowRequestPermissionRationale(permissionsStr[i])) {
                    permissionsList.add(permissionsStr[i]);
                } else if (!hasPermission(MainActivity.this, permissionsStr[i])) {
                    permissionsCount++;
                }
            }
            if (permissionsList.size() > 0) {
                //Some permissions are denied and can be asked again.
                askForPermissions(permissionsList);
            } else if (permissionsCount > 0) {
                //Show alert dialog
                showPermissionDialog();
            } else {
                // All permissions granted. Do your stuff 🤞
            }
        }
    });

    private boolean hasPermission(Context context, String permissionStr) {
        return ContextCompat.checkSelfPermission(context, permissionStr) == PackageManager.PERMISSION_GRANTED;
    }

    private void askForPermissions(ArrayList<String> permissionsList) {
        String[] newPermissionStr = new String[permissionsList.size()];
        for (int i = 0; i < newPermissionStr.length; i++) {
            newPermissionStr[i] = permissionsList.get(i);
        }
        if (newPermissionStr.length > 0) {
            permissionsLauncher.launch(newPermissionStr);
        } else {
        /* User has pressed 'Deny & Don't ask again' so we have to show the enable permissions dialog
        which will lead them to app details page to enable permissions from there. */
            showPermissionDialog();
        }
    }

    AlertDialog alertDialog;

    private void showPermissionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permission required").setMessage("Some permissions are need to be allowed to use this app without any problems.").setPositiveButton("Continue", (dialog, which) -> {
            dialog.dismiss();
        });
        if (alertDialog == null) {
            alertDialog = builder.create();
            if (!alertDialog.isShowing()) {
                alertDialog.show();
            }
        }
    }
//endregion
}