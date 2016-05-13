package net.aquadc.decouplex;

import android.content.Context;
import android.content.Intent;

import net.aquadc.decouplex.android.DecouplexService;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import static net.aquadc.decouplex.Decouplex.ACTION;

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
        Intent service = new Intent(context, DecouplexService.class);
        service.setAction(ACTION);
        service.putExtra("face", face.toString().hashCode());
        service.putExtra("impl", impl.hashCode());
        service.putExtra("method", method.getName());
        // todo: marshall args
        context.startService(service);

        if (method.getReturnType().isPrimitive())
            return 0;
        return null;
    }
}
