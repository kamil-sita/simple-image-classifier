package pl.ksitarski.imageclassifier.neuralnetwork.math;

import pl.ksitarski.imageclassifier.neuralnetwork.LoggerSettings;

import java.util.Arrays;

/**
 * Two dimensional matrix.
 */
public class Matrix {
    private int height;
    private int width;

    private double[][] matrix;

    /**
     * Default constructor;
     * @param height height of the matrix
     * @param width width of the matrix
     */
    public Matrix(int height, int width) {
        this.height = height;
        this.width = width;

        matrix = new double[height][width];
    }

    /**
     * Deep copy constructor
     * @param other matrix to be copied
     */
    public Matrix(Matrix other) {
        this.height = other.height;
        this.width = other.width;
        this.matrix = deepCopy(other.matrix);
    }

    /**
     * Sets all values of this matrix to random values.
     */
    public Matrix setRandom() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                matrix[y][x] = NLMath.getDoubleFromRange(-1, 1);
            }
        }
        return this;
    }

    /**
     * Sets given value in matrix at given position.
     */
    public Matrix set(int x, int y, double value) {
        matrix[y][x] = value;
        return this;
    }

    /**
     * Sets values of given row.
     */
    public Matrix setRow(int y, double... values) {
        int index = 0;
        for (double val : values) {
            if (y >= height) {
                throw new IllegalArgumentException(y + " (y) is equal or bigger than "  + height + "(height)");
            }
            if (index >= width) {
                throw new IllegalArgumentException(index + " (x) is equal or bigger than "  + width + "(width), which is strange");
            }
            matrix[y][index] = val;
            index++;
        }
        return this;
    }

    /**
     * Returns value of this matrix at given positon.
     */
    public double get(int x, int y) {
        return matrix[y][x];
    }

    /**
     * Returns values of given row.
     */
    public double[] getRow(int y) {
        return matrix[y];
    }

    /**
     * Returns values of given row as matrix.
     */
    public Matrix getRowAsMatrix(int y) {
        Matrix m = new Matrix(1, getWidth());
        for (int i = 0; i < getWidth(); i++) {
            m.set(i, 0, get(i, y));
        }
        return m;
    }

    /**
     * Returns height of this matrix.
     */
    public int getHeight() {
        return height;
    }

    /**
     * Returns width of this matrix.
     */
    public int getWidth() {
        return width;
    }

    /**
     * Writes selected row to the logger.
     */
    public void writeRow(int y) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < width; i++) {
            sb.append(String.format("%.3f", get(i, y)));
            if (y + 1 != width) {
                sb.append(",  ");
            }
        }
        LoggerSettings.getLogger().log(sb.toString());
    }

    /**
     * Returns id of biggest value in given row.
     */
    public int getBiggestIdInRow(int y) {
        double biggest = Double.MIN_VALUE;
        int biggestId = -1;
        for (int x = 0; x < width; x++) {
            double val = get(x, y);
            if (val > biggest) {
                biggest = val;
                biggestId = x;
            }
        }
        return biggestId;
    }

    /**
     * Adds value of this matrix to another matrix.
     */
    public void add(Matrix m) {
        checkEqual(m);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                matrix[y][x] += m.matrix[y][x];
            }
        }
    }


    private String getSizeArray() {
        return "[w: " + getWidth() + " x h: " + getHeight() + "]";
    }

    /**
     * Performs a check between two matrix for width and height equality, and throws exception
     * if sizes are not equal.
     */
    public void checkEqual(Matrix b) {

        if (this.getWidth() != b.getWidth()) {
            throw new IllegalArgumentException("a width differs from b width. Sizes: a: " + this.getSizeArray() + ", b: " + b.getSizeArray());
        }
        if (this.getHeight() != b.getHeight()) {
            throw new IllegalArgumentException("a height differs from b height. Sizes: a: " + this.getSizeArray() + ", b: " + b.getSizeArray());
        }

    }

    @Override
    public String toString() {
        return "Matrix{" +
                "height=" + height +
                ", width=" + width +
                '}';
    }


    private static double[][] deepCopy(double[][] original) {
        if (original == null) {
            return null;
        }

        final double[][] result = new double[original.length][];
        for (int i = 0; i < original.length; i++) {
            result[i] = Arrays.copyOf(original[i], original[i].length);
        }
        return result;
    }
}
