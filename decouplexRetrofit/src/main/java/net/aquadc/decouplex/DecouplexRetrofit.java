package net.aquadc.decouplex;

import android.content.Context;

import net.aquadc.decouplex.adapter.Retrofit2ErrorAdapter;
import net.aquadc.decouplex.adapter.Retrofit2ResultProcessor;

/**
 * Created by miha on 17.08.16
 */
public final class DecouplexRetrofit {

    private DecouplexRetrofit() {
        throw new AssertionError();
    }

    /**
     * Creates new Builder with Retrofit2 adapters and 2 threads.
     *
     * @param face         Retrofit2-compatible interface
     * @param impl         implementation of this interface from {@code new Retrofit.Builder().(...).create()}
     * @param handler      class where results wll be delivered, typically {@code CurrentClassName.class}
     * @return new Decouplex Builder
     */
    public static <FACE, HANDLER> DecouplexBuilder<FACE, HANDLER>
    retrofit2Builder(Class<FACE> face, FACE impl, Class<HANDLER> handler) {
        return new DecouplexBuilder<>(face, impl, handler)
                .resultProcessor(Retrofit2ResultProcessor.INSTANCE)
                .errorAdapter(Retrofit2ErrorAdapter.INSTANCE)
                .threads(2);
    }

    /**
     * Acts like {@link DecouplexRetrofit#retrofit2Builder(java.lang.Class, java.lang.Object, java.lang.Class)}
     * but returns already created Decouplex.
     *
     * @param context      context (to start Service)
     * @param face         Retrofit2-compatible interface
     * @param impl         implementation
     * @param handler      handler class
     * @return new Decouplex
     */
    public static <FACE, HANDLER> FACE retrofit2(Context context, Class<FACE> face, FACE impl, Class<HANDLER> handler) {
        return retrofit2Builder(face, impl, handler)
                .create(context);
    }

}
