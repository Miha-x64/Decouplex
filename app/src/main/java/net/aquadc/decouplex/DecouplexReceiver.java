package net.aquadc.decouplex;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import static net.aquadc.decouplex.Decouplex.ACTION_ERR;
import static net.aquadc.decouplex.Decouplex.ACTION_RESULT;
import static net.aquadc.decouplex.Decouplex.ACTION_RESULT_BATCH;

/**
 * Created by miha on 24.05.16.
 * BroadcastReceiver to serve an Activity or Fragment
 */
public class DecouplexReceiver extends BroadcastReceiver {

    public static IntentFilter createFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_RESULT);
        filter.addAction(ACTION_RESULT_BATCH);
        filter.addAction(ACTION_ERR);
        return filter;
    }

    private final Object resultHandler;

    public DecouplexReceiver(Object resultHandler) {
        this.resultHandler = resultHandler;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle resp = intent.getExtras();
        switch (intent.getAction()) {
            case ACTION_RESULT: {
                Decouplex decouplex = Decouplex.find(resp.getInt("id"));
                decouplex.dispatchResult(resultHandler, resp);
                break;
            }
            case ACTION_RESULT_BATCH: {
                Decouplex.dispatchResults(resultHandler, resp);
                break;
            }
            case ACTION_ERR: {
                Bundle req = resp.getBundle("request");
                assert req != null;
                Decouplex decouplex = Decouplex.find(req.getInt("id"));
                decouplex.dispatchError(resultHandler, req, resp);
                break;
            }
        }
    }

}
