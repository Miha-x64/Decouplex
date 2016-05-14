package net.aquadc.decouplex;

import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.util.Log;
import android.util.Size;
import android.util.SizeF;

import net.aquadc.decouplex.adapter.Packer;
import net.aquadc.decouplex.adapter.PostProcessor;
import net.aquadc.decouplex.adapter.ResultAdapter;
import net.aquadc.decouplex.annotation.OnResult;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by miha on 14.05.16.
 *
 */
public abstract class Decouplex {

    /**
     * Action to use in Intents
     */
    public static final String ACTION = "DECOUPLEX";

    /**
     * lambdas used to pack objects of final types
     * (no primitives here, because {@see java.lang.reflect.Proxy} wraps them)
     */
    private static final Map<Class, Packer> immediatePackers;
    static {
        HashMap<Class, Packer> m = new HashMap<>();

        m.put(Boolean.class,        (b, k, v) -> b.putBoolean(k,    (boolean) v));
        m.put(Byte.class,           (b, k, v) -> b.putByte(k,       (byte) v));
        m.put(Short.class,          (b, k, v) -> b.putShort(k,      (short) v));
        m.put(Character.class,      (b, k, v) -> b.putChar(k,       (char) v));
        m.put(Integer.class,        (b, k, v) -> b.putInt(k,        (int) v));
        m.put(Long.class,           (b, k, v) -> b.putLong(k,       (long) v));
        m.put(Float.class,          (b, k, v) -> b.putFloat(k,      (float) v));
        m.put(Double.class,         (b, k, v) -> b.putDouble(k,     (double) v));

        m.put(boolean[].class,      (b, k, v) -> b.putBooleanArray(k,   (boolean[]) v));
        m.put(byte[].class,         (b, k, v) -> b.putByteArray(k,      (byte[]) v));
        m.put(short[].class,        (b, k, v) -> b.putShortArray(k,     (short[]) v));
        m.put(char[].class,         (b, k, v) -> b.putCharArray(k,      (char[]) v));
        m.put(int[].class,          (b, k, v) -> b.putIntArray(k,       (int[]) v));
        m.put(long[].class,         (b, k, v) -> b.putLongArray(k,      (long[]) v));
        m.put(float[].class,        (b, k, v) -> b.putFloatArray(k,     (float[]) v));
        m.put(double[].class,       (b, k, v) -> b.putDoubleArray(k,    (double[]) v));

        m.put(String.class,         (b, k, v) -> b.putString(k, (String) v));
        m.put(Bundle.class,         (b, k, v) -> b.putBundle(k, (Bundle) v));

        if (Build.VERSION.SDK_INT >= 21) {
            m.put(Size.class, (b, k, v) -> b.putSize(k, (Size) v));
            m.put(SizeF.class, (b, k, v) -> b.putSizeF(k, (SizeF) v));
        }

        m.put(String[].class,       (b, k, v) -> b.putStringArray(k, (String[]) v));

        immediatePackers = Collections.unmodifiableMap(m);
    }

    /**
     * lambdas used to pack objects of non-final types
     */
    public static final Map<Class, Packer> mediatedPackers;
    static {
        HashMap<Class, Packer> m = new HashMap<>();

        m.put(CharSequence.class,   (b, k, v) -> b.putCharSequence(k,       (CharSequence) v));
        m.put(CharSequence[].class, (b, k, v) -> b.putCharSequenceArray(k,  (CharSequence[]) v));

        m.put(Parcelable.class,     (b, k, v) -> b.putParcelable(k,         (Parcelable) v));
        m.put(Parcelable[].class,   (b, k, v) -> b.putParcelableArray(k,    (Parcelable[]) v));

        if (Build.VERSION.SDK_INT >= 18) {
            m.put(IBinder.class, (b, k, v) -> b.putBinder(k, (IBinder) v));
        }

        mediatedPackers = Collections.unmodifiableMap(m);
    }

    public static final Map<Class, Class> wrappers;
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
    public static final Map<String, Class> primitives;
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
     * Implementations of interfaces
     */
    static final Map<Integer, Object> implementations = new HashMap<>();

    public static Object impl(int code) {
        return implementations.get(code);
    }

    /**
     * Post-processors
     */
    static final Map<Integer, PostProcessor> postProcessors = new HashMap<>();

    public static PostProcessor postProcessor(int code) {
        return postProcessors.get(code);
    }

    /**
     * Result adapters
     */
    static final Map<Integer, ResultAdapter> resultAdapters = new HashMap<>();

    public static ResultAdapter resultAdapter(int code) {
        return resultAdapters.get(code);
    }

    /**
     * find handler for the method result
     * @param target class where lookup will be produced
     * @param face interface through which the action was performed
     * @param methodName method name from the given interface
     * @return method to handle response
     */
    public static Method responseHandler(Class target, Class face, String methodName) {
        Method[] methods = target.getDeclaredMethods();
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
        throw new RuntimeException("handler for " + face.getSimpleName() + "::" + methodName +
                " not found in " + target.getSimpleName());
    }

    /**
     * Pack parameters, passed to the method, to the bundle
     * @param bun bundle where arguments will be put
     * @param args arguments
     */
    public static void packParameters(Bundle bun, Class[] types, Object[] args) {
        for (int i = 0; i < args.length; i++) {
            put(bun, Integer.toString(i), types[i], args[i]);
        }
    }

    /**
     * Unpack parameters
     * @param bun bundle where the parameters stored
     * @param count number of parameters
     * @return parameters
     */
    public static Object[] unpackParameters(Bundle bun, int count) {
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
    public static void packTypes(Bundle bun, Class<?>[] types) {
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
    public static Class<?>[] unpackTypes(Bundle bun) {
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

    /**
     * Put an object to a bundle
     * @param bun bundle
     * @param key key
     * @param value object
     */
    @SuppressWarnings("unchecked")
    public static void put(Bundle bun, String key, Type type, Object value) {
        if (value == null)
            return;

        // final classes that can be just found in the set
        Packer p = immediatePackers.get(value.getClass());
        if (p != null) {
            p.put(bun, key, value);
            return;
        }

        // interfaces or non-final classes that must be checked with instanceof
        for (Class medType : mediatedPackers.keySet()) {
            if (medType.isInstance(value)) {
                mediatedPackers.get(medType).put(bun, key, value);
                return;
            }
        }

        // ArrayList<Something>

        if (ArrayList.class.isInstance(value)) {
            Class E = (Class) ((ParameterizedType) type).getActualTypeArguments()[0];
            if (E == Integer.class) {
                bun.putIntegerArrayList(key, (ArrayList<Integer>) value);
                return;
            } else if (E == String.class) {
                bun.putStringArrayList(key, (ArrayList<String>) value);
                return;
            } else if (CharSequence.class.isAssignableFrom(E)) {
                bun.putCharSequenceArrayList(key, (ArrayList<CharSequence>) value);
                return;
            } else if (Parcelable.class.isAssignableFrom(E)) {
                bun.putParcelableArrayList(key, (ArrayList<Parcelable>) value);
                return;
            }
        }

        // TODO: SparseArray<Parcelable>

        // the last, the worst try
        if (value instanceof Serializable) {
            bun.putSerializable(key, (Serializable) value);
            Log.e("Decouplex", "warn: writing Serializable (" + value.getClass() + ") to bundle");
            return;
        }

        throw new IllegalArgumentException("unknown type: " + value.getClass());
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

    public static Object[] arguments(Class[] types, Set<Object> args) {
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

}
