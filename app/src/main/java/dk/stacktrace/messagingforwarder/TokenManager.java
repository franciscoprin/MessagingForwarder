package dk.stacktrace.messagingforwarder;

import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.MalformedURLException;
// import org.json.simple.JSONObject;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class TokenManager {
    private static final String TAG = HttpPostThread.class.getName();
    private String baseURL;
    private String username;
    private String password;
    private String refreshToken;

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

    private JSONObject getResponse(HttpURLConnection conn) throws IOException, JSONException {
        InputStreamReader in = new InputStreamReader((InputStream) conn.getContent());
        BufferedReader buff = new BufferedReader(in);
        String line;
        StringBuilder builder = new StringBuilder();
        do {
            line = buff.readLine();
            builder.append(line).append("\n");
        } while (line != null);
        buff.close();
        return new JSONObject(builder.toString());
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
            connection = (HttpURLConnection) url.openConnection();
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
                response = this.getResponse(connection);
                accessToken = response.getString("access");
                int status = connection.getResponseCode();
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
                    response = this.getResponse(connection);
                    this.refreshToken = response.getString("refresh");
                    accessToken = response.getString("access");
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
