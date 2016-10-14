package net.aquadc.decouplex;

import android.support.annotation.UiThread;
import android.util.ArraySet;

import net.aquadc.decouplex.adapter.ErrorAdapter;

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
        final DcxRequest request = this.request;
        String method = request.methodName;

        try {
            Method handler = HandlerSet.forMethod(request.face, method, true, request.handler);
            handler.setAccessible(true);

            Set<Object> args = new HashSet<>(2);

            if (request.resultAdapter == null) {
                args.add(result);
            } else {
                request.resultAdapter.adapt(request.face, method, handler, result, args);
            }

            handler.invoke(resultHandler, arguments(handler, args));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Acts as dispatchResult, but for errors.
     */
    @UiThread
    void dispatchError(ErrorAdapter errorAdapter, Class<?> face,
                              DcxInvocationHandler.ErrorHandler fallbackErrorHandler,
                              Class<?> handlerClass,
                              Object resultHandler) {
        Throwable executionFail = (Throwable) result;

        DcxRequest req = request;
        HashSet<Object> args = new HashSet<>(2);
        args.add(req);
        args.add(executionFail);

        try {
            Method handler = HandlerSet.forMethod(req.face, req.methodName, false, handlerClass);

            handler.setAccessible(true);

            if (errorAdapter != null) {
                errorAdapter.adapt(face, req.methodName, handler, executionFail, this, args);
            }

            handler.invoke(resultHandler, arguments(handler, args));
        } catch (Throwable deliveryFail) {
            if (fallbackErrorHandler == null) {
                if (deliveryFail instanceof RuntimeException) {
                    throw (RuntimeException) deliveryFail; // don't create a new one
                }
                throw new RuntimeException(deliveryFail);
            } else {
                fallbackErrorHandler.onError(req, executionFail);
            }
        }
    }

}
