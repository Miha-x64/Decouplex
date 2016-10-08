package net.aquadc.decouplex;

import android.support.annotation.UiThread;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import static net.aquadc.decouplex.TypeUtils.arguments;

/**
 * Created by miha on 01.09.16
 */
public class DcxResponse {

    final DcxRequest request;
    final Object result;

    DcxResponse(DcxRequest request, Object result) {
        this.request = request;
        this.result = result;
    }

    /**
     * Dispatch method invocation result to an appropriate method.
     * First, searches in methods, targeted for a certain class;
     * non-targeted methods then.
     * Searches concrete methods first, wildcard-containing methods then and
     * "*"-methods finally.
     * @param resultHandler object whose class will be analyzed & on which the method will be invoked
     */
    @UiThread
    void dispatchResult(Object resultHandler) {
        String method = request.methodName;

        try {
            Method handler = HandlerSet.forMethod(method, true, request.handler);
            handler.setAccessible(true);

            Set<Object> args = new HashSet<>(2);

            if (request.resultAdapter == null) {
                args.add(result);
            } else {
                request.resultAdapter.adapt(request.face, method, handler, result, args);
            }

            handler.invoke(resultHandler, arguments(handler, args));
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e);
        }
    }

}
