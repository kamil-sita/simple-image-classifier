package pl.ksitarski.imageclassifier.neuralnetwork.math;

/**
 * Contains matrix math related functions.
 */
public class MatrixMath {

    /**
     * Perform multiplication of two matrices and returns result.
     */
    public static Matrix multiply(Matrix a, Matrix b) {
        Matrix result = new Matrix(a.getHeight(), b.getWidth());

        int iit = a.getHeight();
        int jit = a.getWidth();
        int xit = b.getHeight();
        int kit = b.getWidth();

        for (int i = 0; i < iit; i++) {
            for (int k = 0; k < kit; k++) {

                double sum = 0;
                for (int j = 0; j < jit; j++) {
                    sum += a.get(j, i) * b.get(k, j);
                }

                result.set(k, i, sum);

            }
        }

        return result;
    }

    /**
     * Substracts two matrices and returns result.
     */
    public static Matrix subtract(Matrix a, Matrix b) {
        Matrix result = new Matrix(a.getHeight(), a.getWidth());

        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x <  a.getWidth(); x++) {
                result.set(x, y, a.get(x, y) - b.get(x, y));
            }
        }

        return result;
    }

    /**
     * Multiplies values in matrix A by scalar values in matrix B.
     */
    public static Matrix scalarMultiply(Matrix a, Matrix b) {
        a.checkEqual(b);

        Matrix result = new Matrix(a.getHeight(), a.getWidth());

        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x <  a.getWidth(); x++) {
                result.set(x, y, a.get(x, y) * b.get(x, y));
            }
        }

        return result;
    }

    /**
     * Transposes given matrix.
     */
    public static Matrix transpose(Matrix a) {
        Matrix result = new Matrix(a.getWidth(), a.getHeight());

        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x <  a.getWidth(); x++) {
                result.set(y, x, a.get(x, y));
            }
        }

        return result;
    }

    /**
     * Scales values in matrix by given scalar value.
     */
    public static Matrix scale(Matrix a, double value) {
        Matrix result = new Matrix(a.getHeight(), a.getWidth());

        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x <  a.getWidth(); x++) {
                result.set(x, y, a.get(x, y) * value);
            }
        }

        return result;
    }

    /**
     * Calculates deviation for given one dimensional matrix.
     */
    public static double deviation(Matrix expected, Matrix result, double maxDeviation) {
        double dev = 0;
        for (int i = 0; i < expected.getWidth(); i++) {
            dev += Math.pow(expected.get(i, 0) - result.get(i, 0), 2);
        }
        return Math.sqrt(dev/maxDeviation);
    }
}
