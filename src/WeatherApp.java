import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class WeatherApp {

    public static JSONObject getWeatherData(String locationName) {
        JSONArray locationData = getLocationData(locationName);

        if (locationData == null || locationData.size() == 0) {
            System.out.println("Error: Location not found");
            return null;
        }

        JSONObject location = (JSONObject) locationData.get(0);
        double latitude = (double) location.get("latitude"); 
        double longitude = (double) location.get("longitude");

        String url = "https://api.open-meteo.com/v1/forecast?latitude=" + latitude + "&longitude=" + longitude +
         "&hourly=temperature_2m,relativehumidity_2m,weathercode,windspeed_10m&timezone=auto";

        try {

            HttpURLConnection connection = fetchApiResponce(url);
            if (connection == null || connection.getResponseCode() != 200) {
                System.out.println("Error: Could not connect to API");
                return null;
            }
            
                StringBuilder resultJson = new StringBuilder();
                Scanner scanner = new Scanner(connection.getInputStream());

                while (scanner.hasNext()) {
                    resultJson.append(scanner.nextLine());
                }
                scanner.close();
                connection.disconnect();

                JSONParser parser = new JSONParser();
                JSONObject resulJsonObject = (JSONObject) parser.parse(String.valueOf(resultJson));



                JSONObject hourly = (JSONObject) resulJsonObject.get("hourly");

                JSONArray time = (JSONArray) hourly.get("time");
                int index = findIndexOfCurrentTime(time);
                JSONArray temperatureData = (JSONArray) hourly.get("temperature_2m");
                if (temperatureData == null || temperatureData.size() <= index) return null;
                double currentTemperature = (double) temperatureData.get(index);

                JSONArray weatherCodeData = (JSONArray) hourly.get("weathercode");
                if (weatherCodeData == null || weatherCodeData.size() <= index) return null;
                String weatherCondition = weatherCodeToString((long) weatherCodeData.get(index));

                JSONArray humidityData = (JSONArray) hourly.get("relativehumidity_2m");
                if (humidityData == null || humidityData.size() <= index) return null;
                long humidity = (long) humidityData.get(index);
                JSONArray windSpeedData = (JSONArray) hourly.get("windspeed_10m");
                JSONObject weatherData = new JSONObject();
                weatherData.put("temperature", currentTemperature);
                weatherData.put("weather_condition", weatherCondition);
                weatherData.put("humidity", humidity);
                if (windSpeedData != null && windSpeedData.size() > index) {
                    double windSpeed = (double) windSpeedData.get(index);
                    weatherData.put("windspeed", windSpeed);
                }
                return weatherData;
        }catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private static String weatherCodeToString(long weatherCode) {
        String weatherCondition = "";
        if (weatherCode == 0L) {
            weatherCondition = "Clear";
        }
        else if (weatherCode == 1L || weatherCode == 2L || weatherCode == 3L) {
            weatherCondition = "Cloudy";
        }
        else if (weatherCode == 45L || weatherCode == 48L) {
            weatherCondition = "Fog";
        }
        else if (weatherCode == 51L || weatherCode == 53L || weatherCode == 55L) {
            weatherCondition = "Drizzle";
        }
        else if (weatherCode == 61L || weatherCode == 63L || weatherCode == 65L) {
            weatherCondition = "Rain";
        }
        else if (weatherCode == 71L || weatherCode == 73L || weatherCode == 75L) {
            weatherCondition = "Snow";
        }
        else if (weatherCode == 95L || weatherCode == 96L || weatherCode == 99L) {
            weatherCondition = "Thunderstorm";
        } 
        else {
            weatherCondition = "Unknown";
        }
        return weatherCondition;
    }

    private static int findIndexOfCurrentTime(JSONArray timeList) {
        String currentTime = getCurrentTime();
        for (int i = 0; i < timeList.size(); i++) {
            String time = (String) timeList.get(i);
            if (time.equalsIgnoreCase(currentTime)) {
                return i;
            }
        }
        return 0;
    }

    public static String getCurrentTime() {
       LocalDateTime currentDateTime = LocalDateTime.now();

       DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:00"); 
         String formattedDateTime = currentDateTime.format(formatter);
         return formattedDateTime;
    }

    public static JSONArray getLocationData(String locationName) {
        locationName = locationName.replaceAll(" ", "+");
        String url = "https://geocoding-api.open-meteo.com/v1/search?name=" + locationName + "&count=10&language=en&format=json";

        try {
            HttpURLConnection connection = fetchApiResponce(url);
            if (connection == null || connection.getResponseCode() != 200) {
                System.out.println("Error: Could not connect to API");
                return null;
            }
            else {
                StringBuilder resultJson = new StringBuilder();
                Scanner scanner = new Scanner(connection.getInputStream());

                while (scanner.hasNext()) {
                    resultJson.append(scanner.nextLine());
                }
                scanner.close();
                connection.disconnect();

                JSONParser parser = new JSONParser();
                JSONObject resulJsonObject = (JSONObject) parser.parse(String.valueOf(resultJson));
                JSONArray locationData = (JSONArray) resulJsonObject.get("results");
                return locationData;
            }


        }catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static HttpURLConnection fetchApiResponce(String url)  {
        try {
            URL apiUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection(); 
            connection.setRequestMethod("GET");
            connection.connect();   
            return connection;
            
        }catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
