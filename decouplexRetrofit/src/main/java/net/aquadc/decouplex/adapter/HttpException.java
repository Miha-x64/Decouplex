package net.aquadc.decouplex.adapter;

import java.io.IOException;

/**
 * Created by miha on 15.05.16.
 *
 */
public final class HttpException extends IOException {

    public final int code;
    public final String message;

    public HttpException(int code, String message) {
        this.code = code;
        this.message = message;
    }

}
