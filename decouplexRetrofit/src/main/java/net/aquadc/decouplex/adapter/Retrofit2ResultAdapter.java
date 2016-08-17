package net.aquadc.decouplex.adapter;

import android.os.Bundle;

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
    public void adapt(Class face, String methodName, Method handler, Bundle answer, Set<Object> args) {
        args.add(answer.get("body"));
        args.add(answer.get("code"));
    }
}
