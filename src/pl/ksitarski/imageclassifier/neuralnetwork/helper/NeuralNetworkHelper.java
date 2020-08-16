package pl.ksitarski.imageclassifier.neuralnetwork.helper;

import pl.ksitarski.imageclassifier.neuralnetwork.NeuralNetwork;
import pl.ksitarski.imageclassifier.neuralnetwork.math.Matrix;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import static pl.ksitarski.imageclassifier.neuralnetwork.LoggerSettings.*;

/**
 * Wrapper/decorator on top of neural network or many networks, in case some of them are trained using multistart.
 */
public class NeuralNetworkHelper {
    private NeuralNetwork bestNet;
    private List<NeuralNetwork> neuralNetworkList = new ArrayList<>();
    private final int multistart;
    private boolean startComplete = false;

    /**
     * Constructor
     * @param layers number of layers
     * @param neuronsInLayer number of neurons per layer
     * @param inputs input count
     * @param outputs output count
     * @param learningRate learning rate
     * @param adjustLearningRate adjustment of learning rate
     * @param maxDeviation maximum deviation, for error calculcation
     * @param backup whether learning should be managed
     * @param multistart multistart thread count or 1
     */
    public NeuralNetworkHelper(int layers, int[] neuronsInLayer, int inputs, int outputs, double learningRate, double adjustLearningRate, double maxDeviation, boolean backup, int multistart) {
        if (multistart == 1) {
            bestNet = new NeuralNetwork(inputs, outputs, neuronsInLayer, layers, learningRate, backup, adjustLearningRate, maxDeviation);
            startComplete = true;
        } else {
            for (int i = 0; i < multistart; i++) {
                neuralNetworkList.add(new NeuralNetwork(inputs, outputs, neuronsInLayer, layers, learningRate, backup, adjustLearningRate, maxDeviation));
            }
        }
        this.multistart = multistart;
    }

    /**
     * Trains underlying network (and might start multistart) using given strategy.
     */
    public void train(LearningCaseHelper learningCaseHelper, NeuralNetwork.LearningStopConditionTarget learningStopConditionTarget) {
        checkVariables(learningCaseHelper);
        if (!startComplete) {
            selectBestNetwork(learningCaseHelper);
            startComplete = true;
        }
        bestNet.train(learningCaseHelper.getInput(), learningCaseHelper.getOutput(), learningStopConditionTarget);
    }

    /**
     * Deduces output based on given input matrix
     */
    public Matrix deduce(Matrix input) {
        return bestNet.deduce(input);
    }

    /**
     * Selects best network based on multistart process.
     */
    private void selectBestNetwork(LearningCaseHelper learningCaseHelper) {
        Executor executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        double[] accuracy = new double[multistart];
        AtomicBoolean failure = new AtomicBoolean(false);

        //multiple threads are launched

        Semaphore semaphore = new Semaphore(0);

        getLogger().log("multistart for " + multistart + " start and " + Runtime.getRuntime().availableProcessors() + " threads");
        for (int i = 0; i < multistart; i++) {
            int finalI = i;
            executor.execute(() -> {
                try {
                    double tmp = neuralNetworkList.get(finalI).trainMultistart(learningCaseHelper.getInput(), learningCaseHelper.getOutput(), finalI);
                    accuracy[finalI] = tmp;
                    getLogger().log("multistart finished for id " + finalI + " with accuracy " + tmp);
                } catch (Exception e) {
                    failure.set(true);
                    getLogger().log("Failed to iterate. Check if your settings are correct.");
                    getLogger().log(e.toString());
                } finally {
                    semaphore.release();

                }
            });
        }

        //waiting for all threads to finish
        semaphore.acquireUninterruptibly(multistart);
        if (failure.get()) {
            getLogger().log("Stopped training because of exception!");
            return;
        }

        double bestAccuracy = 10;
        int bestAccuracyIndex = 0;

        getLogger().log("multistart finished");

        //selecting best accuracy
        for (int i = 0; i < multistart; i++) {
            getLogger().log("Accuracy for " + i + " is " + accuracy[i]);
            if (accuracy[i] <= bestAccuracy) {
                bestAccuracy = accuracy[i];
                bestAccuracyIndex = i;
            }
        }
        getLogger().log("Best accuracy was for index " + bestAccuracyIndex + " (" + bestAccuracy + ")");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        bestNet = neuralNetworkList.get(bestAccuracyIndex);
        neuralNetworkList.clear();
        neuralNetworkList = null;
    }

    public void setLearningRate(double val) {
        if (bestNet != null) {
            bestNet.setInitialLearningRate(val);
            return;
        }
        for (NeuralNetwork neuralNetwork : neuralNetworkList) {
            neuralNetwork.setInitialLearningRate(val);
        }
    }

    public void setAdjustedLearningRate(double val) {
        if (bestNet != null) {
            bestNet.setAdjustLearningRate(val);
            return;
        }
        for (NeuralNetwork neuralNetwork : neuralNetworkList) {
            neuralNetwork.setInitialLearningRate(val);
        }
    }

    public void setManaged(boolean val) {
        if (bestNet != null) {
            bestNet.setManaged(val);
            return;
        }
        for (NeuralNetwork neuralNetwork : neuralNetworkList) {
            neuralNetwork.setManaged(val);
        }
    }

    private void checkVariables(LearningCaseHelper learningCaseHelper) {
        if (learningCaseHelper.getActualHeight() != learningCaseHelper.getInput().getHeight()) {
            throw new IllegalArgumentException("Number of test cases should match input height");
        }
        if (learningCaseHelper.getActualHeight() != learningCaseHelper.getOutput().getHeight()) {
            throw new IllegalArgumentException("Number of test cases should match output height");
        }
        if (learningCaseHelper.getInputs() != learningCaseHelper.getInput().getWidth()) {
            throw new IllegalArgumentException("Number of inputs should match input width");
        }
        if (learningCaseHelper.getOutputs() != learningCaseHelper.getOutput().getWidth()) {
            throw new IllegalArgumentException("Number of outputs should match output width");
        }
        if (learningCaseHelper.getOutputs() != getNeuronNetHelper().getLastLayer().getNeuronCount()) {
            throw new IllegalArgumentException("Number of neural network outputs should match output width");
        }
    }

    private NeuralNetwork getNeuronNetHelper() {
        if (bestNet == null) {
            return neuralNetworkList.get(0);
        } else {
            return bestNet;
        }
    }



    public void setWorkingInterface(NeuralNetwork.WorkingInterface workingInterface) {
        if (bestNet != null) {
            bestNet.setWorkingInterface(workingInterface);
            return;
        }
        for (NeuralNetwork neuralNetwork : neuralNetworkList) {
            neuralNetwork.setWorkingInterface(workingInterface);
        }
    }

    public int getCategoriesCount() {
        if (bestNet != null) {
            return bestNet.getCategoriesCount();
        }
        return neuralNetworkList.get(0).getCategoriesCount();
    }

    public int getInputSize() {
        return bestNet.getInputSize();
    }
}
