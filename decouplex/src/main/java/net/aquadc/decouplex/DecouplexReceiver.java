package net.aquadc.decouplex;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.UiThread;
import android.support.v4.content.LocalBroadcastManager;

import net.aquadc.decouplex.adapter.ErrorAdapter;
import net.aquadc.decouplex.delivery.DeliveryStrategies;
import net.aquadc.decouplex.delivery.DeliveryStrategy;

import java.lang.reflect.Method;
import java.util.HashSet;

import static net.aquadc.decouplex.DcxInvocationHandler.ACTION_ERR;
import static net.aquadc.decouplex.DcxInvocationHandler.ACTION_RESULT;
import static net.aquadc.decouplex.DcxInvocationHandler.ACTION_RESULT_BATCH;
import static net.aquadc.decouplex.TypeUtils.arguments;

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
        String id = '_' + resultHandler.getClass().getSimpleName();
        actionResult = ACTION_RESULT + id;
        actionResultBatch = ACTION_RESULT_BATCH + id;
        actionError = ACTION_ERR + id;

        filter = new IntentFilter();
        filter.addAction(ACTION_RESULT + id);
        filter.addAction(ACTION_RESULT_BATCH + id);
        filter.addAction(ACTION_ERR + id);
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
            dispatchError(res.request.errorAdapter, res.request.face,
                    res.request.fallbackErrorHandler, res.request.handler,
                    resultHandler, res);
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

    /**
     * Acts as dispatchResult, but for errors.
     * @param resultHandler    object who is ready to handle result
     * @param resp             response
     */
    @UiThread
    static void dispatchError(ErrorAdapter errorAdapter, Class<?> face,
                              DcxInvocationHandler.ErrorHandler fallbackErrorHandler,
                              Class<?> handlerClass,
                              Object resultHandler, DcxResponse resp) {
        Throwable executionFail = (Throwable) resp.result;

        DcxRequest req = resp.request;
        HashSet<Object> args = new HashSet<>(2);
        args.add(req);
        args.add(executionFail);

        try {
            Method handler = HandlerSet.forMethod(req.methodName, false, handlerClass);

            handler.setAccessible(true);

            if (errorAdapter != null) {
                errorAdapter.adapt(face, req.methodName, handler, executionFail, resp, args);
            }

            handler.invoke(resultHandler, arguments(handler, args));
        } catch (Throwable deliveryFail) {
            if (fallbackErrorHandler == null) {
                if (deliveryFail instanceof RuntimeException) {
                    throw (RuntimeException) deliveryFail; // don't create a new one
                }
                throw new RuntimeException(deliveryFail);
            } else {
                fallbackErrorHandler.onError(req, executionFail);
            }
        }
    }

}
