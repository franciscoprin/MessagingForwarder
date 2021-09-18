package dk.stacktrace.messagingforwarder;

import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class TokenManager {
    private final String baseURL;
    private final String username;
    private final String password;
    private final String refreshToken;

    public TokenManager(String baseURL, String username, String password, String refreshToken) {
        this.baseURL = baseURL;
        this.username = username;
        this.password = password;
        this.refreshToken = refreshToken;
    }

    private URL get_url(String path){
      try {
          return new URL(this.baseURL + path);
      } catch (MalformedURLException e) {
          Log.w(TAG, "Unable to parse URL: " + e.getMessage());
          return null;
      }
    }

    private String refresh_token() {
        URL url = this.get_url("/api/token/refresh/");
        HttpURLConnection connection = null;
        String accessToken = null;
        JSONObject response = null;

        if (this.refreshToken == null){
            return null;
        }

        try {
            connection = (HttpURLConnection)url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);

            String jsonInputString = String.format(
                "{\"refresh\": \"%s\"}",
                this.refreshToken
            );

            try(OutputStream out = connection.getOutputStream()) {
                // Request:
                byte[] input = jsonInputString.getBytes("UTF-8");
                out.write(input, 0, input.length);
                out.flush();

                // Response:
                response = out.getResponseMessage();
                accessToken = response.get("access");
                Log.i(TAG, "Server replied with HTTP status: " + status);
                out.close();
            }
        }
        catch (IOException e) {
            Log.w(TAG, "Error communicating with HTTP server", e);
        }
        finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return accessToken;
    }

    public String[] get_tokens() {
        String[] arg = new String[2];
        URL url = this.get_url("api/token/");
        HttpURLConnection connection = null;
        String accessToken = this.refresh_token();
        JSONObject response = null;
        while (accessToken == null) {
            try {
                connection = (HttpURLConnection)url.openConnection();
                connection.setDoOutput(true);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                connection.setRequestProperty("Accept", "application/json");
                connection.setDoOutput(true);

                String jsonInputString = String.format(
                    "{\"username\": \"%s\", \"password\": \"%s\"}",
                    this.username,
                    this.password
                );

                try(OutputStream out = connection.getOutputStream()) {
                    // Request:
                    byte[] input = jsonInputString.getBytes("UTF-8");
                    out.write(input, 0, input.length);
                    out.flush();

                    // Response:
                    response = out.getResponseMessage();
                    this.refreshToken = response.get("refresh");
                    accessToken = response.get("access");
                    out.close();
                }
            }
            catch (IOException e) {
                Log.w(TAG, "Error communicating with HTTP server", e);
            }
            finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
        arg[0] = this.refreshToken;
        arg[1] = accessToken;
        return arg;
    }
}
