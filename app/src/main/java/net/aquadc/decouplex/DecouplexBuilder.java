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

import static net.aquadc.decouplex.Decouplex.*;

/**
 * Created by miha on 14.05.16.
 *
 */
public class DecouplexBuilder<FACE> {

    private static final Object lock = new Object();

    private static int implCount;
    private static int resultProcessorCount;
    private static int resultAdapterCount;
    private static int errorAdapterCount;
    private static int errorProcessorCount;

    private Class face;
    private FACE impl;

    private ResultProcessor resultProcessor;
    private ResultAdapter resultAdapter;

    private ErrorProcessor errorProcessor;
    private ErrorAdapter errorAdapter;

    public DecouplexBuilder(@NonNull Class<FACE> face) {
        // noinspection ConstantConditions
        if (face == null)
            throw new NullPointerException("interface required, null given");
        if (!face.isInterface())
            throw new IllegalArgumentException("interface required");
        this.face = face;
    }

    public DecouplexBuilder<FACE> impl(@NonNull FACE impl) {
        // noinspection ConstantConditions
        if (impl == null) {
            throw new NullPointerException("interface implementation required, null given");
        }
        this.impl = impl;
        return this;
    }

    public DecouplexBuilder<FACE> resultProcessor(@NonNull ResultProcessor processor) {
        // noinspection ConstantConditions
        if (processor == null) {
            throw new NullPointerException("attempt to set null ResultProcessor");
        }
        this.resultProcessor = processor;
        return this;
    }

    public DecouplexBuilder<FACE> resultAdapter(@NonNull ResultAdapter adapter) {
        // noinspection ConstantConditions
        if (adapter == null) {
            throw new NullPointerException("attempt to set null ResultAdapter");
        }
        this.resultAdapter = adapter;
        return this;
    }

    public DecouplexBuilder<FACE> errorProcessor(@NonNull ErrorProcessor processor) {
        // noinspection ConstantConditions
        if (processor == null) {
            throw new NullPointerException("attempt to set null ErrorProcessor");
        }
        this.errorProcessor = processor;
        return this;
    }

    public DecouplexBuilder<FACE> errorAdapter(@NonNull ErrorAdapter adapter) {
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
        synchronized (lock) {
            implCount++;
            implementations.put(implCount, impl);

            if (resultProcessor != null) {
                resultProcessorCount++;
                resultProcessors.put(resultProcessorCount, resultProcessor);
            }
            if (resultAdapter != null) {
                resultAdapterCount++;
                resultAdapters.put(resultAdapterCount, resultAdapter);
            }
            if (errorProcessor != null) {
                errorProcessorCount++;
                errorProcessors.put(errorProcessorCount, errorProcessor);
            }
            if (errorAdapter != null) {
                errorAdapterCount++;
                errorAdapters.put(errorAdapterCount, errorAdapter);
            }
            return (FACE) Proxy.newProxyInstance(
                    face.getClassLoader(), new Class[]{face},
                    new DecouplexRequestHandler<>(context.getApplicationContext(),
                            face, implCount,
                            resultProcessor == null ? 0 : resultProcessorCount,
                            resultAdapter == null   ? 0 : resultAdapterCount,
                            errorProcessor == null  ? 0 : errorProcessorCount,
                            errorAdapter == null    ? 0 : resultAdapterCount));
        }
    }

    public static <FACE> FACE retrofit2(Context context, Class<FACE> face, FACE impl) {
        return new DecouplexBuilder<>(face)
                .impl(impl)
                .resultProcessor(new Retrofit2ResultProcessor())
                .resultAdapter(new Retrofit2ResultAdapter())
                .errorAdapter(new Retrofit2ErrorAdapter())
                .create(context);
    }

}
