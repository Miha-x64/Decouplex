package net.aquadc.decouplex.adapter;

import net.aquadc.decouplex.DecouplexResponse;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * Created by miha on 15.05.16
 */
public interface ErrorAdapter {
    void adapt(Class face, String methodName, Method handler, Throwable t, DecouplexResponse response, Set<Object> params) throws Throwable;
}
