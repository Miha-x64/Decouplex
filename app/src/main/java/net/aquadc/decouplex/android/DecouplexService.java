package net.aquadc.decouplex.android;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

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
        int faceCode = bun.getInt("face");
        int implCode = bun.getInt("impl");
        String methodName = bun.getString("method");

        try {
            Class<?> face = face(faceCode);
            Object impl = impl(implCode);

            Class<?>[] types = unpackTypes(bun);
            Method method = face.getDeclaredMethod(methodName, types);
            Object result = method.invoke(impl, unpackParameters(bun, types.length));

            Bundle answer = new Bundle();
            answer.putInt("face", faceCode);
            answer.putString("method", methodName);
            put(answer, "result", result);

            Intent resp = new Intent(ACTION);
            resp.putExtras(answer);
            LocalBroadcastManager
                    .getInstance(this)
                    .sendBroadcast(resp);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
