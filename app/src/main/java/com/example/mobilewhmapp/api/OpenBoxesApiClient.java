package com.example.mobilewhmapp.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class OpenBoxesApiClient {
    private static final int TIMEOUT = 15000; // 15 seconds
    private static final int MAX_RETRIES = 2;

    private Context context;
    private String baseUrl;
    private String authType; // "Basic" or "ApiKey"
    private String basicAuthToken; // base64 encoded "username:password"
    private String apiKey;

    private SharedPreferences preferences;

    public OpenBoxesApiClient(Context context, String baseUrl) {
        this.context = context;
        if (baseUrl.endsWith("/"))
            this.baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        else
            this.baseUrl = baseUrl;
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        loadAuth();
    }

    private void loadAuth() {
        authType = preferences.getString("auth_type", "Basic");
        basicAuthToken = preferences.getString("basic_auth_token", null);
        apiKey = preferences.getString("api_key", null);
    }

    public void saveBasicAuth(String username, String password) {
        basicAuthToken = Base64.encodeToString((username + ":" + password).getBytes(), Base64.NO_WRAP);
        authType = "Basic";
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("auth_type", authType);
        editor.putString("basic_auth_token", basicAuthToken);
        editor.remove("api_key");
        editor.apply();
    }

    public void saveApiKey(String key) {
        apiKey = key;
        authType = "ApiKey";
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("auth_type", authType);
        editor.putString("api_key", apiKey);
        editor.remove("basic_auth_token");
        editor.apply();
    }

    public void clearAuth() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove("basic_auth_token");
        editor.remove("api_key");
        editor.remove("auth_type");
        editor.apply();
        basicAuthToken = null;
        apiKey = null;
        authType = null;
    }

    public Object get(String endpoint) throws Exception {
        return request("GET", endpoint, null);
    }

    public JSONObject post(String endpoint, JSONObject payload) throws Exception {
        Object response = request("POST", endpoint, payload.toString());
        if (response instanceof JSONObject) {
            return (JSONObject) response;
        }
        return null;
    }

    public JSONObject put(String endpoint, JSONObject payload) throws Exception {
        Object response = request("PUT", endpoint, payload.toString());
        if (response instanceof JSONObject) {
            return (JSONObject) response;
        }
        return null;
    }

    private Object request(String method, String endpoint, String jsonPayload) throws Exception {
        int retries = 0;
        Exception lastException = null;

        while (retries < MAX_RETRIES) {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(baseUrl + endpoint);
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(TIMEOUT);
                connection.setReadTimeout(TIMEOUT);
                connection.setRequestMethod(method);
                connection.setRequestProperty("Accept", "application/json");

                if ("Basic".equals(authType) && basicAuthToken != null) {
                    connection.setRequestProperty("Authorization", "Basic " + basicAuthToken);
                } else if ("ApiKey".equals(authType) && apiKey != null) {
                    connection.setRequestProperty("X-API-KEY", apiKey);
                }

                if (jsonPayload != null) {
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type", "application/json");
                    OutputStream os = connection.getOutputStream();
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                    writer.write(jsonPayload);
                    writer.flush();
                    writer.close();
                    os.close();
                }

                int responseCode = connection.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    // Clear auth and throw exception for session renewal
                    clearAuth();
                    throw new Exception("Unauthorized - 401");
                }

                InputStream is;
                if (responseCode >= 200 && responseCode < 400) {
                    is = connection.getInputStream();
                } else {
                    is = connection.getErrorStream();
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();

                String response = sb.toString();

                if (response.startsWith("[")) {
                    return new JSONArray(response);
                } else if (response.startsWith("{")) {
                    return new JSONObject(response);
                } else {
                    return response;
                }

            } catch (Exception e) {
                lastException = e;
                retries++;
                if (retries >= MAX_RETRIES) {
                    throw lastException;
                }
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
        throw lastException;
    }
}
