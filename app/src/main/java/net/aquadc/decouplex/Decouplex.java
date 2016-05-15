package net.aquadc.decouplex;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.Pair;

import net.aquadc.decouplex.adapter.ErrorAdapter;
import net.aquadc.decouplex.adapter.ErrorProcessor;
import net.aquadc.decouplex.adapter.ResultAdapter;
import net.aquadc.decouplex.adapter.ResultProcessor;
import net.aquadc.decouplex.android.DecouplexService;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static net.aquadc.decouplex.TypeUtils.*;
import static net.aquadc.decouplex.Converter.put;

/**
 * Created by miha on 14.05.16.
 *
 */
public class Decouplex<FACE, HANDLER> implements InvocationHandler {

    /**
     * Actions to use in Intents
     */
    public static final String ACTION_EXEC = "DECOUPLEX_EXEC";
    public static final String ACTION_RESULT = "DECOUPLEX_RESULT";
    public static final String ACTION_ERR = "DECOUPLEX_ERR";

    /**
     * Instances management
     */
    private static final
        ConcurrentHashMap<Integer, WeakReference<Decouplex>> instances = new ConcurrentHashMap<>();
    private static final Object counterLock = new Object();
    private static int requestSenderCount;

    @SuppressWarnings("unchecked")
    public static <T, H> Decouplex<T, H> find(int id) {
        return instances.get(id).get();
    }

    /**
     * instance
     */
    private final int id;

    private final Context context;

    private final Class<FACE> face;
    private final FACE impl;

    private final ResultProcessor resultProcessor;
    private final ResultAdapter resultAdapter;
    private final ErrorProcessor errorProcessor;
    private final ErrorAdapter errorAdapter;

    private final Class<HANDLER> handler;
    private Pair<Handlers, Handlers> resultHandlers;
    private Pair<Handlers, Handlers> errorHandlers;

    Decouplex(Context context,
              Class<FACE> face, FACE impl, Class<HANDLER> handler,
              ResultProcessor resultProcessor, ResultAdapter resultAdapter,
              ErrorProcessor errorProcessor, ErrorAdapter errorAdapter) {
        this.context = context;

        this.face = face;
        this.impl = impl;
        this.handler = handler;

        this.resultProcessor = resultProcessor;
        this.resultAdapter = resultAdapter;

        this.errorProcessor = errorProcessor;
        this.errorAdapter = errorAdapter;

        synchronized (counterLock) {
            requestSenderCount++;
            id = requestSenderCount;
        }
        instances.put(id, new WeakReference<>(this));
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        Bundle data = new Bundle();

        data.putInt("id", id);
        data.putString("method", method.getName());

        Class[] types = method.getParameterTypes();
        packTypes(data, types);
        packParameters(data, types, args);

        Intent service = new Intent(context, DecouplexService.class);
        service.setAction(ACTION_EXEC);
        service.putExtras(data);

        context.startService(service);

        if (method.getReturnType().isPrimitive())
            return 0;
        return null;
    }

    public void execute(Context con, Bundle req) {
        String methodName = req.getString("method");

        Class<?>[] types;
        Method method;
        Object[] params;
        try {
            types = unpackTypes(req);
            method = face.getDeclaredMethod(methodName, types);
            params = unpackParameters(req, types.length);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            Object result = method.invoke(impl, params);

            Bundle resp = new Bundle();
            resp.putInt("id", id);
            resp.putString("method", methodName);

            if (resultProcessor == null) {
                put(resp, "result", method.getReturnType(), result);
            } else {
                resultProcessor.process(resp, face, method, params, result);
            }

            broadcast(con, ACTION_RESULT, resp);
        } catch (Exception e) {
            Bundle resp = new Bundle();
            resp.putBundle("request", req);
            put(resp, "exception", null, e);

            if (errorProcessor!= null) {
                errorProcessor.process(resp, face, method, params, e);
            }

            broadcast(con, ACTION_ERR, resp);
        }
    }

    private static void broadcast(Context con, String action, Bundle extras) {
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

    public void dispatchResult(Object resultHandler, Bundle bun) {
        String method = bun.getString("method");

        try {
            if (resultHandlers == null) {
                findHandlers();
            }
            Method handler = handler(method, resultHandlers);
            handler.setAccessible(true); // protected methods are inaccessible by default O_o

            HashSet<Object> args = new HashSet<>();
            args.add(bun.get("result"));

            if (resultAdapter != null) {
                resultAdapter.adapt(face, method, handler, bun, args);
            }

            handler.invoke(resultHandler, arguments(handler.getParameterTypes(), args));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void dispatchError(Object resultHandler, Bundle req, Bundle resp) {
        Throwable e = (Throwable) resp.get("exception");

        String methodName = req.getString("method");

        HashSet<Object> args = new HashSet<>();
//        args.add(resp);
        args.add(e);

        try {
            if (resultHandlers == null) {
                findHandlers();
            }
            Method handler = handler(methodName, errorHandlers);

            handler.setAccessible(true);

            if (errorAdapter != null) {
                errorAdapter.adapt(face, methodName, handler, e, resp, args);
            }

            handler.invoke(resultHandler, arguments(handler.getParameterTypes(), args));
        } catch (Exception f) {
            throw new RuntimeException(f);
        }
    }

    private void findHandlers() {
        Handlers[] h = Handlers.analyze(handler);
        resultHandlers = new Pair<>(h[0], h[1]);
        errorHandlers = new Pair<>(h[2], h[3]);
    }
}
