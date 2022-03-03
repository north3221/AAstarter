package com.north3221.aastarter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.content.ContextCompat;

public class ServiceActivity extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent serviceintent = new Intent(context, ServerService.class);
        String action = intent.getAction();
        if (action.equalsIgnoreCase("com.north3221.aastarter.service.START")) {
            ContextCompat.startForegroundService(context, serviceintent);
        }
        if (action.equalsIgnoreCase("com.north3221.aastarter.service.STOP")) {
            context.stopService(serviceintent);
        }


    }
}
