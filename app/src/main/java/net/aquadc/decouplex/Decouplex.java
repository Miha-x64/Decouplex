package net.aquadc.decouplex;

import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;

import net.aquadc.decouplex.annotation.OnResult;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by miha on 14.05.16.
 *
 */
public abstract class Decouplex {

    public static final String ACTION = "DECOUPLEX";

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
//      API 21
//      tmp.put(Size.class,       (b, k, v) -> b.putSize(k, (Size) v));
//      tmp.put(SizeF.class,      (b, k, v) -> b.putSizeF(k, (SizeF) v));

        m.put(String[].class,       (b, k, v) -> b.putStringArray(k, (String[]) v));

        immediatePackers = Collections.unmodifiableMap(m);
    }
    public static final Map<Class, Packer> mediatedPackers;
    static {
        HashMap<Class, Packer> m = new HashMap<>();

        m.put(CharSequence.class,   (b, k, v) -> b.putCharSequence(k,       (CharSequence) v));
        m.put(CharSequence[].class, (b, k, v) -> b.putCharSequenceArray(k,  (CharSequence[]) v));

        m.put(Parcelable.class,     (b, k, v) -> b.putParcelable(k,         (Parcelable) v));
        m.put(Parcelable[].class,   (b, k, v) -> b.putParcelableArray(k,    (Parcelable[]) v));

//      API 18
//      m.put(IBinder.class,        (b, k, v) -> b.putBinder(k, (IBinder) v));

        mediatedPackers = Collections.unmodifiableMap(m);
    }

    /*public static final Map<Class, Class> wrappers;
    static {
        HashMap<Class, Class> m = new HashMap<>();

        m.put(boolean.class, Boolean.class);
        m.put(byte.class,   Byte.class);
        m.put(short.class,  Short.class);
        m.put(char.class,   Character.class);
        m.put(int.class,    Integer.class);
        m.put(long.class,   Long.class);
        m.put(float.class,  Float.class);
        m.put(double.class, Double.class);

        wrappers = Collections.unmodifiableMap(m);
    }*/

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

    public static void packParameters(Bundle bun, Object[] args) {
        for (int i = 0; i < args.length; i++) {
            put(bun, Integer.toString(i), args[i]);
        }
    }

    public static Object[] unpackParameters(Bundle bun, int count) {
        Object[] params = new Object[count];
        for (int i = 0; i < count; i++) {
            params[i] = bun.get(Integer.toString(i));
        }
        return params;
    }

    public static void packTypes(Bundle bun, Class<?>[] types) {
        for (int i = 0; i < types.length; i++) {
            Class<?> type = types[i];
//            if (type.isPrimitive()) {
//                type = wrappers.get(type);
//            }
            bun.putString("T" + Integer.toString(i), type.getCanonicalName());
        }
    }

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

    public static void put(Bundle bun, String key, Object value) {
        if (value == null)
            return;

        Packer p = immediatePackers.get(value.getClass());
        if (p != null) {
            p.put(bun, key, value);
            return;
        }

        for (Class type : mediatedPackers.keySet()) {
            if (type.isInstance(value)) {
                mediatedPackers.get(type).put(bun, key, value);
                return;
            }
        }

        if (value instanceof Serializable) {
            bun.putSerializable(key, (Serializable) value);
            Log.e("Decouplex", "warn: writing Serializable (" + value.getClass() + ") to bundle");
            return;
        }

        // FIXME: ArrayList<? ex String/CharSequence/Integer/Parcelable>
        // FIXME: SparseArray<? ex Parcelable>

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

}
