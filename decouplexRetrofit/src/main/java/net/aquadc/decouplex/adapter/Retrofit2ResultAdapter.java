package net.aquadc.decouplex.adapter;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * Created by miha on 15.05.16.
 *
 */
public final class Retrofit2ResultAdapter implements ResultAdapter {

    public static final Retrofit2ResultAdapter INSTANCE = new Retrofit2ResultAdapter();

    private Retrofit2ResultAdapter() {}

    @Override
    public void adapt(Class face, String methodName, Method handler, Object answer, Set<Object> args) {
        args.add(answer); // todo: rm this adapter if no more code
    }
}
