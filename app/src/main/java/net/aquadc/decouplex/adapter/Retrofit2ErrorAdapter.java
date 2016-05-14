package net.aquadc.decouplex.adapter;

import android.os.Bundle;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * Created by miha on 15.05.16.
 *
 */
public class Retrofit2ErrorAdapter implements ErrorAdapter {
    @Override
    public void adaptErrorParams(Class face, String methodName, Method handler, Throwable t, Bundle bun, Set<Object> params) {
        if (t instanceof HttpException) {
            params.add(((HttpException) t).code);
            params.add(((HttpException) t).message);
        } else {
            params.add(0);
            params.add("");
        }
    }
}
