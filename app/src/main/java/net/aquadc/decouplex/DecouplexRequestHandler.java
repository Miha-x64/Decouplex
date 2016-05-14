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

    private final Context context;

    private final Class<FACE> face;
    private final int implCode;

    private final int resultProcessorCode;
    private final int resultAdapterCode;
    private final int errorProcessorCode;
    private final int errorAdapterCode;

    DecouplexRequestHandler(Context context,
                            Class<FACE> face, int implCode,
                            int resultProcessorCode, int resultAdapterCode,
                            int errorProcessorCode, int errorAdapterCode) {
        this.context = context;

        this.face = face;
        this.implCode = implCode;

        this.resultProcessorCode = resultProcessorCode;
        this.resultAdapterCode = resultAdapterCode;

        this.errorProcessorCode = errorProcessorCode;
        this.errorAdapterCode = errorAdapterCode;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        Bundle data = new Bundle();
        data.putString("face", face.getCanonicalName());
        data.putInt("impl", implCode);
        data.putString("method", method.getName());

        if (resultProcessorCode != 0)
            data.putInt("resultProcessor", resultProcessorCode);

        if (resultAdapterCode != 0)
            data.putInt("resultAdapter", resultAdapterCode);

        if (errorProcessorCode != 0)
            data.putInt("errorProcessor", errorProcessorCode);

        if (errorAdapterCode != 0)
            data.putInt("errorAdapter", resultAdapterCode);

        Class[] types = method.getParameterTypes();
        packTypes(data, types);
        packParameters(data, types, args);

        Intent service = new Intent(context, DecouplexService.class);
        service.setAction(ACTION_EXEC);
        service.putExtras(data);

        context.startService(service);

        if (method.getReturnType().isPrimitive())
            return 0;
        return null;
    }
}
