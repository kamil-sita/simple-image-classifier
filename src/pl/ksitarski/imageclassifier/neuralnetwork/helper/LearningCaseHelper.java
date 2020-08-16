package pl.ksitarski.imageclassifier.neuralnetwork.helper;

import pl.ksitarski.imageclassifier.neuralnetwork.math.Matrix;

/**
 * Builder-like structure that helps in creation of input and output data for classifier/neural network.
 */
public class LearningCaseHelper {
    private Matrix input;
    private Matrix output;
    private int testCases;
    private int currentIterationInput = 0;
    private int currentIterationOutput = 0;
    private int inputs;
    private int outputs;

    /**
     * Default constructor.
     * @param testCases number of inputs/outputs
     * @param inputs size of inputs
     * @param outputs size of outputs
     */
    public LearningCaseHelper(int testCases, int inputs, int outputs) {
        this.testCases = testCases;
        this.inputs = inputs;
        this.outputs = outputs;
        input = new Matrix(testCases, inputs);
        output = new Matrix(testCases, outputs);
    }

    /**
     * Sets category for current iteration.
     */
    public void setOutputDataClassifier(int categoryId) {
        for (int x = 0; x < output.getWidth(); x++) {
            output.set(x, currentIterationOutput, x == categoryId ? 1 : 0);
        }
        currentIterationOutput++;
    }

    /**
     * Trims this structure to not overuse space.
     */
    public void trim() {
        if (currentIterationInput != currentIterationOutput) {
            throw new IllegalArgumentException("currentIterationInput and currentIterationOutput must be equal for this operation");
        }
        if (getActualHeight() == input.getHeight()) {
            return;
        }
        Matrix newInput = new Matrix(getActualHeight(), input.getWidth());
        Matrix newOutput = new Matrix(getActualHeight(), output.getWidth());
        System.out.println("Trimmer. Height: " + input.getHeight() + ", actual height: " + getActualHeight());
        for (int y = 0; y < getActualHeight(); y++) {
            newInput.setRow(y, input.getRow(y));
            newOutput.setRow(y, output.getRow(y));
        }
        input = newInput;
        output = newOutput;
    }

    public int getActualHeight() {
        return currentIterationInput;
    }

    public int getInputs() {
        return inputs;
    }

    public int getOutputs() {
        return outputs;
    }

    public int getTestCases() {
        return testCases;
    }


    public void setInputData(double... data) {
        input.setRow(currentIterationInput, data);
        currentIterationInput++;
    }

    public void setOutputData(double... data) {
        output.setRow(currentIterationOutput, data);
        currentIterationOutput++;
    }

    public Matrix getInput() {
        return input;
    }

    public Matrix getOutput() {
        return output;
    }
}
