package net.aquadc.decouplex;

import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.util.Log;
import android.util.Size;
import android.util.SizeF;
import android.util.SparseArray;

import net.aquadc.decouplex.adapter.Packer;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by miha on 15.05.16.
 *
 */
public abstract class Converter {

    private Converter() {
    }

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
    private static final Map<Class, Packer> mediatedPackers;
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

        // SparseArray<Parcelable>
        if (SparseArray.class.isInstance(value)) {
            Class E = (Class) ((ParameterizedType) type).getActualTypeArguments()[0];
            if (Parcelable.class.isAssignableFrom(E)) {
                bun.putSparseParcelableArray(key, (SparseArray<? extends  Parcelable>) value);
                return;
            }
        }

        // the last, the worst try
        if (value instanceof Serializable) {
            bun.putSerializable(key, (Serializable) value);
            if (!(value instanceof Throwable)) { // serializing Exceptions is ok
                Log.e("Decouplex", "warn: writing Serializable (" + value.getClass() + ") to bundle");
            }
            return;
        }

        throw new IllegalArgumentException("unknown type: " + value.getClass());
    }

}
