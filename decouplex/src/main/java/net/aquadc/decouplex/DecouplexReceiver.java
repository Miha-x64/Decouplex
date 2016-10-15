package net.aquadc.decouplex;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import net.aquadc.decouplex.delivery.DeliveryStrategies;
import net.aquadc.decouplex.delivery.DeliveryStrategy;

import static net.aquadc.decouplex.DcxInvocationHandler.ACTION_ERR;
import static net.aquadc.decouplex.DcxInvocationHandler.ACTION_RESULT;
import static net.aquadc.decouplex.DcxInvocationHandler.ACTION_RESULT_BATCH;

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

    public DecouplexReceiver(Activity activity) {
        this((Object) activity);
    }

    public DecouplexReceiver(Fragment fragment) {
        this((Object) fragment);
    }

    public DecouplexReceiver(android.support.v4.app.Fragment fragment) {
        this((Object) fragment);
    }

    private DecouplexReceiver(Object resultHandler) {
        this.resultHandler = resultHandler;
        String id = '_' + resultHandler.getClass().getSimpleName();
        actionResult = ACTION_RESULT + id;
        actionResultBatch = ACTION_RESULT_BATCH + id;
        actionError = ACTION_ERR + id;

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_RESULT + id);
        filter.addAction(ACTION_RESULT_BATCH + id);
        filter.addAction(ACTION_ERR + id);
        this.filter = filter;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle resp = intent.getExtras();
        String action = intent.getAction();
        if (actionResult.equals(action)) {
            DeliveryStrategy strategy = DeliveryStrategies.forName(resp.getString("deliveryStrategy"));
            DcxResponse response = strategy.obtainResponse(resp.getParcelable("response"));
            response.dispatchResult(resultHandler);
        } else if (actionResultBatch.equals(action)) {
            DecouplexBatch
                    .find(resp.getInt("id"))
                    .dispatchResults(resultHandler, resp);
        } else if (actionError.equals(action)) {
            DeliveryStrategy strategy = DeliveryStrategies.forName(resp.getString("deliveryStrategy"));
            DcxResponse res = strategy.obtainResponse(resp.getParcelable("response"));
            res.dispatchError(res.request.errorAdapter, res.request.face,
                    res.request.fallbackErrorHandler, res.request.handler,
                    resultHandler);
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

    private static Context getContext(Object o) {
        if (o instanceof Context) {
            return (Context) o;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && o instanceof Fragment) {
            return ((Fragment) o).getActivity();
        }
        if (o instanceof android.support.v4.app.Fragment) {
            return ((android.support.v4.app.Fragment) o).getActivity();
        }
        throw new IllegalStateException("can't get context from " + o);
    }

}
