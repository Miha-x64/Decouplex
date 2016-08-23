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
import net.aquadc.decouplex.annotation.Debounce;

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

    private final ErrorHandler fallbackErrorHandler;

    private final Class<HANDLER> handler;
    private HandlerSet handlers;

    Decouplex(Context context,
              Class<FACE> face, FACE impl, Class<HANDLER> handler, int threads,
              ResultProcessor resultProcessor, ResultAdapter resultAdapter,
              ErrorProcessor errorProcessor, ErrorAdapter errorAdapter,
              ErrorHandler fallbackErrorHandler) {
        this.context = context;

        this.face = face;
        this.impl = impl;
        this.handler = handler;
        this.threads = threads;

        this.resultProcessor = resultProcessor;
        this.resultAdapter = resultAdapter;

        this.errorProcessor = errorProcessor;
        this.errorAdapter = errorAdapter;

        this.fallbackErrorHandler = fallbackErrorHandler;

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
        DecouplexRequest.startExecService(context, prepareRequest(method, args));

        if (method.getReturnType().isPrimitive())
            return 0;
        return null;
    }

    Bundle prepareRequest(Method method, Object[] args) {
        DecouplexRequest request = new DecouplexRequest(id, method, args, "_" + handler.getSimpleName());

        return request.prepare(method.getAnnotation(Debounce.class));
    }

    /**
     * execute the requested action on the background
     * @param con       context
     * @param request   request
     * @param bReceiver broadcastReceiver action suffix
     */
    @WorkerThread
    void executeAndBroadcast(Context con, DecouplexRequest request, String bReceiver) {
        Pair<Boolean, Bundle> result = execute(request);
        if (result.first) { // success
            broadcast(con, ACTION_RESULT + bReceiver, result.second);
        } else {
            broadcast(con, ACTION_ERR + bReceiver, result.second);
        }
    }

    Pair<Boolean, Bundle> execute(DecouplexRequest req) {
        String methodName = req.methodName;

        Class<?>[] types = req.parameterTypes();
        Method method;
        Object[] params = req.parameters();
        try {
            method = face.getDeclaredMethod(methodName, types);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            Bundle resp = execute(methodName, method, params);
            return new Pair<>(true, resp);
        } catch (Throwable e) {
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

    Bundle error(DecouplexRequest req, Throwable e, Method method, Object[] params) {
        Bundle resp = new Bundle();
        resp.putParcelable("request", req);
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
            Method handler = handler(method, handlers.classifiedResultHandlers, handlers.resultHandlers, this.handler);
            handler.setAccessible(true); // protected methods are inaccessible by default O_o

            HashSet<Object> args = new HashSet<>();
            args.add(bun.get("result"));

            if (resultAdapter != null) {
                resultAdapter.adapt(face, method, handler, bun, args);
            }

            handler.invoke(resultHandler, arguments(handler, args));
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
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
    void dispatchError(Object resultHandler, DecouplexRequest req, Bundle resp) {
        Throwable executionFail = (Throwable) resp.get("exception");

        HashSet<Object> args = new HashSet<>();
        args.add(req);
        args.add(executionFail);

        try {
            if (handlers == null) {
                findHandlers();
            }
            Method handler = handler(req.methodName, handlers.classifiedErrorHandlers, handlers.errorHandlers);

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

    @FunctionalInterface
    public interface ErrorHandler {
        void onError(DecouplexRequest request, Throwable throwable);
    }
}
