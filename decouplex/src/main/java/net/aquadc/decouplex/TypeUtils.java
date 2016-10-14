package net.aquadc.decouplex;

import android.support.v4.util.SimpleArrayMap;

import net.aquadc.decouplex.annotation.DcxNullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Set;

/**
 * Created by miha on 14.05.16.
 *
 */
/*package*/ final class TypeUtils { // rename me

    private TypeUtils() {
        throw new AssertionError();
    }

    /*package*/ static final Object[] EMPTY_ARRAY = new Object[0];

    /**
     * wrapper-classes for primitives
     */
    private static final SimpleArrayMap<Class, Class> wrappers;
    static {
        SimpleArrayMap<Class, Class> m = new SimpleArrayMap<>(8);

        m.put(boolean.class,Boolean.class);
        m.put(byte.class,   Byte.class);
        m.put(short.class,  Short.class);
        m.put(char.class,   Character.class);
        m.put(int.class,    Integer.class);
        m.put(long.class,   Long.class);
        m.put(float.class,  Float.class);
        m.put(double.class, Double.class);

        wrappers = m;
    }

    /**
     * Class.forName ignores primitives, this is a workaround
     */
    /*private static final SimpleArrayMap<String, Class> primitives;
    static {
        SimpleArrayMap<String, Class> m = new SimpleArrayMap<>(8);

        m.put(boolean.class.getCanonicalName(), boolean.class);
        m.put(byte.class.getCanonicalName(),    byte.class);
        m.put(short.class.getCanonicalName(),   short.class);
        m.put(char.class.getCanonicalName(),    char.class);
        m.put(int.class.getCanonicalName(),     int.class);
        m.put(long.class.getCanonicalName(),    long.class);
        m.put(float.class.getCanonicalName(),   float.class);
        m.put(double.class.getCanonicalName(),  double.class);

        primitives = m;
    }*/

    /**
     * Pack parameters, passed to the method, to the bundle
     * @param bun bundle where arguments will be put
     * @param args arguments
     */
    /*package*/ /*static void packParameters(Bundle bun, Class[] types, Object[] args) {
        for (int i = 0; i < args.length; i++) {
            Converter.put(bun, Integer.toString(i), types[i], args[i]);
        }
    }*/

    /**
     * Unpack parameters
     * @param bun bundle where the parameters stored
     * @param count number of parameters
     * @return parameters
     */
    /*package*/ /*static Object[] unpackParameters(Bundle bun, int count) {
        Object[] params = new Object[count];
        for (int i = 0; i < count; i++) {
            params[i] = bun.get(Integer.toString(i));
        }
        return params;
    }*/

    /**
     * Store parameter types to the bundle
     * @param bun bundle to store types
     * @param types types
     */
    /*package*/ /*static void packTypes(Bundle bun, Class<?>[] types) {
        for (int i = 0; i < types.length; i++) {
            Class<?> type = types[i];
//            if (type.isPrimitive()) {
//                type = wrappers.get(type);
//            }
            bun.putString('T' + Integer.toString(i), type.getCanonicalName());
        }
    }*/

    /**
     * Obtain parameter types from the bundle
     * @param bun bundle
     * @return types
     */
    /*package*/ /*static Class<?>[] unpackTypes(Bundle bun) {
        ArrayList<Class> types = new ArrayList<>(4);
        String type;
        int i = 0;
        while ((type = bun.getString("T" + i)) != null) {
            types.add(classForName(type));
            i++;
        }
        Class[] classes = new Class[types.size()];
        return types.toArray(classes);
    }*/

    // eats args!
    /*package*/ static Object[] arguments(Method handlerMethod, Collection<?> args) {
        Class[] types = handlerMethod.getParameterTypes();
        final int size = types.length;
        boolean[] nullables = null;
        {
            Annotation[][] annotations = handlerMethod.getParameterAnnotations();
            for (int i = 0; i < size; i++) {
                for (Annotation ann : annotations[i]) {
                    if (ann.annotationType() == DcxNullable.class) {
                        if (nullables == null) {
                            nullables = new boolean[size];
                        }
                        nullables[i] = true;
                        break;
                    }
                }
            }
        }
        Object[] params = new Object[size];
        SimpleArrayMap<Class, Class> wrappers = TypeUtils.wrappers;
        for (int i = 0; i < size; i++) {
            // first, go through non-nullable args
            if (nullables != null && nullables[i]) {
                continue;
            }

            Class type = types[i];
            Object arg = null;
            for (Object o : args) {
                if (type.isInstance(o) ||
                        (type.isPrimitive() && wrappers.get(type).isInstance(o))) {
                    arg = o;
                    args.remove(o);
                    break;
                }
            }
            if (arg == null) {
                throw new IllegalArgumentException("can't find applicable argument of type '" + type + "' for method '" + handlerMethod + '\'');
            }
            params[i] = arg;
        }

        if (nullables != null) {
            // go through nullable arguments then
            for (int i = 0; i < size; i++) {
                if (!nullables[i]) {
                    continue;
                }

                Class type = types[i];
                for (Object o : args) {
                    if (type.isInstance(o) ||
                            (type.isPrimitive() && wrappers.get(type).isInstance(o))) {
                        params[i] = o;
                        args.remove(o);
                        break;
                    }
                }
            }
        }

        return params;
    }

    /*package*/ /*static Class<?> classForName(String name) {
        Class cls = primitives.get(name);
        if (cls != null)
            return cls;
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }*/

}
