package pl.ksitarski.imageclassifier.neuralnetwork.math;

import java.util.Random;

/**
 * Contains neural network related math functions.
 */
public class NLMath {

    private static Random random = new Random();

    /**
     * Returns random double value from given range.
     */
    public static double getDoubleFromRange(double min, double max) {
        return (max - min) * random.nextDouble() + min;
    }

    /**
     * Calculates value for activation function.
     */
    public static double activationFunction(double v) {
        return 1.0 / (1.0 + Math.exp(-v));
    }

    /**
     * Calculates value for derivative of activation function.
     */
    public static double activationFunctionDerivative(double v) {
        return v * (1 - v);
    }

    /**
     * Applies activation function to entire matrix.
     */
    public static Matrix applyFunction(Matrix matrix) {
        Matrix result = new Matrix(matrix.getHeight(), matrix.getWidth());

        for (int y = 0; y < matrix.getHeight(); y++) {
            for (int x = 0; x < matrix.getWidth(); x++) {
                result.set(x, y, activationFunction(matrix.get(x, y)));
            }
        }

        return result;
    }

    /**
     * Applies derivative of activation function to entire matrix.
     */
    public static Matrix applyFunctionDerivative(Matrix matrix) {
        Matrix result = new Matrix(matrix.getHeight(), matrix.getWidth());

        for (int y = 0; y < matrix.getHeight(); y++) {
            for (int x = 0; x < matrix.getWidth(); x++) {
                result.set(x, y, activationFunctionDerivative(matrix.get(x, y)));
            }
        }

        return result;
    }
}
