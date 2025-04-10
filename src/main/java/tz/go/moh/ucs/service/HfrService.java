package tz.go.moh.ucs.service;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

import static tz.go.moh.ucs.Main.processAdminHierarchyData;
import static tz.go.moh.ucs.Main.processHfrResponse;

public class HfrService {
    private static final Config config = ConfigFactory.load();

    private static final String BASE_URL_GET_HEALTH_FACILITIES = config.getString("hfr.baseUrlGetHealthFacilities");
    private static final String BASE_URL_GET_HIERARCHY = config.getString("hfr.baseUrlGetHierarchy");

    private static final String USERNAME = config.getString("hfr.username");
    private static final String PASSWORD = config.getString("hfr.password");

    public static void fetchHealthFacilityData(int currentPage) throws Exception {
        int totalPageCount;
        do {
            System.out.println("Fetching health facility data... PAGE : " + currentPage);
            String url = BASE_URL_GET_HEALTH_FACILITIES + currentPage;
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Basic " + getBasicAuth());
            connection.setRequestProperty("Accept", "application/json");

            int responseCode = connection.getResponseCode();

            if (responseCode == 200) { // OK
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                JSONObject rootObject = new JSONObject(response.toString());

                // Get the metaData object
                JSONObject metaDataObject = rootObject.getJSONObject("metaData");
                totalPageCount = metaDataObject.getInt("pageCount");
                currentPage = metaDataObject.getInt("currentPage");

                // Process the data
                JSONArray dataArray = rootObject.getJSONArray("data");
                processData(dataArray);

                // Move to the next page
                currentPage++;
            } else {
                throw new Exception("Failed to fetch data. HTTP response code: " + responseCode);
            }

            connection.disconnect();
        } while (currentPage <= totalPageCount);
    }


    public static void fetchAdminHierarchData(int currentPage) throws Exception {
        int totalPageCount;
        do {
            System.out.println("Fetching health facility data... PAGE : " + currentPage);
            String url = BASE_URL_GET_HIERARCHY + currentPage;
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Basic " + getBasicAuth());
            connection.setRequestProperty("Accept", "application/json");

            int responseCode = connection.getResponseCode();

            if (responseCode == 200) { // OK
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                JSONObject rootObject = new JSONObject(response.toString());

                // Get the metaData object
                JSONObject metaDataObject = rootObject.getJSONObject("metaData");
                totalPageCount = metaDataObject.getInt("pageCount");
                currentPage = metaDataObject.getInt("currentPage");

                // Process the data
                JSONArray dataArray = rootObject.getJSONArray("data");
                processAdminHierarchyData(dataArray);

                // Move to the next page
                currentPage++;
            } else {
                throw new Exception("Failed to fetch data. HTTP response code: " + responseCode);
            }

            connection.disconnect();
        } while (currentPage <= totalPageCount);
    }

    private static String getBasicAuth() {
        String auth = USERNAME + ":" + PASSWORD;
        return Base64.getEncoder().encodeToString(auth.getBytes());
    }

    private static void processData(JSONArray dataNode) {
        // Process the data as needed
        processHfrResponse(dataNode);
    }
}
