package net.aquadc.decouplex.example;

import java.math.BigInteger;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Created by miha on 15.05.16.
 *
 */
public class LongRunningTaskImpl implements LongRunningTask {
    @Override
    public BigInteger calculateSomethingBig() {
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            //ok
        }
        Random r = new Random();
        return BigInteger.probablePrime(256, r)
                .multiply(BigInteger.probablePrime(256, r))
                .multiply(BigInteger.probablePrime(256, r))
                .multiply(BigInteger.probablePrime(256, r))
                .multiply(BigInteger.probablePrime(256, r))
                .multiply(BigInteger.probablePrime(256, r));
    }
}
