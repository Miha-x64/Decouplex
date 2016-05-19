package net.aquadc.decouplex;

import android.app.Activity;

import net.aquadc.decouplex.example.LongRunningTask;
import net.aquadc.decouplex.example.LongRunningTaskImpl;
import net.aquadc.decouplex.example.SampleActivity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.math.BigInteger;

import static org.robolectric.Robolectric.*;
import static org.robolectric.Shadows.*;

/**
 * Created by miha on 16.05.16.
 *
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class DecouplexTest {

    @Test
    public void factTest() {
        Activity activity = setupActivity(SampleActivity.class);
        new DecouplexBuilder<>(LongRunningTask.class, new LongRunningTaskImpl(), SampleActivity.class)
                .create(activity).calculateFactorial(BigInteger.valueOf(1));

        shadowOf(activity).getNextStartedService();
    }

}