package net.aquadc.decouplex;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * Created by miha on 20.08.16
 */
public final class DecouplexRequest implements Parcelable {

    final int decouplexId;
    final String methodName;
    final String[] parameterTypes;
    final Bundle parameters;

    DecouplexRequest(int decouplexId, Method method, @Nullable Object[] args) {
        this.decouplexId = decouplexId;
        this.methodName = method.getName();
        this.parameterTypes = parameterTypesOf(method);
        this.parameters = asBundle(method, args == null ? TypeUtils.EMPTY_ARRAY : args);
    }

    DecouplexRequest(int decouplexId, String methodName, String[] parameterTypes, Bundle parameters) {
        this.decouplexId = decouplexId;
        this.methodName = methodName;
        this.parameterTypes = parameterTypes;
        this.parameters = parameters;
    }

    private static String[] parameterTypesOf(Method method) {
        Class[] classes = method.getParameterTypes();
        final int len = classes.length;
        String[] types = new String[len];
        for (int i = 0; i < len; i++) {
            types[i] = classes[i].getCanonicalName();
        }
        return types;
    }

    private static Bundle asBundle(Method method, Object[] args) {
        Bundle bun = new Bundle(args.length); // todo capacity
        TypeUtils.packParameters(bun, method.getParameterTypes(), args);
        return bun;
    }

    Class[] parameterTypes() {
        final int count = parameterTypes.length;
        String[] classes = parameterTypes;
        Class[] types = new Class[count];
        for (int i = 0; i < count; i++) {
            types[i] = TypeUtils.classForName(classes[i]);
        }
        return types;
    }

    Object[] parameters() {
        final int count = parameterTypes.length;
        Bundle bundled = this.parameters;
        Object[] parameters = new Object[count];
        for (int i = 0; i < count; i++) {
            parameters[i] = bundled.get(Integer.toString(i));
        }
        return parameters;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(decouplexId);
        parcel.writeString(methodName);
        parcel.writeStringArray(parameterTypes);
        parcel.writeBundle(parameters);
    }

    public static final Parcelable.Creator<DecouplexRequest> CREATOR = new Parcelable.Creator<DecouplexRequest>() {
        @Override
        public DecouplexRequest createFromParcel(Parcel parcel) {
            int decouplexId = parcel.readInt();
            String methodName = parcel.readString();
            String[] parameterTypes = parcel.createStringArray();
            Bundle parameters = parcel.readBundle(getClass().getClassLoader());
            return new DecouplexRequest(decouplexId, methodName, parameterTypes, parameters);
        }

        @Override
        public DecouplexRequest[] newArray(int i) {
            return new DecouplexRequest[i];
        }
    };
}
