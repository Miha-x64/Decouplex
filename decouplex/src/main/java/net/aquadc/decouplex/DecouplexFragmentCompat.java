package net.aquadc.decouplex;

import android.content.BroadcastReceiver;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;

/**
 * Created by miha on 24.05.16.
 * absolutely identical to DecouplexFragment
 */
public abstract class DecouplexFragmentCompat extends Fragment {

    private BroadcastReceiver decouplexReceiver;

    @Override
    public void onStart() {
        super.onStart();
        if (decouplexReceiver == null) {
            decouplexReceiver = new DecouplexReceiver(this);
        }

        LocalBroadcastManager
                .getInstance(getActivity())
                .registerReceiver(decouplexReceiver, DecouplexReceiver.createFilter(this));
    }

    @Override
    public void onStop() {
        LocalBroadcastManager
                .getInstance(getActivity())
                .unregisterReceiver(decouplexReceiver);
        super.onStop();
    }

}
