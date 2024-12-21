package com.nihility;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public abstract class InternalMessenger extends BroadcastReceiver {
    private final LocalBroadcastManager localBroadcast;

    public InternalMessenger(Context context) {
        localBroadcast = LocalBroadcastManager.getInstance(context);
    }

    public void send(Intent intent) {
        localBroadcast.sendBroadcast(intent);
    }

    public void register(IntentFilter intentFilter) {
        localBroadcast.registerReceiver(this, intentFilter);
    }
}
