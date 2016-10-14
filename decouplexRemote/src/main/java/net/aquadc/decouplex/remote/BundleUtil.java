package net.aquadc.decouplex.remote;

import android.os.BaseBundle;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.util.Log;
import android.util.Size;
import android.util.SizeF;
import android.util.SparseArray;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Map;

/**
 * Created by miha on 15.05.16
 */
public final class BundleUtil {

    private BundleUtil() {
        throw new AssertionError();
    }

    /**
     * Internal Bundle field & method
     */
    private static final Field bundle_mMapField;
    private static final Method bundle_unparcel;
    static {
        Field mMap;
        Method unparcel;

        try {
            mMap = BaseBundle.class.getDeclaredField("mMap");
            mMap.setAccessible(true);
            unparcel = BaseBundle.class.getDeclaredMethod("unparcel");
            unparcel.setAccessible(true);
        } catch (NoClassDefFoundError e) {
            try {
                mMap = Bundle.class.getDeclaredField("mMap");
                mMap.setAccessible(true);
                unparcel = Bundle.class.getDeclaredMethod("unparcel");
                unparcel.setAccessible(true);
            } catch (Throwable t) {
                mMap = null;
                unparcel = null;
                Log.e("Converter", "init bundle internals", e);
            }
        } catch (Throwable e) {
            mMap = null;
            unparcel = null;
            Log.e("Converter", "init bundle internals", e);
        }
        bundle_mMapField = mMap;
        bundle_unparcel = unparcel;
    }

    /**
     * This allows you to put any object into Bundle. Use judiciously, not to trick Bundle,
     * but as an optimization and for debug purposes.
     */
    public static boolean ALLOW_REFLECT_PUT = true;

    /**
     * Put an object to a bundle
     * @param bun bundle
     * @param key key
     * @param value object
     */
    @SuppressWarnings("unchecked")
    public static void put(Bundle bun, String key, Type type, Object value) {
        if (value == null) {
            return;
        }

        if (ALLOW_REFLECT_PUT && reflectPut(bun, key, value)) {
            return;
        }

        // final classes that can be just found with ==
        Class klass = value.getClass();
        if (klass == Boolean.class) {
            bun.putBoolean(key, (boolean) value);
            return;
        } else if (klass == Byte.class) {
            bun.putByte(key, (byte) value);
            return;
        } else if (klass == Short.class) {
            bun.putShort(key, (short) value);
            return;
        } else if (klass == Character.class) {
            bun.putChar(key, (char) value);
            return;
        } else if (klass == Integer.class) {
            bun.putInt(key, (int) value);
            return;
        } else if (klass == Long.class) {
            bun.putLong(key, (long) value);
            return;
        } else if (klass == Float.class) {
            bun.putFloat(key, (float) value);
            return;
        } else if (klass == Double.class) {
            bun.putDouble(key, (double) value);
            return;
        } else if (klass == boolean[].class) {
            bun.putBooleanArray(key, (boolean[]) value);
            return;
        } else if (klass == byte[].class) {
            bun.putByteArray(key, (byte[]) value);
            return;
        } else if (klass == short[].class) {
            bun.putShortArray(key, (short[]) value);
            return;
        } else if (klass == char[].class) {
            bun.putCharArray(key, (char[]) value);
            return;
        } else if (klass == int[].class) {
            bun.putIntArray(key, (int[]) value);
            return;
        } else if (klass == long[].class) {
            bun.putLongArray(key, (long[]) value);
            return;
        } else if (klass == float[].class) {
            bun.putFloatArray(key, (float[]) value);
            return;
        } else if (klass == double[].class) {
            bun.putDoubleArray(key, (double[]) value);
            return;
        } else if (klass == String.class) {
            bun.putString(key, (String) value);
            return;
        } else if (klass == String[].class) {
            bun.putStringArray(key, (String[]) value);
            return;
        } else if (klass == Bundle.class) {
            bun.putBundle(key, (Bundle) value);
            return;
        } else if (Build.VERSION.SDK_INT >= 21 && klass == Size.class) {
            bun.putSize(key, (Size) value);
            return;
        } else if (Build.VERSION.SDK_INT >= 21 && klass == SizeF.class) {
            bun.putSizeF(key, (SizeF) value);
            return;
        } else
        // interfaces or non-final classes that must be checked with instanceof
        if (value instanceof  CharSequence) {
            bun.putCharSequence(key, (CharSequence) value);
            return;
        } else if (value instanceof CharSequence[]) {
            bun.putCharSequenceArray(key, (CharSequence[]) value);
            return;
        } else if (value instanceof Parcelable) {
            bun.putParcelable(key, (Parcelable) value);
            return;
        } else if (value instanceof Parcelable[]) {
            bun.putParcelableArray(key, (Parcelable[]) value);
            return;
        } else if (Build.VERSION.SDK_INT >= 18 && value instanceof IBinder) {
            bun.putBinder(key, (IBinder) value);
            return;
        } else
        // ArrayList<Something>
        if (value instanceof ArrayList) {
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
        } else
        // SparseArray<Parcelable>
        if (value instanceof SparseArray) {
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

    private static boolean reflectPut(Bundle bun, String key, Object value) {
        try {
            bundle_unparcel.invoke(bun);
            Map<String, Object> map = (Map) bundle_mMapField.get(bun);
            map.put(key, value);
            return true;
        } catch (Exception e) {
            Log.e("Converter", "reflectPut", e);
            return false;
        }
    }

}
