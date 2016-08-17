package net.aquadc.decouplex;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import net.aquadc.decouplex.adapter.ErrorAdapter;
import net.aquadc.decouplex.adapter.ErrorProcessor;
import net.aquadc.decouplex.adapter.ResultAdapter;
import net.aquadc.decouplex.adapter.ResultProcessor;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static net.aquadc.decouplex.TypeUtils.*;
import static net.aquadc.decouplex.Converter.put;

/**
 * Created by miha on 14.05.16.
 *
 */
final class Decouplex<FACE, HANDLER> implements InvocationHandler {

    /**
     * Actions to use in Intents
     */
    public static final String ACTION_EXEC = "DECOUPLEX_EXEC";
    public static final String ACTION_EXEC_BATCH = "DECOUPLEX_EXEC_BATCH";
    public static final String ACTION_RESULT = "DECOUPLEX_RESULT";
    public static final String ACTION_RESULT_BATCH = "DECOUPLEX_RESULT_BATCH";
    public static final String ACTION_ERR = "DECOUPLEX_ERR";

    /**
     * Instances management
     */
    private static final Map<Integer, WeakReference<Decouplex>> instances = new ConcurrentHashMap<>();
    private static AtomicInteger instancesCount = new AtomicInteger();

    @SuppressWarnings("unchecked")
    static <T, H> Decouplex<T, H> find(int id) {
        return instances.get(id).get();
    }

    /**
     * instance
     */
    private final int id;

    private final Context context;

    final Class<FACE> face; // access needed by Batch
    private final FACE impl;
    final int threads;

    private final ResultProcessor resultProcessor;
    private final ResultAdapter resultAdapter;
    private final ErrorProcessor errorProcessor;
    private final ErrorAdapter errorAdapter;

    private final Class<HANDLER> handler;
    private HandlerSet handlers;

    Decouplex(Context context,
              Class<FACE> face, FACE impl, Class<HANDLER> handler, int threads,
              ResultProcessor resultProcessor, ResultAdapter resultAdapter,
              ErrorProcessor errorProcessor, ErrorAdapter errorAdapter) {
        this.context = context;

        this.face = face;
        this.impl = impl;
        this.handler = handler;
        this.threads = threads;

        this.resultProcessor = resultProcessor;
        this.resultAdapter = resultAdapter;

        this.errorProcessor = errorProcessor;
        this.errorAdapter = errorAdapter;

        id = instancesCount.incrementAndGet();
        instances.put(id, new WeakReference<>(this));
    }

    /**
     * Invocation handler
     * @param proxy     not used
     * @param method    invoked method
     * @param args      passed arguments
     * @return zero (as primitive) or null pointer (as object)
     * @throws Throwable throws nothing by itself
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        Bundle data = prepareRequest(method, args);

        Intent service = new Intent(context, DecouplexService.class); // TODO: different executors
        service.setAction(ACTION_EXEC);
        service.putExtras(data);

        context.startService(service);

        if (method.getReturnType().isPrimitive())
            return 0;
        return null;
    }

    Bundle prepareRequest(Method method, Object[] args) {
        Bundle data = new Bundle();

        data.putInt("id", id);
        data.putString("method", method.getName());

        Class[] types = method.getParameterTypes();
        packTypes(data, types);
        packParameters(data, types, args);

        return data;
    }

    /**
     * execute the requested action on the background
     * @param con    context
     * @param req    request bundle
     */
    @WorkerThread
    void executeAndBroadcast(Context con, Bundle req) {
        Pair<Boolean, Bundle> result = execute(req);
        if (result.first) { // success
            broadcast(con, ACTION_RESULT, result.second);
        } else {
            broadcast(con, ACTION_ERR, result.second);
        }
    }

    Pair<Boolean, Bundle> execute(Bundle req) {
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
            Bundle resp = execute(methodName, method, params);
            return new Pair<>(true, resp);
        } catch (Exception e) {
            Bundle resp = error(req, e, method, params);
            return new Pair<>(false, resp);
        }
    }

    Bundle execute(String methodName, Method method, Object[] params) throws Exception {
        Object result = method.invoke(impl, params);

        Bundle resp = new Bundle();
        resp.putInt("id", id);
        resp.putString("method", methodName);

        if (resultProcessor == null) {
            put(resp, "result", method.getReturnType(), result);
        } else {
            resultProcessor.process(resp, face, method, params, result);
        }

        return resp;
    }

    Bundle error(Bundle req, Exception e, Method method, Object[] params) {
        Bundle resp = new Bundle();
        resp.putBundle("request", req);
        put(resp, "exception", null, e);

        if (errorProcessor != null) {
            errorProcessor.process(resp, face, method, params, e);
        }
        return resp;
    }

    /**
     * broadcast a result or an error from service
     * @param con       context
     * @param action    ACTION_RESULT or ACTION_ERR
     * @param extras    extras to broadcast
     */
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
     * Dispatch method invocation result to an appropriate method.
     * First, searches in methods, targeted for a certain class;
     * non-targeted methods then.
     * Searches concrete methods first, wildcard-containing methods then and
     * "*"-methods finally.
     * @param resultHandler object whose class will be analyzed & on which the method will be invoked
     * @param bun           result
     */
    @UiThread
    void dispatchResult(Object resultHandler, Bundle bun) {
        String method = bun.getString("method");

        try {
            if (handlers == null) {
                findHandlers();
            }
            Method handler = handler(method, handlers.classifiedResultHandlers, handlers.resultHandlers);
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


    /**
     * Acts as the previous one, but for errors.
     * @param resultHandler    object who is ready to handle result
     * @param req              request bundle
     * @param resp             response bundle
     */
    @UiThread
    void dispatchError(Object resultHandler, Bundle req, Bundle resp) {
        Throwable e = (Throwable) resp.get("exception");

        String methodName = req.getString("method");

        HashSet<Object> args = new HashSet<>();
//        args.add(resp);
        args.add(e);

        try {
            if (handlers == null) {
                findHandlers();
            }
            Method handler = handler(methodName, handlers.classifiedErrorHandlers, handlers.errorHandlers);

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
        handlers = Handlers.forClass(handler);
    }

    @Override
    public int hashCode() {
        return id;
    }

    /**
     * Dispatch results of a batch invocation
     * @param resultHandler    an object that will receive a result
     * @param results          a bundle with invocation results
     */
    @UiThread
    static void dispatchResults(Object resultHandler, Bundle results, HandlerSet handlers) {
        List<String> methods = new ArrayList<>();
        List<Set<Object>> resultSets = new ArrayList<>();
        int i = 0;
        while (true) {
            Bundle result = results.getBundle(Integer.toString(i));
            if (result == null)
                break;

            String method = result.getString("method");
            methods.add(method);

            Decouplex dec = find(result.getInt("id"));

            HashSet<Object> args = new HashSet<>();
            args.add(result.get("result"));
            if (dec.resultAdapter != null) {
                dec.resultAdapter.adapt(dec.face, method, null, result, args);
            }
            resultSets.add(args);

            i++;
        }

        String method = TextUtils.join(", ", methods);

        Method handler = handler(method, handlers.classifiedResultHandlers, handlers.resultHandlers);
        handler.setAccessible(true); // protected methods are inaccessible by default O_o

        try {
            handler.invoke(resultHandler, arguments(handler.getParameterTypes(), resultSets));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}