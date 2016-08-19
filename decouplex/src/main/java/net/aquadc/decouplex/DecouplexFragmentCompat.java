package net.aquadc.decouplex;

import android.support.v4.app.Fragment;

/**
 * Created by miha on 24.05.16.
 * absolutely identical to DecouplexFragment
 */
public abstract class DecouplexFragmentCompat extends Fragment {

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
