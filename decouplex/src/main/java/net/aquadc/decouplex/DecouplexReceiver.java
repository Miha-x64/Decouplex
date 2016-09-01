package net.aquadc.decouplex;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import net.aquadc.decouplex.delivery.DeliveryStrategy;

import static net.aquadc.decouplex.Decouplex.ACTION_ERR;
import static net.aquadc.decouplex.Decouplex.ACTION_RESULT;
import static net.aquadc.decouplex.Decouplex.ACTION_RESULT_BATCH;

/**
 * Created by miha on 24.05.16.
 * BroadcastReceiver to serve an Activity or Fragment
 */
public final class DecouplexReceiver extends BroadcastReceiver {

    private final Object resultHandler;
    private final String actionResult;
    private final String actionResultBatch;
    private final String actionError;
    private final IntentFilter filter;

    public DecouplexReceiver(Object resultHandler) {
        this.resultHandler = resultHandler;
        String id = "_" + resultHandler.getClass().getSimpleName();
        actionResult = ACTION_RESULT + id;
        actionResultBatch = ACTION_RESULT_BATCH + id;
        actionError = ACTION_ERR + id;
        filter = createFilter();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle resp = intent.getExtras();
        String action = intent.getAction();
        if (actionResult.equals(action)) {
            DeliveryStrategy strategy = DeliveryStrategy.valueOf(resp.getString("deliveryStrategy"));
            DecouplexResponse response = strategy.obtainResponse(resp.getParcelable("response"));
            response.dispatchResult(resultHandler);
        } else if (actionResultBatch.equals(action)) {
            DecouplexBatch
                    .find(resp.getInt("id"))
                    .dispatchResults(resultHandler, resp);
        } else if (actionError.equals(action)) {
            DeliveryStrategy strategy = DeliveryStrategy.valueOf(resp.getString("deliveryStrategy"));
            DecouplexResponse res = strategy.obtainResponse(resp.getParcelable("response"));
            res.request.decouplex.dispatchError(resultHandler, res);
        }
    }

    public void register() {
        Context con = getContext(resultHandler);
        LocalBroadcastManager.getInstance(con).registerReceiver(this, filter);
    }

    public void unregister() {
        Context con = getContext(resultHandler);
        LocalBroadcastManager.getInstance(con).unregisterReceiver(this);
    }

    private IntentFilter createFilter() {
        IntentFilter filter = new IntentFilter();
        String id = "_" + resultHandler.getClass().getSimpleName();
        filter.addAction(ACTION_RESULT + id);
        filter.addAction(ACTION_RESULT_BATCH + id);
        filter.addAction(ACTION_ERR + id);
        return filter;
    }

    @SuppressLint("NewApi")
    private static Context getContext(Object o) {
        if (o instanceof Context)
            return (Context) o;
        else if (o instanceof android.support.v4.app.Fragment)
            return ((android.support.v4.app.Fragment) o).getActivity();
        else if (o instanceof android.app.Fragment)
            return ((Fragment) o).getActivity();
        throw new IllegalStateException("can't get context from " + o);
    }

}
