package net.aquadc.decouplex;

import android.content.Context;
import android.support.annotation.NonNull;

import net.aquadc.decouplex.adapter.PostProcessor;
import net.aquadc.decouplex.adapter.ResultAdapter;
import net.aquadc.decouplex.adapter.Retrofit2PostProcessor;
import net.aquadc.decouplex.adapter.Retrofit2ResultAdapter;

import java.lang.reflect.Proxy;

import static net.aquadc.decouplex.Decouplex.*;

/**
 * Created by miha on 14.05.16.
 *
 */
public class DecouplexBuilder<FACE> {

    private static int implCounter;
    private static int postProcessorCount;
    private static int resultAdapterCount;
    private static final Object lock = new Object();

    private Class face;
    private FACE impl;

    private PostProcessor postProcessor;
    private ResultAdapter resultAdapter;

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

    public DecouplexBuilder<FACE> postProcessor(@NonNull PostProcessor processor) {
        // noinspection ConstantConditions
        if (processor == null) {
            throw new NullPointerException("attempt to set null PostProcessor");
        }
        this.postProcessor = processor;
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
            implCounter++;
            implementations.put(implCounter, impl);

            if (postProcessor != null) {
                postProcessorCount++;
                postProcessors.put(postProcessorCount, postProcessor);
            }
            if (resultAdapter != null) {
                resultAdapterCount++;
                resultAdapters.put(resultAdapterCount, resultAdapter);
            }
            return (FACE) Proxy.newProxyInstance(
                    face.getClassLoader(), new Class[]{face},
                    new DecouplexRequestHandler<>(context.getApplicationContext(),
                            face, implCounter,
                            postProcessor == null ? 0 : postProcessorCount,
                            resultAdapter == null ? 0 : resultAdapterCount));
        }
    }

    public static <FACE> FACE retrofit2(Context context, Class<FACE> face, FACE impl) {
        return new DecouplexBuilder<>(face)
                .impl(impl)
                .postProcessor(new Retrofit2PostProcessor())
                .resultAdapter(new Retrofit2ResultAdapter())
                .create(context);
    }

}
