package net.aquadc.decouplex.adapter;

import android.os.Bundle;
import android.support.annotation.Nullable;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * Created by miha on 15.05.16.
 * you can add arguments to the set so they can be dispatched to the handler method
 */
public interface ResultAdapter {
    void adapt(Class face, String methodName, @Nullable Method handler, Bundle answer, Set<Object> args);
}
