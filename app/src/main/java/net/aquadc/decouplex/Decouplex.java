package net.aquadc.decouplex;

import net.aquadc.decouplex.annotation.OnResult;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by miha on 14.05.16.
 *
 */
public abstract class Decouplex {

    public static final String ACTION = "DECOUPLEX";

    static final Map<Integer, Class> interfaces = new HashMap<>();
    static final Map<Integer, Object> implementations = new HashMap<>();

    public static Class face(int hash) {
        return interfaces.get(hash);
    }

    public static Method responseHandler(Class target, Class face, String methodName) {
        Method[] methods = target.getMethods();
        for (Method method : methods) {
            OnResult onResult = method.getAnnotation(OnResult.class);
            if (onResult == null)
                continue;
            if (onResult.face() != face)
                continue;
            if (!onResult.method().equals(methodName))
                continue;
            return method;
        }
        throw new RuntimeException("no handler for " + face.getSimpleName() + "::" + methodName);
    }

    public static Object impl(int hashCode) {
        return implementations.get(hashCode);
    }

}
