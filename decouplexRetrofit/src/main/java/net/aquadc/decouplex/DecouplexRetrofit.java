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

    public static <FACE, HANDLER> DecouplexBuilder<FACE, HANDLER> retrofit2Builder(Class<FACE> face, FACE impl, Class<HANDLER> handler) {
        return new DecouplexBuilder<>(face, impl, handler)
                .resultProcessor(Retrofit2ResultProcessor.INSTANCE)
                .errorAdapter(Retrofit2ErrorAdapter.INSTANCE)
                .threads(2);
    }

    public static <FACE, HANDLER> FACE retrofit2(Context context, Class<FACE> face, FACE impl, Class<HANDLER> handler) {
        return retrofit2Builder(face, impl, handler)
                .create(context);
    }

}
