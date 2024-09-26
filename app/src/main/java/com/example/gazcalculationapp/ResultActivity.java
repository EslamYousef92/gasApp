package com.example.gazcalculationapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ResultActivity extends AppCompatActivity {
    private ArrayList<String> locations;
    private TextView resultTextView,gazTextView;
    private Button recommendButton,gazPrice;
    int price;


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        resultTextView = findViewById(R.id.resultTextView);
        gazTextView=findViewById(R.id.gazTextView);
        recommendButton = findViewById(R.id.recommendButton);
        gazPrice=findViewById(R.id.gazPrice);


        Intent intent = getIntent();
        locations = intent.getStringArrayListExtra("locations");

        displayLocationsAndDistance();

        recommendButton.setOnClickListener(v -> {
            findOptimalRoute();
            displayLocationsAndDistance();
        });
        gazPrice.setOnClickListener(v -> {
            price= Integer.parseInt(gazTextView.getText().toString());
                displayLocationsAndDistance();
        });
    }

    private void displayLocationsAndDistance() {
        if (locations != null) {
            StringBuilder result = new StringBuilder();
            double totalDistance = 0.0;
            double realDistance=totalDistance+(totalDistance*(30/100));


            for (int i = 0; i < locations.size(); i++) {
                result.append(locations.get(i)).append("\n");
                if (i > 0) {
                    double[] prevCoords = parseCoordinates(locations.get(i - 1));
                    double[] currCoords = parseCoordinates(locations.get(i));
                    double distance = calculateDistance(prevCoords[0], prevCoords[1], currCoords[0], currCoords[1]);
                    realDistance += distance;
                }
            }
            result.append("\nTotal Distance: ").append(realDistance).append(" km").append(" "+"and it will cost you"+" "+((realDistance/15)*price)+"L.E");
            resultTextView.setText(result.toString());
        }
    }

    private void findOptimalRoute() {
        // Convert locations into a list of coordinate points
        List<Coordinate> coordinates = new ArrayList<>();
        for (String location : locations) {
            double[] coords = parseCoordinates(location);
            String name = parseName(location);
            coordinates.add(new Coordinate(name, coords[0], coords[1]));
        }

        // Simple nearest neighbor heuristic to solve the TSP
        ArrayList<Coordinate> optimizedRoute = new ArrayList<>();
        Coordinate current = coordinates.get(0);
        optimizedRoute.add(current);
        coordinates.remove(0);

        while (!coordinates.isEmpty()) {
            Coordinate nearest = findNearestNeighbor(current, coordinates);
            optimizedRoute.add(nearest);
            coordinates.remove(nearest);
            current = nearest;
        }

        // Update locations with the optimized route
        locations.clear();
        for (Coordinate coord : optimizedRoute) {
            locations.add("Latitude: " + coord.lat + ", Longitude: " + coord.lon + ", Name: " + coord.name);
        }
    }

    private Coordinate findNearestNeighbor(Coordinate current, List<Coordinate> coordinates) {
        Coordinate nearest = null;
        double shortestDistance = Double.MAX_VALUE;

        for (Coordinate coord : coordinates) {
            double distance = calculateDistance(current.lat, current.lon, coord.lat, coord.lon);
            if (distance < shortestDistance) {
                shortestDistance = distance;
                nearest = coord;
            }
        }

        return nearest;
    }

    private double[] parseCoordinates(String location) {
        String[] parts = location.split(",");
        double lat = Double.parseDouble(parts[0].split(":")[1].trim());
        double lon = Double.parseDouble(parts[1].split(":")[1].trim());
        return new double[]{lat, lon};
    }

    private String parseName(String location) {
        String[] parts = location.split(",");
        return parts[2].split(":")[1].trim();
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the earth in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c; // convert to kilometers
    }

    private static class Coordinate {
        String name;
        double lat;
        double lon;

        Coordinate(String name, double lat, double lon) {
            this.name = name;
            this.lat = lat;
            this.lon = lon;
        }
    }
}