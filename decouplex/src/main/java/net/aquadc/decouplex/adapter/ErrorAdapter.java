package net.aquadc.decouplex.adapter;

import android.os.Bundle;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * Created by miha on 15.05.16.
 * {@see ResultAdapter}
 */
public interface ErrorAdapter {
    void adapt(Class face, String methodName, Method handler, Throwable t, Bundle response, Set<Object> params) throws Throwable;
}
