package net.aquadc.decouplex.adapter;

import net.aquadc.decouplex.DcxResponse;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * Created by miha on 15.05.16
 */
public interface ErrorAdapter {
    /**
     *
     * @param face
     * @param methodName
     * @param handler
     * @param t
     * @param response may be successful, if {@code t instanceof InvocationTargetException} (exception thrown in @OnResult method)
     * @param params
     * @throws Throwable
     */
    void adapt(Class face, String methodName, Method handler, Throwable t, DcxResponse response, Set<Object> params) throws Throwable;
}
