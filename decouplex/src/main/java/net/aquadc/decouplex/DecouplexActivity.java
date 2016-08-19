package net.aquadc.decouplex;

import android.support.v7.app.AppCompatActivity;

/**
 * Created by miha on 14.05.16.
 *
 */
public abstract class DecouplexActivity extends AppCompatActivity {

    private DecouplexReceiver decouplexReceiver;

    @Override
    protected void onStart() {
        super.onStart();
        if (decouplexReceiver == null)
            decouplexReceiver = new DecouplexReceiver(this);
        decouplexReceiver.register();
    }

    @Override
    protected void onStop() {
        decouplexReceiver.unregister();
        super.onStop();
    }
}
