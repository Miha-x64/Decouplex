package net.aquadc.decouplex.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;

import net.aquadc.decouplex.Decouplex;

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
        if (receiver == null) {
            receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Bundle resp = intent.getExtras();
                    if (ACTION_RESULT.equals(intent.getAction())) {
                        Decouplex decouplex = Decouplex.find(resp.getInt("id"));
                        decouplex.dispatchResult(DecouplexActivity.this, resp);
                    } else if (ACTION_ERR.equals(intent.getAction())) {
                        Bundle req = resp.getBundle("request");
                        assert req != null;

                        Decouplex decouplex = Decouplex.find(req.getInt("id"));
                        decouplex.dispatchError(DecouplexActivity.this, req, resp);
                    }
                }
            };
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_RESULT);
        filter.addAction(ACTION_ERR);
        LocalBroadcastManager
                .getInstance(this)
                .registerReceiver(receiver, filter);
    }

    @Override
    protected void onStop() {
        LocalBroadcastManager
                .getInstance(this)
                .unregisterReceiver(receiver);
        super.onStop();
    }
}
