package net.aquadc.decouplex;

import android.support.annotation.UiThread;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.NoSuchElementException;
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
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        } catch (InvocationTargetException e) {
            dispatchErrorInternal(e, resultHandler);
        }
    }

    /**
     * Acts as dispatchResult, but for errors.
     */
    @UiThread
    void dispatchError(Object resultHandler) {
        dispatchErrorInternal((Throwable) result, resultHandler);
    }

    private void dispatchErrorInternal(Throwable exception, Object resultHandler) {
        HashSet<Object> args = new HashSet<>(2);
        args.add(request);
        args.add(exception);

        try {
            Method handler = HandlerSet.forMethod(request.face, request.methodName, false, request.handler);

            handler.setAccessible(true);

            if (request.errorAdapter != null) {
                request.errorAdapter.adapt(request.face, request.methodName, handler, exception, this, args);
            }

            handler.invoke(resultHandler, arguments(handler, args));
        } catch (NoSuchElementException e) {
            // no error handler
            if (request.fallbackErrorHandler == null) {
                throw new RuntimeException(e);
            } else {
                request.fallbackErrorHandler.onError(request, exception);
            }
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        } catch (InvocationTargetException deliveryFail) {
            // exception in @OnError
            if (request.fallbackErrorHandler == null) {
                throw new RuntimeException(deliveryFail);
            } else {
                request.fallbackErrorHandler.onErrorDeliveryFail(request, exception, deliveryFail.getTargetException());
            }
        } catch (Throwable t) {
            // thrown by adapter
            if (request.fallbackErrorHandler == null) {
                throw new RuntimeException(t);
            } else {
                request.fallbackErrorHandler.onErrorDeliveryFail(request, exception, t);
            }
        }
    }

}
