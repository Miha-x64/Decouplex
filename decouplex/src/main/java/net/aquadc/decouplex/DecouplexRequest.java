package net.aquadc.decouplex;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.Pair;

import net.aquadc.decouplex.adapter.ErrorProcessor;
import net.aquadc.decouplex.adapter.ResultProcessor;
import net.aquadc.decouplex.annotation.Debounce;
import net.aquadc.decouplex.delivery.DeliveryStrategy;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import static net.aquadc.decouplex.Decouplex.ACTION_RESULT;
import static net.aquadc.decouplex.Decouplex.ACTION_ERR;

/**
 * Created by miha on 20.08.16
 */
public final class DecouplexRequest {

    final Decouplex decouplex;
    final int threads;
    final Class<?> face;
    final Object impl;
    final String methodName;
    final String[] parameterTypes;
    final Object[] parameters;
    final String receiverActionSuffix;
    final DeliveryStrategy deliveryStrategy;
    final ResultProcessor resultProcessor;
    final ErrorProcessor errorProcessor;

    DecouplexRequest(
            Decouplex decouplex,
            int threads,
            Class face, Object impl, Method method, @Nullable Object[] args,
            String receiverActionSuffix,
            DeliveryStrategy deliveryStrategy,
            ResultProcessor resultProcessor, ErrorProcessor errorProcessor) {
        this.decouplex = decouplex;
        this.threads = threads;
        this.face = face;
        this.impl = impl;
        this.methodName = method.getName();
        this.parameterTypes = parameterTypesOf(method);
        this.parameters = args == null ? TypeUtils.EMPTY_ARRAY : args;
        this.receiverActionSuffix = receiverActionSuffix;
        this.deliveryStrategy = deliveryStrategy;
        this.resultProcessor = resultProcessor;
        this.errorProcessor = errorProcessor;
    }

    public void retry(Context context) {
        startExecService(context, prepare(null));
    }

    @Override
    public String toString() {
        StringBuilder bu = new StringBuilder("DecouplexRequest:");
        bu.append(methodName).append("(");
        for (Object param : parameters) {
            bu.append(param);
        }
        bu.append(")");
        return bu.toString();
    }

    Bundle prepare(@Nullable Debounce debounce) {
        Bundle data = new Bundle(debounce == null ? 2 : 3);

        if (debounce != null) {
            data.putInt("debounce", (int) debounce.unit().toMillis(debounce.value()));
        }

        data.putString("receiver", receiverActionSuffix);

        data.putParcelable("request", deliveryStrategy.transferRequest(this));
        data.putString("deliveryStrategy", deliveryStrategy.name());

        return data;
    }

    private static String[] parameterTypesOf(Method method) {
        Class[] classes = method.getParameterTypes();
        final int len = classes.length;
        String[] types = new String[len];
        for (int i = 0; i < len; i++) {
            types[i] = classes[i].getCanonicalName();
        }
        return types;
    }

    /*private static Bundle asBundle(Method method, Object[] args) {
        Bundle bun = new Bundle(args.length);
        TypeUtils.packParameters(bun, method.getParameterTypes(), args);
        return bun;
    }*/

    Class[] parameterTypes() {
        final int count = parameterTypes.length;
        String[] classes = parameterTypes;
        Class[] types = new Class[count];
        for (int i = 0; i < count; i++) {
            types[i] = TypeUtils.classForName(classes[i]);
        }
        return types;
    }

    /*Object[] parameters() {
        final int count = parameterTypes.length;
        Bundle bundled = this.parameters;
        Object[] parameters = new Object[count];
        for (int i = 0; i < count; i++) {
            parameters[i] = bundled.get(Integer.toString(i));
        }
        return parameters;
    }*/

    static void startExecService(Context context, Bundle extras) {
        Intent service = new Intent(context, DecouplexService.class); // TODO: different executors
        service.setAction(Decouplex.ACTION_EXEC);
        service.putExtras(extras);

        if (context.startService(service) == null) {
            throw new IllegalStateException("Did you forget to declare DecouplexService in your manifest?");
        }
    }

    @WorkerThread
    void executeAndBroadcast(Context con, String bReceiver) {
        Pair<Boolean, DecouplexResponse> result = execute();
        if (result.first) { // success
            broadcast(con, ACTION_RESULT + bReceiver, result.second);
        } else {
            broadcast(con, ACTION_ERR + bReceiver, result.second);
        }
    }

    static void broadcast(Context con, String action, DecouplexResponse result) {
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

    Pair<Boolean, DecouplexResponse> execute() {
        Method method;
        Object[] params = parameters;
        try {
            method = face.getDeclaredMethod(methodName, parameterTypes());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            DecouplexResponse resp = execute(method, params);
            return new Pair<>(true, resp);
        } catch (Throwable e) {
            DecouplexResponse resp = error(this, e, method, params);
            return new Pair<>(false, resp);
        }
    }

    DecouplexResponse execute(Method method, Object[] params) throws Exception {
        Object result = method.invoke(impl, params);

        if (resultProcessor != null) {
            result = resultProcessor.process(face, method, params, result);
        }

        return new DecouplexResponse(this, result);
    }

    DecouplexResponse error(DecouplexRequest req, Throwable e, Method method, Object[] params) {
        DecouplexResponse response = new DecouplexResponse(req, e);

        if (errorProcessor != null) {
            errorProcessor.process(face, method, params, e);
        }

        return response;
    }
}
