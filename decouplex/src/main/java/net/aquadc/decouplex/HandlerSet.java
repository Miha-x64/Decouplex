package net.aquadc.decouplex;

import java.lang.reflect.Method;

/**
 * Created by miha on 24.05.16.
 *
 */
final class HandlerSet {

    private final Handlers classifiedResultHandlers;
    private final Handlers resultHandlers;
    private final Handlers classifiedErrorHandlers;
    private final Handlers errorHandlers;

    HandlerSet(Handlers classifiedResultHandlers, Handlers resultHandlers,
                      Handlers classifiedErrorHandlers, Handlers errorHandlers) {
        this.classifiedResultHandlers = classifiedResultHandlers;
        this.resultHandlers = resultHandlers;
        this.classifiedErrorHandlers = classifiedErrorHandlers;
        this.errorHandlers = errorHandlers;
    }

    /**
     * find handler for the method result
     * @param face          the interface on which the method has been invoked on
     * @param methodName    the name of method that has been invoked
     * @param forResult     search result handlers; search error handlers if false
     * @param handlerClass  class where handlers coming from
     * @return Method to handle response
     */
    static Method forMethod(Class face, String methodName, boolean forResult, Class<?> handlerClass) {
        HandlerSet h = Handlers.forClass(face, handlerClass);
        Handlers classified = forResult ? h.classifiedResultHandlers : h.classifiedErrorHandlers;
        Method handler = classified.forName(methodName);
        if (handler != null)
            return handler;

        Handlers unclassified = forResult ? h.resultHandlers : h.errorHandlers;
        handler = unclassified.forName(methodName);
        if (handler != null)
            return handler;

        throw new RuntimeException("handler for result of method '" + methodName +
                "' not found in class " + handlerClass.getSimpleName() + '.');
    }

    @Override
    public String toString() {
        return "HandlerSet(classifiedResultHandlers: " + classifiedResultHandlers + ", " +
                "resultHandlers: " + resultHandlers + ", " +
                "classifiedErrorHandlers: " + classifiedErrorHandlers + ", " +
                "errorHandlers: " + errorHandlers + ')';
    }
}
