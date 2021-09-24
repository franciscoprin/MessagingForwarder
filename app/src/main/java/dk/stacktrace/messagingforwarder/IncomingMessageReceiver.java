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
        Log.i(TAG, "[MessageForwarder] Handling message for forwarding");
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        // VALIDATIONS:
        if (!preferences.contains("phone_number")) {
            Log.i(TAG, "[MessageForwarder] Phone number to forward from not set. Will not forward any messages");
            return;
        }
        if (!preferences.contains("target_URL")) {
            Log.i(TAG, "[MessageForwarder] URL to forward to not set. Will not forward any messages");
            return;
        }
        if (!preferences.contains("service_username")) {
            Log.i(TAG, "[MessageForwarder] Username to auth with service not set. Will not forward any messages");
            return;
        }
        if (!preferences.contains("service_password")) {
            Log.i(TAG, "[MessageForwarder] Password to auth with service not set. Will not forward any messages");
            return;
        }
        // if (!preferences.getBoolean("enable", false)) {
        //     Log.i(TAG, "Messaging Forwarding was disable");
        //     return;
        // }

        // Getting shared variables:
        String msg = null;
        String senderPhoneNumber = null;
        String phone_number = preferences.getString("phone_number", "");

        // Setting ulr:
        URL target_url;
        try {
            target_url = new URL(preferences.getString("target_URL", "") + "/tmd/message/");
        } catch (MalformedURLException e) {
            Log.i(TAG, "[MessageForwarder] Unable to parse URL: " + e.getMessage());
            return;
        }

        SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        for (SmsMessage message : messages) {
            // Sending sms:
            msg = message.getDisplayMessageBody();
            senderPhoneNumber = message.getDisplayOriginatingAddress().substring(3);

            new Thread(
                new HttpPostThread(
                    target_url,
                    msg,
                    phone_number,
                    senderPhoneNumber,
                    preferences
                )
            ).start();
        }
    }
}
