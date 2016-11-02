package net.aquadc.decouplex;

import android.content.Context;
import android.support.annotation.NonNull;

import net.aquadc.decouplex.adapter.ErrorAdapter;
import net.aquadc.decouplex.adapter.ErrorHandler;
import net.aquadc.decouplex.adapter.ErrorProcessor;
import net.aquadc.decouplex.adapter.ResultProcessor;
import net.aquadc.decouplex.adapter.ResultAdapter;
import net.aquadc.decouplex.delivery.DeliveryStrategies;

import java.lang.reflect.Proxy;

/**
 * Created by miha on 14.05.16.
 *
 */
public final class DecouplexBuilder<FACE, HANDLER> {

    private final Class face;
    private final FACE impl;
    private final Class<HANDLER> handler;
    private int threads = 1;

    private ResultProcessor resultProcessor;
    private ResultAdapter resultAdapter;

    private ErrorProcessor errorProcessor;
    private ErrorAdapter errorAdapter;

    private ErrorHandler fallbackErrorHandler;

    public DecouplexBuilder(@NonNull Class<FACE> face, @NonNull FACE impl, @NonNull Class<HANDLER> handler) {
        // noinspection ConstantConditions
        if (face == null) {
            throw new NullPointerException("interface required, null given");
        }
        if (!face.isInterface()) {
            throw new IllegalArgumentException("interface required, class given");
        }
        // noinspection ConstantConditions
        if (impl == null) {
            throw new NullPointerException("interface implementation required, null given");
        }
        // noinspection ConstantConditions
        if (handler == null) {
            throw new NullPointerException("handler class required, null given");
        }
        this.face = face;
        this.impl = impl;
        this.handler = handler;
    }

    public DecouplexBuilder<FACE, HANDLER> resultProcessor(@NonNull ResultProcessor processor) {
        // noinspection ConstantConditions
        if (processor == null) {
            throw new NullPointerException("attempt to set null ResultProcessor");
        }
        this.resultProcessor = processor;
        return this;
    }

    public DecouplexBuilder<FACE, HANDLER> resultAdapter(@NonNull ResultAdapter adapter) {
        // noinspection ConstantConditions
        if (adapter == null) {
            throw new NullPointerException("attempt to set null ResultAdapter");
        }
        this.resultAdapter = adapter;
        return this;
    }

    public DecouplexBuilder<FACE, HANDLER> errorProcessor(@NonNull ErrorProcessor processor) {
        // noinspection ConstantConditions
        if (processor == null) {
            throw new NullPointerException("attempt to set null ErrorProcessor");
        }
        this.errorProcessor = processor;
        return this;
    }

    public DecouplexBuilder<FACE, HANDLER> errorAdapter(@NonNull ErrorAdapter adapter) {
        // noinspection ConstantConditions
        if (adapter == null) {
            throw new NullPointerException("attempt to set null ErrorAdapter");
        }
        this.errorAdapter = adapter;
        return this;
    }

    public DecouplexBuilder<FACE, HANDLER> threads(int threads) {
        if (threads < 1) {
            throw new IllegalArgumentException("positive thread pool required");
        }
        this.threads = threads;
        return this;
    }

    public DecouplexBuilder<FACE, HANDLER> fallbackErrorHandler(ErrorHandler handler) {
        // noinspection ConstantConditions
        if (handler == null) {
            throw new NullPointerException("attempt to set null ErrorHandler");
        }

        this.fallbackErrorHandler = handler;

        return this;
    }

    @SuppressWarnings("unchecked")
    public FACE create(@NonNull Context context) {
        // noinspection ConstantConditions
        if (context == null) {
            throw new NullPointerException("Null context given. How am I supposed to start service?");
        }
        final Class<FACE> face = this.face;
        return (FACE) Proxy.newProxyInstance(
                face.getClassLoader(), new Class[]{face},
                new DcxInvocationHandler<>(context.getApplicationContext(),
                        face, impl, handler, threads,
                        resultProcessor, resultAdapter,
                        errorProcessor, errorAdapter,
                        fallbackErrorHandler,
                        DeliveryStrategies.LOCAL)); // todo
    }

}
