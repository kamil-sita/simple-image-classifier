package pl.ksitarski.imageclassifier.neuralnetwork;

import pl.ksitarski.imageclassifier.neuralnetwork.math.Matrix;
import pl.ksitarski.imageclassifier.neuralnetwork.math.MatrixMath;
import pl.ksitarski.imageclassifier.neuralnetwork.math.NLMath;
import pl.ksitarski.imageclassifier.othertools.IO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static pl.ksitarski.imageclassifier.neuralnetwork.LoggerSettings.*;

/**
 * Neural network that consists of multiple neuron layers.
 * Defines its structure, can be trained upon and can be used to deduce outputs based on given inputs.
 */
public class NeuralNetwork {

    private double initialLearningRate;

    private List<NeuralLayer> neuralLayers = new ArrayList<>();
    private double adjustLearningRate;
    private boolean isManaged;
    private double maxDeviation;
    private List<Double> resultsHistory = new ArrayList<>();
    private List<NeuralLayer> backupNeuralLayers = new ArrayList<>();

    private transient WorkingInterface workingInterface;
    private int totalIterations = 0;

    /**
     * Default constructor.
     * @param inputs number of inputs
     * @param outputs number of outputs
     * @param neuronsInLayer number of neurons per layer
     * @param layerCount number of layers
     * @param initialLearningRate initial learning rate for the network
     * @param isManaged whether network manages learning rate using its algorithm
     * @param adjustLearningRate adjustment to learning rate upon failing
     * @param maxDeviation maximum deviation for purposes of deviation calculation
     */
    public NeuralNetwork(int inputs, int outputs, int[] neuronsInLayer, int layerCount, double initialLearningRate, boolean isManaged, double adjustLearningRate, double maxDeviation) {
        layerCount = layerCount + 1;
        for (int i = 0; i < layerCount; i++) {
            NeuralLayer neuralLayer;
            if (i == 0) {
                neuralLayer = new NeuralLayer(inputs, neuronsInLayer[0]);
            } else if (i + 1 == layerCount) {
                neuralLayer = new NeuralLayer(neuronsInLayer[neuronsInLayer.length - 1], outputs);
            } else {
                neuralLayer = new NeuralLayer(neuronsInLayer[i - 1], neuronsInLayer[i]);
            }
            neuralLayers.add(neuralLayer);
        }
        this.initialLearningRate = initialLearningRate;
        this.adjustLearningRate = adjustLearningRate;
        this.isManaged = isManaged;
        this.maxDeviation = maxDeviation;
    }

    /**
     * Deduces output from given input.
     */
    public Matrix deduce(Matrix input) {
        Matrix it = input;

        for (NeuralLayer neuralLayer : neuralLayers) {
            it = propagate(it, neuralLayer);
        }
        return it;
    }


    private void generateBackup() {
        if (!isManaged) return;
        backupNeuralLayers = new ArrayList<>();
        for (NeuralLayer neuralLayer : neuralLayers) {
            backupNeuralLayers.add(new NeuralLayer(neuralLayer));
        }
    }

    private void restoreBackup() {
        if (!isManaged) return;
        getLogger().log("Restoring old neuronnet");
        neuralLayers = backupNeuralLayers;
    }

    public void setWorkingInterface(WorkingInterface workingInterface) {
        this.workingInterface = workingInterface;
    }

    public int getCategoriesCount() {
        return neuralLayers.get(neuralLayers.size() - 1).getWeights().getWidth();
    }

    public int getInputSize() {
        return neuralLayers.get(0).getWeights().getHeight();
    }

    /**
     * Simplified training mode for multistart.
     * @param inputs inputs for neural network
     * @param outputs outputs for neural network
     * @param thisId id of this multistart instance.
     * @return deviation of this multistart instance.
     */
    public double trainMultistart(Matrix inputs, Matrix outputs, int thisId) {
        double customLearningRate = initialLearningRate;
        for (int i = 0; i < 10; i++) {
            iterate(inputs, outputs, customLearningRate);
            if (i % 2 == 0) {
                getLogger().log(thisId + ": " + i + "/" + 10);
            }
        }
        double deviation = getDeviation(inputs, outputs, maxDeviation);
        resultsHistory.add(deviation);
        return deviation;
    }

    /**
     * Trains this neural network
     * @param inputs inputs for neural network
     * @param outputs output for neural network
     * @param learningStopConditionTarget end condition
     */
    public void train(Matrix inputs, Matrix outputs, LearningStopConditionTarget learningStopConditionTarget) {
        double lastDeviation = 10;
        double customLearningRate = initialLearningRate;
        int i = 0;
        int falling = 0;
        while (learningStopConditionTarget.canIterate(lastDeviation, i)) {
            if (workingInterface != null && !workingInterface.isContinueLearning()) {
                getLogger().log("Ending work because of stop request");
                printHistory();
                return;
            }

            iterate(inputs, outputs, customLearningRate);

            if (i % 10 == 0) { //logging and adjustments happen only every 10 iterations
                getLogger().clear();
                getLogger().log("Learning rate: " + (customLearningRate * 100000) + ", is managed: " + isManaged);
                getLogger().log("Iteration: " + i + " (global iterations: " + totalIterations + ")");
                learningStopConditionTarget.writeInfo();
                double deviation = getDeviation(inputs, outputs, maxDeviation);

                if (!isManaged && deviation > lastDeviation && adjustLearningRate != 1) { //simple, non algorithm based learning rate adjustment
                    customLearningRate *= adjustLearningRate;
                    getLogger().log("Deviation: " + deviation + ", Learning rate adjustment: " + (customLearningRate * 100000));
                } else {
                    getLogger().log("Deviation: " + deviation);
                }

                getLogger().log("Dev difference: " + (lastDeviation - deviation));
                double devDiff = (lastDeviation - deviation);
                double tmpLastDeviation = deviation;
                boolean usesOldValue = false;

                if (isManaged) { //algorithm adjusts learning rate
                    if (Math.abs(devDiff) < 0.0001) { //too small progress
                        if (devDiff < 0) { //and it is negative
                            falling++;
                        } else {
                            falling = 0;
                        }
                        if (falling == 0) {
                            customLearningRate *= 1.1;
                        }
                        if (falling > 3) { //too much negative progress
                            falling = 0;
                            restoreBackup();
                            customLearningRate *= 0.7;
                            usesOldValue = true;
                        } else {
                            lastDeviation = tmpLastDeviation;
                        }
                        getLogger().log("Deviation: " + deviation + ", Learning rate adjustment (changes too small): " + (customLearningRate * 100000));
                    } else if (devDiff < -0.0005) { //giant negative progress
                        usesOldValue = true;
                        restoreBackup();
                        customLearningRate *= 0.9;
                        getLogger().log("Deviation: " + deviation + ", Learning rate adjustment (negative learning): " + (customLearningRate * 100000));
                    } else { //progress is okay
                        lastDeviation = tmpLastDeviation;
                        generateBackup();
                    }
                }

                if (usesOldValue) { //reporting progress over time
                    resultsHistory.add(lastDeviation);
                } else {
                    resultsHistory.add(deviation);
                }
            }
            getLogger().logI("iteration: " + i);
            i++;
            totalIterations++;
        }
        printHistory();

    }

    private Matrix propagate(Matrix input, NeuralLayer neuralLayer) {
        return NLMath.applyFunction(MatrixMath.multiply(input, neuralLayer.getWeights()));
    }

    private void iterate(Matrix inputs, Matrix outputs, double customLearningRate) {
        List<Matrix> layerResults = new ArrayList<>();

        //forward propagation calculation
        Matrix lastOutputLayer = inputs;
        for (NeuralLayer layer : neuralLayers) {
            lastOutputLayer = propagate(lastOutputLayer, layer);
            layerResults.add(lastOutputLayer);
        }

        //error for last layer
        Matrix errorLast = MatrixMath.subtract(outputs, layerResults.get(layerResults.size() - 1));

        //backpropagation
        List<Matrix> reversedErrorLayers = new ArrayList<>();
        reversedErrorLayers.add(errorLast);
        for (int i = layerResults.size() - 2; i >= 0; i--) {
            NeuralLayer neuralLayer = neuralLayers.get(i + 1);
            errorLast = MatrixMath.multiply(errorLast, MatrixMath.transpose(neuralLayer.getWeights()));
            reversedErrorLayers.add(errorLast);
        }
        List<Matrix> errorLayers = new ArrayList<>(reversedErrorLayers);
        Collections.reverse(errorLayers);

        //modification with propagation
        Matrix propagatedInput = inputs;
        for (int i = 0; i < neuralLayers.size(); i++) {
            NeuralLayer layer = neuralLayers.get(i);
            Matrix tmpPropagate = propagate(propagatedInput, layer);
            Matrix deltaLayer = MatrixMath.scalarMultiply(errorLayers.get(i), NLMath.applyFunctionDerivative(tmpPropagate));
            Matrix adjustmentLayer = MatrixMath.multiply(MatrixMath.transpose(propagatedInput), deltaLayer);
            layer.adjustWeights(MatrixMath.scale(adjustmentLayer, customLearningRate));
            propagatedInput = propagate(propagatedInput, layer);
        }
    }

    private double getDeviation(Matrix inputs, Matrix outputs, double maxDeviation) {
        double deviation = 0;
        for (int i = 0; i < inputs.getHeight(); i++) {
            Matrix input = inputs.getRowAsMatrix(i);
            Matrix expectedOutput = outputs.getRowAsMatrix(i);
            Matrix actualOutput = deduce(input);
            deviation += MatrixMath.deviation(expectedOutput, actualOutput, maxDeviation);
        }
        return deviation / inputs.getHeight();
    }

    public NeuralLayer getFirstLayer() {
        return neuralLayers.get(0);
    }

    public NeuralLayer getLastLayer() {
        return neuralLayers.get(neuralLayers.size() - 1);
    }

    //prints history to logger and to csv file (in polish locale)
    private void printHistory() {
        getLogger().log("History");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < resultsHistory.size(); i++) {
            String locString = (i + 1) * 10 + "; " + resultsHistory.get(i);
            locString = locString.replace(".", ",");
            getLogger().log(locString);
            sb.append(locString).append("\r\n");
        }
        IO.toFile(sb.toString(), "lastGraph.csv");
    }

    /**
     * Interface that allows to define function that decided whether to safely stop algorithm.
     */
    @FunctionalInterface
    public interface WorkingInterface {
        /**
         * Whether neural network should still be learning.
         */
        boolean isContinueLearning();
    }


    /**
     * Defines ending condition for neural network training.
     */
    public static class LearningStopConditionTarget {
        public LearningStopConditionType learningStopConditionType;
        public double val; //target value, that can mean various things depending on selected LearningTargetType

        private double tmp = 10000; //used to hold last

        /**
         * Denotes whether neural network should still iterate depending on this ending condition.
         * @param deviation current deviation
         * @param iteration current iteration
         */
        public boolean canIterate(double deviation, int iteration) {
            switch (learningStopConditionType) {
                case ITERATION:
                    if (iteration <= val) {
                        return true;
                    }
                    break;
                case ERROR:
                    if (deviation >= val) {
                        return true;
                    }
                    break;
                case ERROR_DIFFERENCE:
                    if (tmp - deviation >= val) {
                        return true;
                    }
                    tmp = deviation;
                    break;
            }
            return false;
        }

        /**
         * Writes information about learning progress to logger
         */
        public void writeInfo() {
            switch (learningStopConditionType) {
                case ITERATION:
                    getLogger().log("of " + val);
                    break;
                case ERROR:
                    getLogger().log("until accuracy " + val);
                    break;
                case ERROR_DIFFERENCE:
                    getLogger().log("until target difference" + val);
                    break;
            }
        }
    }


    private int getLayerCount() {
        return neuralLayers.size();
    }


    public NeuralNetwork setInitialLearningRate(double initialLearningRate) {
        this.initialLearningRate = initialLearningRate;
        return this;
    }

    public NeuralNetwork setAdjustLearningRate(double adjustLearningRate) {
        this.adjustLearningRate = adjustLearningRate;
        return this;
    }

    public NeuralNetwork setManaged(boolean managed) {
        this.isManaged = managed;
        return this;
    }

    /**
     * Enum that describes ending conditions
     */
    public enum LearningStopConditionType {
        /**
         * Ending condition that is dependent on iteration count (in current learning batch, not all-time)
         */
        ITERATION,
        /**
         * Ending condition that is dependent on error value (deviation value)
         */
        ERROR,
        /**
         * Ending condition that is dependent on difference between error (deviation) values in two subsequent iterations
         */
        ERROR_DIFFERENCE
    }
}
