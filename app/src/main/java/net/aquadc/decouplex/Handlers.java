package net.aquadc.decouplex;

import android.support.annotation.Nullable;

import net.aquadc.decouplex.annotation.OnError;
import net.aquadc.decouplex.annotation.OnResult;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by miha on 15.05.16.
 *
 */
class Handlers {

    final Map<String, Method> immediateHandlers = new HashMap<>();
    final Map<String, Method> wildcardHandlers = new HashMap<>();
    Method fallback;

    private Handlers() {
    }

    /**
     * handlers management
     */
    private static final Map<Class, Reference<HandlerSet>> handlerSets = new HashMap<>();

    static HandlerSet forClass(Class target) {
        HandlerSet set = get(handlerSets.get(target));
        if (set != null)
            return set;

        Handlers targetedResultHandlers = new Handlers();
        Handlers resultHandlers = new Handlers();
        Handlers targetedErrorHandlers = new Handlers();
        Handlers errorHandlers = new Handlers();

        for (Method method : target.getDeclaredMethods()) {
            OnResult onResult = method.getAnnotation(OnResult.class);
            if (onResult != null) {
                if (onResult.face() == null) {
                    add(resultHandlers, method, onResult.value());
                } else {
                    add(targetedResultHandlers, method, onResult.value());
                }
            }

            OnError onError = method.getAnnotation(OnError.class);
            if (onError != null) {
                if (onError.face() == null) {
                    add(errorHandlers, method, onError.value());
                } else {
                    add(targetedErrorHandlers, method, onError.value());
                }
            }
        }

        set = new HandlerSet(targetedResultHandlers, resultHandlers, targetedErrorHandlers, errorHandlers);
        handlerSets.put(target, new WeakReference<>(set));
        return set;
    }

    private static void add(Handlers handlers, Method method, String methodName) {
        if (methodName.equals("*")) {
            if (handlers.fallback != null) {
                throw new IllegalStateException("ambiguous * handlers: " + handlers.fallback + " and " + method);
            }
            handlers.fallback = method;
            return;
        }

        if (methodName.contains("*")) {
            Method oldMethod = handlers.wildcardHandlers.put(methodName, method);
            if (oldMethod != null) {
                throw new IllegalStateException("ambiguous wildcard handlers: " + oldMethod + " and " + method);
            }
            return;
        }

        Method oldMethod = handlers.immediateHandlers.put(methodName, method);
        if (oldMethod != null) {
            throw new IllegalStateException("ambiguous immediate handlers: " + oldMethod + " and " + method);
        }
    }

    @Nullable
    private static <T> T get(@Nullable Reference<T> ref) {
        return ref == null ? null : ref.get();
    }

}
