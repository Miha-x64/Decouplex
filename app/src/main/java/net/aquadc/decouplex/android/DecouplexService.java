package net.aquadc.decouplex.android;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import net.aquadc.decouplex.adapter.ResultAdapter;

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
        if (!ACTION.equals(intent.getAction()))
            return;

        Bundle bun = intent.getExtras();
        String faceName = bun.getString("face");
        String methodName = bun.getString("method");
        int implCode = bun.getInt("impl");

        Object impl = impl(implCode);
        ResultAdapter resultAdapter = resultAdapter(bun.getInt("resultAdapter"));

        try {
            Class<?> face = Class.forName(faceName);

            Class<?>[] types = unpackTypes(bun);
            Method method = face.getDeclaredMethod(methodName, types);
            Object[] params = unpackParameters(bun, types.length);
            Object result = method.invoke(impl, params);

            Bundle answer = new Bundle();
            answer.putString("face", faceName);
            answer.putString("method", methodName);

            if (resultAdapter == null) {
                put(answer, "result", result);
            } else {
                resultAdapter.processResult(answer, face, method, params, result);
            }

            Intent resp = new Intent(ACTION);
            resp.putExtras(answer);
            LocalBroadcastManager
                    .getInstance(this)
                    .sendBroadcast(resp);
        } catch (Exception e) {
            Log.e("Decouplex service", "exception while executing " + methodName + " on " + impl, e);
            throw new RuntimeException(e);
            // TODO: broadcasting exceptions
        }
    }
}
