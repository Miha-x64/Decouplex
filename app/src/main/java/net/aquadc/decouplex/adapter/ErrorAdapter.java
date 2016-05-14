package net.aquadc.decouplex.adapter;

import android.os.Bundle;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * Created by miha on 15.05.16.
 *
 */
public interface ErrorAdapter {
    void adaptErrorParams(Class face, String methodName, Method handler, Throwable t, Bundle bun, Set<Object> params);
}
