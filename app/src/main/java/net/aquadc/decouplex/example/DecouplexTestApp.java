package net.aquadc.decouplex.example;

import android.app.Application;

/**
 * Created by miha on 14.05.16.
 *
 */
public class DecouplexTestApp extends Application {

    private static DecouplexTestApp instance;

    public DecouplexTestApp() {
        instance = this;
    }

    public static DecouplexTestApp getInstance() {
        return instance;
    }

}
