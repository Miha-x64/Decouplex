package net.aquadc.decouplex;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.UiThread;
import android.text.TextUtils;

import net.aquadc.decouplex.adapter.ErrorAdapter;
import net.aquadc.decouplex.adapter.ErrorProcessor;
import net.aquadc.decouplex.adapter.ResultAdapter;
import net.aquadc.decouplex.adapter.ResultProcessor;
import net.aquadc.decouplex.annotation.Debounce;
import net.aquadc.decouplex.delivery.DeliveryStrategy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static net.aquadc.decouplex.TypeUtils.*;

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
    private static AtomicInteger instancesCount = new AtomicInteger();

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

    private final DeliveryStrategy deliveryStrategy;

    private final Class<HANDLER> handler;
    private HandlerSet handlers;

    Decouplex(Context context,
              Class<FACE> face, FACE impl, Class<HANDLER> handler, int threads,
              ResultProcessor resultProcessor, ResultAdapter resultAdapter,
              ErrorProcessor errorProcessor, ErrorAdapter errorAdapter,
              ErrorHandler fallbackErrorHandler,
              DeliveryStrategy deliveryStrategy) {
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

        this.deliveryStrategy = deliveryStrategy;

        id = instancesCount.incrementAndGet();
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
        DecouplexRequest request =
                new DecouplexRequest(this, threads,
                        face, impl, method, args,
                        "_" + handler.getSimpleName(),
                        deliveryStrategy,
                        resultProcessor, errorProcessor);

        return request.prepare(method.getAnnotation(Debounce.class));
    }

    /**
     * Dispatch method invocation result to an appropriate method.
     * First, searches in methods, targeted for a certain class;
     * non-targeted methods then.
     * Searches concrete methods first, wildcard-containing methods then and
     * "*"-methods finally.
     * @param resultHandler object whose class will be analyzed & on which the method will be invoked
     * @param response      result
     */
    @UiThread
    void dispatchResult(Object resultHandler, DecouplexResponse response) {
        String method = response.request.methodName;

        try {
            if (handlers == null) {
                findHandlers();
            }
            Method handler = handler(method, handlers.classifiedResultHandlers, handlers.resultHandlers, this.handler);
            handler.setAccessible(true); // protected methods are inaccessible by default O_o

            HashSet<Object> args = new HashSet<>();

            if (resultAdapter == null) {
                args.add(response.result);
            } else {
                resultAdapter.adapt(face, method, handler, response.result, args);
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
     * @param resp             response
     */
    @UiThread
    void dispatchError(Object resultHandler, DecouplexResponse resp) {
        Throwable executionFail = (Throwable) resp.result;

        DecouplexRequest req = resp.request;
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
            String n = Integer.toString(i);
            String strategyName = results.getString("strategy" + n);
            if (strategyName == null)
                break;
            DeliveryStrategy strategy = DeliveryStrategy.valueOf(strategyName);
            DecouplexResponse response = strategy.obtainResponse(results.getParcelable(n));

            String method = response.request.methodName;
            methods.add(method);

            Decouplex dec = response.request.decouplex;

            HashSet<Object> args = new HashSet<>();
            args.add(response.result);
            if (dec.resultAdapter != null) {
                dec.resultAdapter.adapt(dec.face, method, null, response.result, args);
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
