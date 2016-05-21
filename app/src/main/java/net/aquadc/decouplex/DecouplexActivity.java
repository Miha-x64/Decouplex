package net.aquadc.decouplex;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;

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
                    switch (intent.getAction()) {
                        case ACTION_RESULT: {
                            Decouplex decouplex = Decouplex.find(resp.getInt("id"));
                            decouplex.dispatchResult(DecouplexActivity.this, resp);
                            break;
                        }
                        case ACTION_RESULT_BATCH: {
                            Decouplex.dispatchResults(DecouplexActivity.this, resp);
                            break;
                        }
                        case ACTION_ERR: {
                            Bundle req = resp.getBundle("request");
                            assert req != null;
                            Decouplex decouplex = Decouplex.find(req.getInt("id"));
                            decouplex.dispatchError(DecouplexActivity.this, req, resp);
                            break;
                        }
                    }
                }
            };
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_RESULT);
        filter.addAction(ACTION_RESULT_BATCH);
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
