package net.aquadc.decouplex.example;

/**
 * Created by miha on 14.05.16.
 *
 */
public class SampleServiceImpl implements SampleService {

    @Override
    public int getSquare(int n) {
        return n*n;
    }
}
