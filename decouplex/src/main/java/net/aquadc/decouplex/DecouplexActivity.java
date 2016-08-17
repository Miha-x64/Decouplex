package net.aquadc.decouplex;

import android.content.BroadcastReceiver;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by miha on 14.05.16.
 *
 */
public abstract class DecouplexActivity extends AppCompatActivity {

    private BroadcastReceiver decouplexReceiver;

    @Override
    protected void onStart() {
        super.onStart();
        if (decouplexReceiver == null) {
            decouplexReceiver = new DecouplexReceiver(this);
        }

        LocalBroadcastManager
                .getInstance(this)
                .registerReceiver(decouplexReceiver, DecouplexReceiver.createFilter());
    }

    @Override
    protected void onStop() {
        LocalBroadcastManager
                .getInstance(this)
                .unregisterReceiver(decouplexReceiver);
        super.onStop();
    }
}
