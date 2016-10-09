package net.aquadc.decouplex;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.UiThread;

import net.aquadc.decouplex.adapter.ErrorAdapter;
import net.aquadc.decouplex.adapter.ErrorProcessor;
import net.aquadc.decouplex.adapter.ResultAdapter;
import net.aquadc.decouplex.adapter.ResultProcessor;
import net.aquadc.decouplex.annotation.Debounce;
import net.aquadc.decouplex.delivery.DeliveryStrategy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;

import static net.aquadc.decouplex.TypeUtils.*;

/**
 * Created by miha on 14.05.16.
 *
 */
final class DcxInvocationHandler<FACE, HANDLER> implements InvocationHandler {

    /**
     * Actions to use in Intents
     */
    static final String ACTION_EXEC = "DECOUPLEX_EXEC";
    static final String ACTION_EXEC_BATCH = "DECOUPLEX_EXEC_BATCH";
    static final String ACTION_RESULT = "DECOUPLEX_RESULT";
    static final String ACTION_RESULT_BATCH = "DECOUPLEX_RESULT_BATCH";
    static final String ACTION_ERR = "DECOUPLEX_ERR";

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
    private final int threads;

    private final ResultProcessor resultProcessor;
    private final ResultAdapter resultAdapter;
    private final ErrorProcessor errorProcessor;
    private final ErrorAdapter errorAdapter;

    private final ErrorHandler fallbackErrorHandler;

    private final DeliveryStrategy deliveryStrategy;

    private final Class<HANDLER> handler;
    private HandlerSet handlers; // just a strong reference

    DcxInvocationHandler(Context context,
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
        this.handlers = Handlers.forClass(face, handler);

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
        DcxRequest.startExecService(context, prepareRequest(method, args));

        if (method.getReturnType().isPrimitive())
            return 0;
        return null;
    }

    Bundle prepareRequest(Method method, Object[] args) {
        DcxRequest request =
                new DcxRequest(threads,
                        face, impl, method, args,
                        '_' + handler.getSimpleName(),
                        deliveryStrategy,
                        resultProcessor, resultAdapter,
                        errorProcessor, errorAdapter,
                        fallbackErrorHandler,
                        handler);

        return request.prepare(method.getAnnotation(Debounce.class));
    }

    @Override
    public int hashCode() {
        return id;
    }

    @SuppressWarnings("Since15")
    @FunctionalInterface
    public interface ErrorHandler {
        void onError(DcxRequest request, Throwable throwable);
    }
}
