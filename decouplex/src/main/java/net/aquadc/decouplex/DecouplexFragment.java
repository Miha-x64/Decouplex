package net.aquadc.decouplex;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.support.v4.content.LocalBroadcastManager;

/**
 * Created by miha on 24.05.16.
 * similar to DecouplexActivity excluding Context operations
 */
@TargetApi(11)
public abstract class DecouplexFragment extends Fragment {

    private BroadcastReceiver decouplexReceiver;

    @Override
    public void onStart() {
        super.onStart();
        if (decouplexReceiver == null) {
            decouplexReceiver = new DecouplexReceiver(this);
        }

        LocalBroadcastManager
                .getInstance(getActivity())
                .registerReceiver(decouplexReceiver, DecouplexReceiver.createFilter());
    }

    @Override
    public void onStop() {
        LocalBroadcastManager
                .getInstance(getActivity())
                .unregisterReceiver(decouplexReceiver);
        super.onStop();
    }
}
