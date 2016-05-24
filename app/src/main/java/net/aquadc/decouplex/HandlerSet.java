package net.aquadc.decouplex;

/**
 * Created by miha on 24.05.16.
 *
 */
class HandlerSet {

    final Handlers classifiedResultHandlers;
    final Handlers resultHandlers;
    final Handlers classifiedErrorHandlers;
    final Handlers errorHandlers;

    public HandlerSet(Handlers classifiedResultHandlers, Handlers resultHandlers,
                      Handlers classifiedErrorHandlers, Handlers errorHandlers) {
        this.classifiedResultHandlers = classifiedResultHandlers;
        this.resultHandlers = resultHandlers;
        this.classifiedErrorHandlers = classifiedErrorHandlers;
        this.errorHandlers = errorHandlers;
    }
}
