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
public final class DecouplexReceiver extends BroadcastReceiver {

    public static IntentFilter createFilter(Object receiver) {
        IntentFilter filter = new IntentFilter();
        String id = "_" + receiver.getClass().getSimpleName();
        filter.addAction(ACTION_RESULT + id);
        filter.addAction(ACTION_RESULT_BATCH + id);
        filter.addAction(ACTION_ERR + id);
        return filter;
    }

    private final Object resultHandler;
    private final String actionResult;
    private final String actionResultBatch;
    private final String actionError;

    public DecouplexReceiver(Object resultHandler) {
        this.resultHandler = resultHandler;
        String id = "_" + resultHandler.getClass().getSimpleName();
        actionResult = ACTION_RESULT + id;
        actionResultBatch = ACTION_RESULT_BATCH + id;
        actionError= ACTION_ERR + id;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle resp = intent.getExtras();
        String action = intent.getAction();
        if (actionResult.equals(action)) {
            Decouplex
                    .find(resp.getInt("id"))
                    .dispatchResult(resultHandler, resp);
        } else if (actionResultBatch.equals(action)) {
            DecouplexBatch
                    .find(resp.getInt("id"))
                    .dispatchResults(resultHandler, resp);
        } else if (actionError.equals(action)) {
            DecouplexRequest req = resp.getParcelable("request");
            Decouplex decouplex = Decouplex.find(req.decouplexId);
            decouplex.dispatchError(resultHandler, req, resp);
        }
    }

}
