package net.aquadc.decouplex;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.Pair;

import net.aquadc.decouplex.adapter.ErrorAdapter;
import net.aquadc.decouplex.adapter.ErrorProcessor;
import net.aquadc.decouplex.adapter.ResultAdapter;
import net.aquadc.decouplex.adapter.ResultProcessor;
import net.aquadc.decouplex.annotation.DcxDelivery;
import net.aquadc.decouplex.annotation.Debounce;
import net.aquadc.decouplex.delivery.DeliveryStrategy;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import static net.aquadc.decouplex.DcxInvocationHandler.ACTION_RESULT;
import static net.aquadc.decouplex.DcxInvocationHandler.ACTION_ERR;

/**
 * Created by miha on 20.08.16
 */
public final class DcxRequest {

    final int threads;
    final Class<?> face;
    private final Object impl;
    final String methodName;
    private final Class<?>[] parameterTypes;
    private final Object[] parameters;
    private final String receiverActionSuffix;
    final DeliveryStrategy deliveryStrategy;
    private final ResultProcessor resultProcessor;
    final ResultAdapter resultAdapter;
    private final ErrorProcessor errorProcessor;
    final ErrorAdapter errorAdapter;
    final DcxInvocationHandler.ErrorHandler fallbackErrorHandler;
    final Class<?> handler;

    DcxRequest(
            int threads,
            Class face, Object impl, Method method, @Nullable Object[] args,
            String receiverActionSuffix,
            DeliveryStrategy deliveryStrategy,
            ResultProcessor resultProcessor, ResultAdapter resultAdapter,
            ErrorProcessor errorProcessor, ErrorAdapter errorAdapter,
            DcxInvocationHandler.ErrorHandler fallbackErrorHandler,
            Class<?> handler) {
        this.threads = threads;
        this.face = face;
        this.impl = impl;
        this.methodName = method.getName();
        this.parameterTypes = method.getParameterTypes();
        this.parameters = args == null ? TypeUtils.EMPTY_ARRAY : args;
        this.receiverActionSuffix = receiverActionSuffix;
        this.deliveryStrategy = deliveryStrategy;
        this.resultProcessor = resultProcessor;
        this.resultAdapter = resultAdapter;
        this.errorProcessor = errorProcessor;
        this.errorAdapter = errorAdapter;
        this.fallbackErrorHandler = fallbackErrorHandler;
        this.handler = handler;
    }

    public void retry(Context context) {
        startExecService(context, prepare(null));
    }

    @Override
    public String toString() {
        StringBuilder bu = new StringBuilder("DecouplexRequest:");
        bu.append(methodName).append('(');
        for (Object param : parameters) {
            bu.append(param).append(", ");
        }
        bu.append(')');
        return bu.toString();
    }

    Bundle prepare(@Nullable Debounce debounce) {
        boolean debounced = debounce != null && debounce.value() > 0;
        Bundle data = new Bundle(debounced ? 3 : 2);

        if (debounced) {
            data.putInt("debounce", (int) debounce.unit().toMillis(debounce.value()));
        }

        data.putString("receiver", receiverActionSuffix);

        data.putParcelable("request", deliveryStrategy.transferRequest(this));
        data.putString("deliveryStrategy", deliveryStrategy.name());

        return data;
    }

    static void startExecService(Context context, Bundle extras) {
        Intent service = new Intent(context, DecouplexService.class);
        service.setAction(DcxInvocationHandler.ACTION_EXEC);
        service.putExtras(extras);

        if (context.startService(service) == null) {
            throw new IllegalStateException("Did you forget to declare " + DecouplexService.class.getSimpleName() + " in your manifest?");
        }
    }

    @WorkerThread
    void executeAndBroadcast(Context con, String bReceiver) {
        Pair<Boolean, DcxResponse> result = execute();
        if (result.second != null) {
            if (result.first) { // success
                broadcast(con, ACTION_RESULT + bReceiver, result.second);
            } else { // error
                broadcast(con, ACTION_ERR + bReceiver, result.second);
            }
        }
    }

    static void broadcast(Context con, String action, DcxResponse result) {
        DeliveryStrategy strategy = result.request.deliveryStrategy;
        Bundle bun = new Bundle();
        bun.putParcelable("response", strategy.transferResponse(result));
        bun.putString("deliveryStrategy", strategy.name());

        broadcast(con, action, bun);
    }

    static void broadcast(Context con, String action, Bundle extras) {
        Intent i = new Intent(action);
        i.putExtras(extras);

        LocalBroadcastManager man = LocalBroadcastManager.getInstance(con);
        int attempts = 0;
        while (!man.sendBroadcast(i)) {
            try {
                TimeUnit.MILLISECONDS.sleep(500);
            } catch (InterruptedException e) {
                // ok
            }
            attempts++;
            if (attempts == 5) {
                Log.e("Decouplex", "Intent has not been delivered: " + i);
                break;
            }
        }
    }

    /**
     * Executes this request.
     *
     * @return a tuple of {@code Boolean} success and {@code DcxResponse?} response.
     * Response will be null if its delivery is not required.
     */
    Pair<Boolean, DcxResponse> execute() {
        Method method;
        Object[] params = parameters;
        final Class<?> face = this.face;
        try {
            method = face.getDeclaredMethod(methodName, parameterTypes);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        DcxDelivery delivery = method.getAnnotation(DcxDelivery.class);
        try {
            Object result = method.invoke(impl, params);
            if (resultProcessor != null) {
                result = resultProcessor.process(face, method, params, result);
            }
            return new Pair<>(true,
                    delivery == null || delivery.deliverResult()
                            ? new DcxResponse(this, result)
                            : null);
        } catch (Throwable e) {
            if (errorProcessor != null) {
                errorProcessor.process(face, method, params, e);
            }
            return new Pair<>(false,
                    delivery == null || delivery.deliverError()
                            ? new DcxResponse(this, e)
                            : null);
        }
    }
}
