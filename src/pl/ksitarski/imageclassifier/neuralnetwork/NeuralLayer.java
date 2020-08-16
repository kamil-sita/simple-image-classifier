package pl.ksitarski.imageclassifier.neuralnetwork;

import pl.ksitarski.imageclassifier.neuralnetwork.math.Matrix;

/**
 * Thin abstraction wrapper over matrix, represents neuron network layer.
 */
public class NeuralLayer {
    private final int inputCount;
    private final int neuronCount;

    private final Matrix weights;

    /**
     * Default constructor.
     * @param inputCount number of inputs per neuron
     * @param neuronCount number of neurons
     */
    public NeuralLayer(int inputCount, int neuronCount) {
        this.inputCount = inputCount;
        this.neuronCount = neuronCount;
        weights = new Matrix(inputCount, neuronCount).setRandom();
    }

    /**
     * Deep copy constructor.
     * @param other neural layer to clone
     */
    public NeuralLayer(NeuralLayer other) {
        this.inputCount = other.inputCount;
        this.neuronCount = other.neuronCount;
        this.weights = new Matrix(other.weights);
    }

    public void adjustWeights(Matrix adjustment) {
        weights.add(adjustment);
    }

    public Matrix getWeights() {
        return weights;
    }

    public int getInputCount() {
        return inputCount;
    }

    public int getNeuronCount() {
        return neuronCount;
    }

    @Override
    public String toString() {
        return "NeuronLayer{" +
                "inputCount=" + inputCount +
                ", neuronCount=" + neuronCount +
                ", weights=" + weights +
                '}';
    }

}
