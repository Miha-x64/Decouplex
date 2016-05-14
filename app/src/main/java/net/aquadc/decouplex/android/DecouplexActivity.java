package net.aquadc.decouplex.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;

import java.lang.reflect.Method;

import static net.aquadc.decouplex.Decouplex.*;

/**
 * Created by miha on 14.05.16.
 *
 */
public abstract class DecouplexActivity extends AppCompatActivity {

    private BroadcastReceiver receiver;

    @Override
    protected void onStart() {
        super.onStart();
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!ACTION.equals(intent.getAction()))
                    return;

                try {
                    Bundle bun = intent.getExtras();
                    Class face = Class.forName(bun.getString("face"));
                    String method = bun.getString("method");

                    Method handler = responseHandler(DecouplexActivity.this.getClass(), face, method);
                    handler.invoke(DecouplexActivity.this, bun.get("result"));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };

        LocalBroadcastManager
                .getInstance(this)
                .registerReceiver(receiver, new IntentFilter(ACTION));
    }

    @Override
    protected void onStop() {
        LocalBroadcastManager
                .getInstance(this)
                .unregisterReceiver(receiver);
        receiver = null;
        super.onStop();
    }
}
