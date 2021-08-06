package dk.stacktrace.messagingforwarder;

import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

class HttpPostThread implements Runnable {
    private static final String TAG = HttpPostThread.class.getName();
    private final URL url;
    private final String message;
    private final String phone;

    public HttpPostThread(URL url, String message, String phone) {
        this.url = url;
        this.phone = phone;
        this.message = message;
    }

    @Override
    public void run() {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection)this.url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);

            String jsonInputString = String.format(
                "{\"phone\": \"%s\", \"sms\": \"%s\"}",
                this.phone,
                this.message
            );

            try(OutputStream out = connection.getOutputStream()) {
                // Request:
                byte[] input = jsonInputString.getBytes("UTF-8");
                out.write(input, 0, input.length);
                out.flush();
                // Response:
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
    }
}
