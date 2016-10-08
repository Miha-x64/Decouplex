package net.aquadc.decouplex;

import android.support.v4.util.SimpleArrayMap;

import net.aquadc.decouplex.annotation.OnError;
import net.aquadc.decouplex.annotation.OnResult;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

/**
 * Created by miha on 15.05.16.
 *
 */
final class Handlers {

    private final SimpleArrayMap<String, Method> immediateHandlers = new SimpleArrayMap<>();
    private final SimpleArrayMap<String, Method> wildcardHandlers = new SimpleArrayMap<>();
    private Method fallback;

    private Handlers() {
    }

    /**
     * handlers management
     */
    private static final SimpleArrayMap<Class, Reference<HandlerSet>> handlerSets = new SimpleArrayMap<>();

    static HandlerSet forClass(Class target) {
        Reference<HandlerSet> setRef = handlerSets.get(target);
        if (setRef != null) {
            HandlerSet set = setRef.get();
            if (set != null) {
                return set;
            }
        }

        Handlers targetedResultHandlers = new Handlers();
        Handlers resultHandlers = new Handlers();
        Handlers targetedErrorHandlers = new Handlers();
        Handlers errorHandlers = new Handlers();

        for (Method method : target.getDeclaredMethods()) {
            OnResult onResult = method.getAnnotation(OnResult.class);
            if (onResult != null) {
                if (onResult.face() == Void.class) {
                    add(resultHandlers, method, onResult.value());
                } else if (onResult.face() == target) { // todo: test targeting
                    add(targetedResultHandlers, method, onResult.value());
                }
            }

            OnError onError = method.getAnnotation(OnError.class);
            if (onError != null) {
                if (onError.face() == Void.class) {
                    add(errorHandlers, method, onError.value());
                } else if (onError.face() == target) {
                    add(targetedErrorHandlers, method, onError.value());
                }
            }
        }

        HandlerSet set = new HandlerSet(targetedResultHandlers, resultHandlers, targetedErrorHandlers, errorHandlers);
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

    Method forName(String methodName) {
        Method handler = immediateHandlers.get(methodName);
        if (handler != null)
            return handler;

        SimpleArrayMap<String, Method> wildcardHandlers = this.wildcardHandlers;
        int size = wildcardHandlers.size();
        for (int i = 0; i < size; i++) {
            String wildcard = wildcardHandlers.keyAt(i);
            if (methodName.matches(wildcard.replace("*", ".*?"))) {
                return wildcardHandlers.get(wildcard);
            }
        }

        return fallback;
    }

    @Override
    public String toString() {
        return "Handlers(" +
                "immediate: " + immediateHandlers + ", " +
                "wildcard: " + wildcardHandlers + ", " +
                "fallback: " + fallback + ')';
    }
}
