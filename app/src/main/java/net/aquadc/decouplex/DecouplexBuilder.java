package net.aquadc.decouplex;

import android.content.Context;
import android.support.annotation.NonNull;

import net.aquadc.decouplex.adapter.ErrorAdapter;
import net.aquadc.decouplex.adapter.ErrorProcessor;
import net.aquadc.decouplex.adapter.ResultProcessor;
import net.aquadc.decouplex.adapter.ResultAdapter;
import net.aquadc.decouplex.adapter.Retrofit2ErrorAdapter;
import net.aquadc.decouplex.adapter.Retrofit2ResultProcessor;
import net.aquadc.decouplex.adapter.Retrofit2ResultAdapter;

import java.lang.reflect.Proxy;

/**
 * Created by miha on 14.05.16.
 *
 */
public class DecouplexBuilder<FACE, HANDLER> {

    private Class face;
    private FACE impl;
    private Class<HANDLER> handler;

    private ResultProcessor resultProcessor;
    private ResultAdapter resultAdapter;

    private ErrorProcessor errorProcessor;
    private ErrorAdapter errorAdapter;

    public DecouplexBuilder(@NonNull Class<FACE> face, @NonNull FACE impl, @NonNull Class<HANDLER> handler) {
        // noinspection ConstantConditions
        if (face == null)
            throw new NullPointerException("interface required, null given");
        if (!face.isInterface())
            throw new IllegalArgumentException("interface required");
        // noinspection ConstantConditions
        if (impl == null) {
            throw new NullPointerException("interface implementation required, null given");
        }
        // noinspection ConstantConditions
        if (handler == null) {
            throw new NullPointerException("handler class require, null given");
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

    @SuppressWarnings("unchecked")
    public FACE create(@NonNull Context context) {
        // noinspection ConstantConditions
        if (context == null) {
            throw new NullPointerException("Null context given. How am I supposed to start service?");
        }
        if (impl == null) {
            throw new NullPointerException("implementation is required");
        }

        return (FACE) Proxy.newProxyInstance(
                face.getClassLoader(), new Class[]{face},
                new Decouplex<>(context.getApplicationContext(),
                        face, impl, handler,
                        resultProcessor, resultAdapter,
                        errorProcessor, errorAdapter));
    }

    public static <FACE, HANDLER> FACE retrofit2(Context context, Class<FACE> face, FACE impl, Class<HANDLER> handler) {
        return new DecouplexBuilder<>(face, impl, handler)
                .resultProcessor(new Retrofit2ResultProcessor())
                .resultAdapter(new Retrofit2ResultAdapter())
                .errorAdapter(new Retrofit2ErrorAdapter())
                .create(context);
    }

}
