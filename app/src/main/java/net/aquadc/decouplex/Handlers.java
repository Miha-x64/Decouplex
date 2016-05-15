package net.aquadc.decouplex;

import net.aquadc.decouplex.annotation.OnError;
import net.aquadc.decouplex.annotation.OnResult;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by miha on 15.05.16.
 *
 */
class Handlers {

    Map<String, Method> immediateHandlers = new HashMap<>();
    Map<String, Method> wildcardHandlers = new HashMap<>();
    Method fallback;

    private Handlers() {
    }

    static Handlers[] analyze(Class target) {
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

        return new Handlers[]{targetedResultHandlers, resultHandlers, targetedErrorHandlers, errorHandlers};
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


}
