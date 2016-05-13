package net.aquadc.decouplex.example;

import java.util.Random;

/**
 * Created by miha on 14.05.16.
 *
 */
public class SampleTaskImpl implements SampleTask {

    @Override
    public int getRandom() {
        return new Random(System.nanoTime()).nextInt();
    }
}
