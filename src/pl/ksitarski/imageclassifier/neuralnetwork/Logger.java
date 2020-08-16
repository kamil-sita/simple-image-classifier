package pl.ksitarski.imageclassifier.neuralnetwork;

/**
 * Interface for loggers.
 */
public interface Logger {
    /**
     * Logs normal information.
     */
    void log(String msg);

    /**
     * Logs minor information.
     */
    void logI(String msg);

    /**
     * Informs logger that it might clear its output
     */
    void clear();
}
