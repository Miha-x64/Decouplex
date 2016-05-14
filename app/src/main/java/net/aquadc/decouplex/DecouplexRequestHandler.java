package net.aquadc.decouplex;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import net.aquadc.decouplex.adapter.ResultAdapter;
import net.aquadc.decouplex.android.DecouplexService;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import static net.aquadc.decouplex.Decouplex.*;

/**
 * Created by miha on 14.05.16.
 *
 */
public class DecouplexRequestHandler<FACE> implements InvocationHandler {

    private final Context context;

    private final Class<FACE> face;
    private final int implCode;

    private int resultAdapterCode;

    DecouplexRequestHandler(Context context,
                            Class<FACE> face, int implCode,
                            int resultAdapterCode) {
        this.context = context;

        this.face = face;
        this.implCode = implCode;

        this.resultAdapterCode = resultAdapterCode;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        Bundle data = new Bundle();
        data.putString("face", face.getCanonicalName());
        data.putInt("impl", implCode);
        data.putString("method", method.getName());
        if (resultAdapterCode != 0)
            data.putInt("resultAdapter", resultAdapterCode);

        packTypes(data, method.getParameterTypes());
        packParameters(data, args);

        Intent service = new Intent(context, DecouplexService.class);
        service.setAction(ACTION);
        service.putExtras(data);

        context.startService(service);

        if (method.getReturnType().isPrimitive())
            return 0;
        return null;
    }
}
