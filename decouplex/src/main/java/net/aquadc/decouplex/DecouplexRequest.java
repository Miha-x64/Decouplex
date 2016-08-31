package net.aquadc.decouplex;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import net.aquadc.decouplex.annotation.Debounce;
import net.aquadc.decouplex.delivery.DeliveryStrategy;

import java.lang.reflect.Method;

/**
 * Created by miha on 20.08.16
 */
public final class DecouplexRequest implements Parcelable {

    final int decouplexId;
    final String methodName;
    final String[] parameterTypes;
    final Bundle parameters;
    final String receiverActionSuffix;
    final DeliveryStrategy deliveryStrategy;

    DecouplexRequest(int decouplexId, Method method, @Nullable Object[] args, String receiverActionSuffix, DeliveryStrategy deliveryStrategy) {
        this.decouplexId = decouplexId;
        this.methodName = method.getName();
        this.parameterTypes = parameterTypesOf(method);
        this.parameters = asBundle(method, args == null ? TypeUtils.EMPTY_ARRAY : args);
        this.receiverActionSuffix = receiverActionSuffix;
        this.deliveryStrategy = deliveryStrategy;
    }

    DecouplexRequest(int decouplexId, String methodName, String[] parameterTypes, Bundle parameters, String receiverActionSuffix, DeliveryStrategy deliveryStrategy) {
        this.decouplexId = decouplexId;
        this.methodName = methodName;
        this.parameterTypes = parameterTypes;
        this.parameters = parameters;
        this.receiverActionSuffix = receiverActionSuffix;
        this.deliveryStrategy = deliveryStrategy;
    }

    public void retry(Context context) {
        startExecService(context, prepare(null));
    }

    @Override
    public String toString() {
        StringBuilder bu = new StringBuilder("DecouplexRequest:");
        bu.append(methodName).append("(");
        for (Object param : parameters()) {
            bu.append(param);
        }
        bu.append(")");
        return bu.toString();
    }

    Bundle prepare(@Nullable Debounce debounce) {
        Bundle data = new Bundle(debounce == null ? 2 : 3);

        if (debounce != null) {
            data.putInt("debounce", (int) debounce.unit().toMillis(debounce.value()));
        }

        data.putString("receiver", receiverActionSuffix);

        data.putParcelable("request", deliveryStrategy.createRequest(this));
        data.putString("deliveryStrategy", deliveryStrategy.name());

        return data;
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
        Bundle bun = new Bundle(args.length);
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

    static void startExecService(Context context, Bundle extras) {
        Intent service = new Intent(context, DecouplexService.class); // TODO: different executors
        service.setAction(Decouplex.ACTION_EXEC);
        service.putExtras(extras);

        if (context.startService(service) == null) {
            throw new IllegalStateException("Did you forget to declare DecouplexService in your manifest?");
        }
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
        parcel.writeString(receiverActionSuffix);
        parcel.writeString(deliveryStrategy.name());
    }

    public static final Parcelable.Creator<DecouplexRequest> CREATOR = new Parcelable.Creator<DecouplexRequest>() {
        @Override
        public DecouplexRequest createFromParcel(Parcel parcel) {
            int decouplexId = parcel.readInt();
            String methodName = parcel.readString();
            String[] parameterTypes = parcel.createStringArray();
            Bundle parameters = parcel.readBundle(getClass().getClassLoader());
            String receiverActionSuffix = parcel.readString();
            DeliveryStrategy deliveryStrategy = DeliveryStrategy.valueOf(parcel.readString());
            return new DecouplexRequest(decouplexId, methodName, parameterTypes, parameters, receiverActionSuffix, deliveryStrategy);
        }

        @Override
        public DecouplexRequest[] newArray(int i) {
            return new DecouplexRequest[i];
        }
    };
}
