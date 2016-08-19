package net.aquadc.decouplex.adapter;

import android.os.Bundle;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Set;

/**
 * Created by miha on 15.05.16.
 *
 */
public final class Retrofit2ErrorAdapter implements ErrorAdapter {

    public static final Retrofit2ErrorAdapter INSTANCE = new Retrofit2ErrorAdapter();

    private Retrofit2ErrorAdapter() {}

    @Override
    public void adapt(Class face, String methodName, Method handler, Throwable t, Bundle response, Set<Object> params) throws Throwable {
        if (t instanceof HttpException) {
            params.add(((HttpException) t).code);
            params.add(((HttpException) t).message);
        } else if (t instanceof IOException) {
            params.add(0);
            params.add(t.getMessage());
        } else {
            throw t;
        }
    }
}
