package net.aquadc.decouplex.android;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import net.aquadc.decouplex.adapter.ErrorProcessor;
import net.aquadc.decouplex.adapter.ResultProcessor;

import java.lang.reflect.Method;

import static net.aquadc.decouplex.Decouplex.*;

/**
 * Created by miha on 14.05.16.
 *
 */
public class DecouplexService extends IntentService {

    public DecouplexService() {
        super(DecouplexService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (!ACTION_EXEC.equals(intent.getAction()))
            return;

        Bundle req = intent.getExtras();
        String faceName = req.getString("face");
        String methodName = req.getString("method");
        int implCode = req.getInt("impl");

        Object impl = impl(implCode);
        ResultProcessor resultProcessor = resultProcessor(req.getInt("resultProcessor"));

        Class<?> face;
        Class<?>[] types;
        Method method;
        Object[] params;
        try {
            face = Class.forName(faceName);

            types = unpackTypes(req);
            method = face.getDeclaredMethod(methodName, types);
            params = unpackParameters(req, types.length);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            Object result = method.invoke(impl, params);

            Bundle resp = new Bundle();
            resp.putString("face", faceName);
            resp.putString("method", methodName);
            resp.putInt("resultAdapter", req.getInt("resultAdapter"));

            if (resultProcessor == null) {
                put(resp, "result", method.getReturnType(), result);
            } else {
                resultProcessor.process(resp, face, method, params, result);
            }

            Intent res = new Intent(ACTION_RESULT);
            res.putExtras(resp);
            LocalBroadcastManager
                    .getInstance(this)
                    .sendBroadcast(res);
        } catch (Exception e) {
            Bundle resp = new Bundle();
            resp.putBundle("request", req);
            put(resp, "exception", null, e);

            ErrorProcessor processor = errorProcessor(req.getInt("errorProcessor"));
            if (processor != null) {
                processor.process(resp, face, method, params, e);
            }

            Intent err = new Intent(ACTION_ERR);
            err.putExtras(resp);
            LocalBroadcastManager
                    .getInstance(this)
                    .sendBroadcast(err);
        }
    }
}
