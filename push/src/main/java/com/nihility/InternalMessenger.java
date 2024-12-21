package com.nihility;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.ArrayList;

public class InternalMessenger extends BroadcastReceiver {
    private final LocalBroadcastManager localBroadcast;
    private final ArrayList<MessageListener> listeners = new ArrayList<>();

    public InternalMessenger(Context context) {
        localBroadcast = LocalBroadcastManager.getInstance(context);
    }

    public void send(Intent intent) {
        localBroadcast.sendBroadcast(intent);
    }

    public void register(IntentFilter intentFilter) {
        localBroadcast.registerReceiver(this, intentFilter);
    }

    public void addListener(MessageListener listener) {
        listeners.add(listener);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        for (MessageListener listener : listeners) {
            listener.onReceive(intent);
        }
    }
}
