package com.example.gazcalculationapp;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private ArrayList<String> items;
    private ArrayAdapter<String> adapter;
    private final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()); // Adjust thread pool size if needed
    private final Handler mainHandler = new Handler(Looper.getMainLooper()); // Handler to post results to the main thread
    private final Map<String, String> locationCache = new HashMap<>(); // Cache for storing locations

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView listView = findViewById(R.id.listView);
        EditText editText = findViewById(R.id.editText);
        Button addButton = findViewById(R.id.addButton);
        Button calculateButton = findViewById(R.id.calculateButton);
        Button clearButton = findViewById(R.id.clearButton);

        // قايمه للبيانات
        items = new ArrayList<>();

        // إعداد ArrayAdapter
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items);

        // ربط الAdaptor  بـ ListView
        listView.setAdapter(adapter);

        // زرار علشان اضيف
        addButton.setOnClickListener(v -> {
            String newItem = editText.getText().toString();
            if (!newItem.isEmpty()) {
                items.add(newItem);
                adapter.notifyDataSetChanged();
                editText.setText("");

                // Calculate location for the new item asynchronously
                calculateLocationAsync(newItem);
            }
        });

        //  زرار علشان احسب اللوكيشن
        calculateButton.setOnClickListener(v -> {
            ArrayList<String> itemsToProcess = new ArrayList<>(items);

            // Execute the geocoding asynchronously
            executorService.submit(() -> {
                // حساب المواقع لكل العناصر في القائمة
                ArrayList<String> locations = new ArrayList<>();
                ArrayList<String> errors = new ArrayList<>();
                try {
                    locations = convertItemsToLocations(itemsToProcess, errors);
                } catch (Exception e) {
                    Log.e("Geocoder", "Unexpected error occurred", e);
                }
                ArrayList<String> finalLocations = locations;
                ArrayList<String> finalErrors = errors;
                mainHandler.post(() -> {
                    Intent intent = new Intent(MainActivity.this, ResultActivity.class);
                    intent.putStringArrayListExtra("locations", finalLocations);
                    intent.putStringArrayListExtra("errors", finalErrors);
                    startActivity(intent);
                });
            });
        });

        //   زرار علشان امسح البيانات كلها
        clearButton.setOnClickListener(v -> {
            items.clear();
            adapter.notifyDataSetChanged();
            editText.setText("");
        });
    }

    // Asynchronously calculate location for a single item
    private void calculateLocationAsync(String item) {
        // بشوف لو اللوكيشن موجود قبل كدة
        if (locationCache.containsKey(item)) {
            String cachedLocation = locationCache.get(item);
            mainHandler.post(() -> {
                Toast.makeText(MainActivity.this, cachedLocation, Toast.LENGTH_SHORT).show();
            });
            return;
        }

        executorService.submit(() -> {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocationName(item, 1);
                String location;
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    location = "Latitude: " + address.getLatitude() + ", Longitude: " + address.getLongitude() + ", Name: " + item;
                    // Cache the result
                    locationCache.put(item, location);
                } else {
                    location = "Location not found for " + item;
                    // اي حاجه غلط بتتمسح
                    mainHandler.post(() -> {
                        items.remove(item);
                        adapter.notifyDataSetChanged();
                        Toast.makeText(MainActivity.this, location, Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                // Update the UI with the result
                mainHandler.post(() -> {
                    Toast.makeText(MainActivity.this, location, Toast.LENGTH_SHORT).show();
                });
            } catch (IOException e) {
                Log.e("Geocoder", "Error finding location for " + item, e);
                mainHandler.post(() -> {
                    Toast.makeText(MainActivity.this, "Error finding location for " + item, Toast.LENGTH_SHORT).show();
                    // Remove item from the list if there is an error
                    items.remove(item);
                    adapter.notifyDataSetChanged();
                });
            }
        });
    }


    // علشان احول العناصر إلى مواقع
    private ArrayList<String> convertItemsToLocations(ArrayList<String> items, ArrayList<String> errors) throws IOException {
        ArrayList<String> locations = new ArrayList<>();
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        // Process items in batches to avoid overloading the Geocoder
        final int batchSize = 20; // Process 20 items at a time
        for (int i = 0; i < items.size(); i += batchSize) {
            int end = Math.min(i + batchSize, items.size());
            List<String> batch = items.subList(i, end);

            List<String> batchLocations = new ArrayList<>();
            for (String item : batch) {
                // Use cached location if available
                if (locationCache.containsKey(item)) {
                    batchLocations.add(locationCache.get(item));
                } else {
                    try {
                        List<Address> addresses = geocoder.getFromLocationName(item, 1);
                        String location;
                        if (addresses != null && !addresses.isEmpty()) {
                            Address address = addresses.get(0);
                            location = "Latitude: " + address.getLatitude() + ", Longitude: " + address.getLongitude() + ", Name: " + item;
                        } else {
                            location = "Location not found for " + item;
                        }
                        // Cache the result
                        locationCache.put(item, location);
                        batchLocations.add(location);
                    } catch (IOException e) {
                        Log.e("Geocoder", "Error finding location for " + item, e);
                        errors.add("Error finding location for " + item);
                        batchLocations.add("Error finding location for " + item);
                    }
                }
            }
            locations.addAll(batchLocations);

            // Sleep for a short time to avoid overloading the Geocoder
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.e("Geocoder", "Batch processing interrupted", e);
            }
        }
        return locations;
    }
}
