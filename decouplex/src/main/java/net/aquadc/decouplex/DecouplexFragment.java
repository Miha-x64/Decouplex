package net.aquadc.decouplex;

import android.annotation.TargetApi;
import android.app.Fragment;

/**
 * Created by miha on 24.05.16.
 * similar to DecouplexActivity excluding Context operations
 */
@TargetApi(11)
public abstract class DecouplexFragment extends Fragment {

    private DecouplexReceiver decouplexReceiver;

    @Override
    public void onStart() {
        super.onStart();
        if (decouplexReceiver == null)
            decouplexReceiver = new DecouplexReceiver(this);
        decouplexReceiver.register();
    }

    @Override
    public void onStop() {
        decouplexReceiver.unregister();
        super.onStop();
    }
}
