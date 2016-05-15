package net.aquadc.decouplex.android;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;

import net.aquadc.decouplex.Decouplex;

import static net.aquadc.decouplex.Decouplex.ACTION_EXEC;

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
        Decouplex<?> decouplex = Decouplex.find(req.getInt("id"));
        decouplex.dispatchRequest(this, req);
    }
}
