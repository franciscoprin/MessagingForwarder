package dk.stacktrace.messagingforwarder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.Telephony;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsMessage;
import android.util.Log;

import java.net.MalformedURLException;
import java.net.URL;

public class IncomingMessageReceiver extends BroadcastReceiver {
    private static final String TAG = IncomingMessageReceiver.class.getName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Handling message for forwarding");
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        // VALIDATIONS:
        if (!preferences.contains("phone_number")) {
            Log.w(TAG, "Phone number to forward from not set. Will not forward any messages");
            return;
        }
        if (!preferences.contains("target_URL")) {
            Log.w(TAG, "URL to forward to not set. Will not forward any messages");
            return;
        }
        if (!preferences.contains("service_username")) {
            Log.w(TAG, "Username to auth with service not set. Will not forward any messages");
            return;
        }
        if (!preferences.contains("service_password")) {
            Log.w(TAG, "Password to auth with service not set. Will not forward any messages");
            return;
        }
        // if (!preferences.getBoolean("enable", false)) {
        //     Log.w(TAG, "Messaging Forwarding was disable");
        //     return;
        // }

        // Getting shared variables:
        String refreshToken = null;
        String newRefreshToken = null;
        TokenManager tokenManager = null;
        String[] arg = new String[2];
        String msg = null;
        boolean username = preferences.getString("service_username", null);
        boolean password = preferences.getString("service_password", null);
        String baseURL = preferences.getString("target_URL", "");
        String phone_number = preferences.getString("phone_number", "");

        // Setting ulr:
        URL target_url;
        try {
            target_url = new URL(preferences.getString("target_URL", "") + "/tmd/message/");
        } catch (MalformedURLException e) {
            Log.w(TAG, "Unable to parse URL: " + e.getMessage());
            return;
        }

        SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        for (SmsMessage message : messages) {
            // Getting token:
            refreshToken = preferences.getString("refresh_token", null);
            tokenManager = new TokenManager(
                baseURL,
                username,
                password,
                refreshToken
            );
            arg = tokenManager.get_tokens();
            newRefreshToken = arg[0];
            accessToken = arg[1];

            // Updating refresh token:
            if(newRefreshToken != refreshToken){
                preferences.edit().putString("refresh_token", newRefreshToken).apply();
            }

            // Sending sms:
            msg = message.getDisplayMessageBody();
            new Thread(
                new HttpPostThread(
                    target_url,
                    msg,
                    phone_number,
                    accessToken,
                )
            ).start();
        }
    }
}
