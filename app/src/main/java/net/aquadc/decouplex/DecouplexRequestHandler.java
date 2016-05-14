package net.aquadc.decouplex;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import net.aquadc.decouplex.android.DecouplexService;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import static net.aquadc.decouplex.Decouplex.*;

/**
 * Created by miha on 14.05.16.
 *
 */
public class DecouplexRequestHandler<FACE> implements InvocationHandler {

    private final Class<FACE> face;
    private final FACE impl;
    private final Context context;

    DecouplexRequestHandler(Context context, Class<FACE> face, FACE impl) {
        this.face = face;
        this.impl = impl;
        this.context = context;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        Bundle data = new Bundle();
        data.putInt("face", face.toString().hashCode());
        data.putInt("impl", impl.hashCode());
        data.putString("method", method.getName());
        packParameters(data, args);
        packTypes(data, method.getParameterTypes());

        Intent service = new Intent(context, DecouplexService.class);
        service.setAction(ACTION);
        service.putExtras(data);

        context.startService(service);

        if (method.getReturnType().isPrimitive())
            return 0;
        return null;
    }
}
