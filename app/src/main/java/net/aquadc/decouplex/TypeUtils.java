package net.aquadc.decouplex;

import android.os.Bundle;
import android.support.annotation.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by miha on 14.05.16.
 *
 */
/*package*/ abstract class TypeUtils {

    /**
     * wrapper-classes for primitives
     */
    private static final Map<Class, Class> wrappers;
    static {
        HashMap<Class, Class> m = new HashMap<>();

        m.put(boolean.class,Boolean.class);
        m.put(byte.class,   Byte.class);
        m.put(short.class,  Short.class);
        m.put(char.class,   Character.class);
        m.put(int.class,    Integer.class);
        m.put(long.class,   Long.class);
        m.put(float.class,  Float.class);
        m.put(double.class, Double.class);

        wrappers = Collections.unmodifiableMap(m);
    }

    /**
     * Class.forName ignores primitives, it is a workaround
     */
    private static final Map<String, Class> primitives;
    static {
        HashMap<String, Class> m = new HashMap<>();

        m.put(boolean.class.getCanonicalName(), boolean.class);
        m.put(byte.class.getCanonicalName(),    byte.class);
        m.put(short.class.getCanonicalName(),   short.class);
        m.put(char.class.getCanonicalName(),    char.class);
        m.put(int.class.getCanonicalName(),     int.class);
        m.put(long.class.getCanonicalName(),    long.class);
        m.put(float.class.getCanonicalName(),   float.class);
        m.put(double.class.getCanonicalName(),  double.class);

        primitives = Collections.unmodifiableMap(m);
    }

    /**
     * find handler for the method result
     * @param methodName    the name of method that has been invoked
     * @param classified    handlers for certain class
     * @param fallback      just handlers
     * @return Method to handle response
     */
    /*package*/ static Method handler(String methodName, Handlers classified, Handlers fallback) {

        Method handler = handler(methodName, classified);
        if (handler != null)
            return handler;

        handler = handler(methodName, fallback);
        if (handler != null)
            return handler;

        throw new RuntimeException("handler for " + methodName + " not found");
    }

    @Nullable
    private static Method handler(String methodName, Handlers handlers) {
        Method handler = handlers.immediateHandlers.get(methodName);
        if (handler != null)
            return handler;

        for (String wildcard : handlers.wildcardHandlers.keySet()) {
            if (matches(methodName, wildcard)) {
                return handlers.wildcardHandlers.get(wildcard);
            }
        }

        return handlers.fallback;
    }

    private static boolean matches(String text, String wildcard) {
        return text.matches(wildcard.replace("*", ".*?"));
    }

    /**
     * Pack parameters, passed to the method, to the bundle
     * @param bun bundle where arguments will be put
     * @param args arguments
     */
    /*package*/ static void packParameters(Bundle bun, Class[] types, Object[] args) {
        for (int i = 0; i < args.length; i++) {
            Converter.put(bun, Integer.toString(i), types[i], args[i]);
        }
    }

    /**
     * Unpack parameters
     * @param bun bundle where the parameters stored
     * @param count number of parameters
     * @return parameters
     */
    /*package*/ static Object[] unpackParameters(Bundle bun, int count) {
        Object[] params = new Object[count];
        for (int i = 0; i < count; i++) {
            params[i] = bun.get(Integer.toString(i));
        }
        return params;
    }

    /**
     * Store parameter types to the bundle
     * @param bun bundle to store types
     * @param types types
     */
    /*package*/ static void packTypes(Bundle bun, Class<?>[] types) {
        for (int i = 0; i < types.length; i++) {
            Class<?> type = types[i];
//            if (type.isPrimitive()) {
//                type = wrappers.get(type);
//            }
            bun.putString("T" + Integer.toString(i), type.getCanonicalName());
        }
    }

    /**
     * Obtain parameter types from the bundle
     * @param bun bundle
     * @return types
     */
    /*package*/ static Class<?>[] unpackTypes(Bundle bun) {
        ArrayList<Class> types = new ArrayList<>();
        String type;
        int i = 0;
        while ((type = bun.getString("T" + i)) != null) {
            types.add(classForName(type));
            i++;
        }
        Class[] classes = new Class[types.size()];
        return types.toArray(classes);
    }

    /*package*/ static Object[] arguments(Class[] types, Set<Object> args) {
        Object[] params = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            Class type = types[i];
            Object arg = null;
            for (Object o : args) {
                if (type.isInstance(o) ||
                        (type.isPrimitive() && wrappers.get(type).isInstance(o))) {
                    arg = o;
                    break;
                }
            }
            if (arg == null) {
                throw new IllegalArgumentException("can't find applicable argument of type " + type);
            }
            params[i] = arg;
        }
        return params;
    }

    /*package*/ static Object[] arguments(Class[] types, List<Set<Object>> args) {
        Object[] params = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            Class type = types[i];
            Object arg = null;
            outer:
            for (Set<Object> so : args) {
                for (Object o : so) {
                    if (type.isInstance(o) ||
                            (type.isPrimitive() && wrappers.get(type).isInstance(o))) {
                        arg = o;
                        break outer;
                    }
                }
            }
            if (arg == null) {
                throw new IllegalArgumentException("can't find applicable argument of type " + type);
            }
            params[i] = arg;
        }
        return params;
    }

    private static Class<?> classForName(String name) {
        Class cls = primitives.get(name);
        if (cls != null)
            return cls;
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

}
