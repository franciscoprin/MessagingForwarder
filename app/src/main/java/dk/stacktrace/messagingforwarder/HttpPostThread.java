package dk.stacktrace.messagingforwarder;

import android.util.Log;
import android.content.SharedPreferences;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

class HttpPostThread implements Runnable {
    private static final String TAG = HttpPostThread.class.getName();
    private final URL url;
    private final String message;
    private final String phone;
    private final String senderPhoneNumber;
    private final SharedPreferences preferences;

    public HttpPostThread(
        URL url,
        String message,
        String phone,
        String senderPhoneNumber,
        SharedPreferences preferences
    ) {
        this.url = url;
        this.phone = phone;
        this.message = message;
        this.senderPhoneNumber = senderPhoneNumber;
        this.preferences = preferences;
    }

    @Override
    public void run() {
        // Getting token:
        String[] arg = new String[2];
        String baseURL = this.preferences.getString("target_URL", "");
        String username = this.preferences.getString("service_username", null);
        String password = this.preferences.getString("service_password", null);
        String refreshToken = this.preferences.getString("refresh_token", null);
        TokenManager tokenManager = new TokenManager(
            baseURL,
            username,
            password,
            refreshToken
        );
        arg = tokenManager.get_tokens();
        String newRefreshToken = arg[0];
        String accessToken = arg[1];

        // Updating refresh token:
        Log.i(TAG, "[MessageForwarder][run] refreshToken: " + refreshToken);
        Log.i(TAG, "[MessageForwarder][run] newRefreshToken: " + newRefreshToken);
        if( refreshToken == null || newRefreshToken != refreshToken){
            this.preferences.edit().putString("refresh_token", newRefreshToken).apply();
        }

        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection)this.url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + accessToken);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);

            String jsonInputString = String.format(
                "{\"inventory_id\": \"%s\", \"text\": \"%s\", \"sender_phone_number\": \"%s\"}",
                this.phone,
                this.message,
                this.senderPhoneNumber
            );

            try {
                OutputStream out = connection.getOutputStream();

                // Request:
                byte[] input = jsonInputString.getBytes("UTF-8");
                out.write(input, 0, input.length);
                out.flush();

                // Response:
                int status = connection.getResponseCode();
                Log.i(TAG, "Server replied with HTTP status: " + status);
                out.close();
            }
            catch (Exception e) {
                Log.i(TAG, "[MessageForwarder][run] generic error", e);
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
}
