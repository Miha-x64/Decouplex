package net.aquadc.decouplex;

import android.os.Bundle;

/**
 * Created by miha on 14.05.16.
 *
 */
public interface Packer<T> {
    void put(Bundle bundle, String key, T value);
}
