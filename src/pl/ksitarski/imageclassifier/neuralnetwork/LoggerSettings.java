package pl.ksitarski.imageclassifier.neuralnetwork;

/**
 * Contains information about logger.
 */
public class LoggerSettings {
    private static Logger logger;

    /**
     * Returns current logger. Might lazy initialize the default logger, if no logger is defined.
     */
    public static Logger getLogger() {
        if (logger == null) {
            logger = new Logger() {
                @Override
                public void log(String msg) {
                    System.out.println(msg);
                }

                @Override
                public void logI(String msg) {

                }

                @Override
                public void clear() {

                }
            };
        }
        return logger;
    }

    /**
     * Sets logger.
     */
    public static void setLogger(Logger logger) {
        LoggerSettings.logger = logger;
    }
}
