package pl.ksitarski.imageclassifier.imageclassifier;

import pl.ksitarski.imageclassifier.neuralnetwork.NeuralNetwork;
import pl.ksitarski.imageclassifier.neuralnetwork.helper.LearningCaseHelper;
import pl.ksitarski.imageclassifier.neuralnetwork.helper.NeuralNetworkHelper;
import pl.ksitarski.imageclassifier.neuralnetwork.math.Matrix;
import pl.ksitarski.imageclassifier.othertools.IO;

import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static pl.ksitarski.imageclassifier.neuralnetwork.LoggerSettings.*;

/**
 * Classifier layer (decorator) on top of neural network. Is responsible for generating data for NN and selecting argument.
 */
public class Classifier {

    private final int imageSize;
    private final boolean colors;
    private final LearningCaseHelper learningCaseHelper;
    private final NeuralNetworkHelper neuralNetworkHelper;

    /**
     * Default constructor
     * @param filesByClassifier files divided in categories by class
     * @param scaledSize size to which files will be scaled
     * @param layers numbers of layers in neural network
     * @param neuronsInLayer description of number of neurons per layer in neural network
     * @param learningRate learning rate
     * @param colors specifies whether colors will be used
     * @param adjustLearningRate adjustment made by neural network after mistake
     * @param isManaged whether learning should be controlled by internal algorithm
     * @param multistart number of multistart threads or 1
     */
    public Classifier(List<List<File>> filesByClassifier, int scaledSize, int layers, int[] neuronsInLayer, double learningRate, boolean colors, double adjustLearningRate, boolean isManaged, int multistart) {
        this.imageSize = scaledSize;
        this.colors = colors;
        int categories = filesByClassifier.size();
        int entries = 0;
        for (List<File> fileList : filesByClassifier) {
            entries += fileList.size();
        }

        int inputs = colors ? scaledSize * scaledSize * 3 : scaledSize * scaledSize;
        learningCaseHelper = new LearningCaseHelper(entries, inputs, categories);

        int categoryId = 0;
        for (List<File> fileList : filesByClassifier) {
            int fileId = 0;
            for (File file : fileList) {
                getLogger().clear();
                getLogger().log("Loading: " + file.getName());
                getLogger().log("File " + fileId++ + "/" + fileList.size() + " of category " + categoryId + "/" + filesByClassifier.size());
                Optional<BufferedImage> optionalBufferedImage = IO.getImage(file);
                if (optionalBufferedImage.isPresent()) {
                    BufferedImage image = optionalBufferedImage.get();

                    double[] data = ImageConverter.imageToData(image, scaledSize, colors);
                    getLogger().log("Scaling: " + file.getName());

                    learningCaseHelper.setInputData(data);
                    learningCaseHelper.setOutputDataClassifier(categoryId);
                } else {
                    getLogger().log("Could not load: " + file.getName());
                }
            }
            categoryId++;
        }
        learningCaseHelper.trim();
        neuralNetworkHelper = new NeuralNetworkHelper(layers, neuronsInLayer, inputs, categories, learningRate, adjustLearningRate, 2.0, isManaged, multistart);
    }

    /**
     * Classifier that uses exisiting LearningCaseHelper (for example reuses files).
     * @param learningCaseHelper data that was already loaded
     * @param scaledSize size to which files will be scaled
     * @param layers numbers of layers in neural network
     * @param learningRateAdjustment adjustment made by neural network after mistake
     * @param learningRate learning rate
     * @param isManaged whether learning should be controlled by internal algorithm
     * @param multistart number of multistart threads or 1
     * @param categories number of categories for classifier
     * @param neurons description of neuron count by layer
     * @param useColors whether classifier should use color information
     */
    public Classifier(LearningCaseHelper learningCaseHelper, int scaledSize, int layers, int[] neurons, double learningRate, boolean useColors, double learningRateAdjustment, boolean isManaged, int multistart, int categories) {
        this.imageSize = scaledSize;
        this.colors = useColors;
        this.learningCaseHelper = learningCaseHelper;
        int inputs = colors ? scaledSize * scaledSize * 3 : scaledSize * scaledSize;
        neuralNetworkHelper = new NeuralNetworkHelper(layers, neurons, inputs, categories, learningRate, learningRateAdjustment, 2.0, isManaged, multistart);
    }

    /**
     * Trains network using given strategy
     */
    public void train(NeuralNetwork.LearningStopConditionTarget learningStopConditionTarget) {
        Instant start = Instant.now();
        neuralNetworkHelper.train(learningCaseHelper, learningStopConditionTarget);
        Instant finish = Instant.now();
        long timeElapsed = Duration.between(start, finish).getSeconds();
        getLogger().log("Training took " + timeElapsed + " seconds");
    }

    /**
     * Tests categorization against testing data and writes information to logger.
     */
    public void categorizeTest(List<List<File>> input) {
        int expectedCategory = 0;
        int guesses = 0;
        int misses = 0;
        int failed = 0;
        for (List<File> category : input) {
            getLogger().log("==========");
            getLogger().log("Categorizing for category: " + expectedCategory);

            for (File file : category) {
                getLogger().log("Categorizing file " + file.getName() + " of category " + expectedCategory);
                Matrix m = categorizeFile(file);
                if (m == null) {
                    getLogger().log("Failed to load file:" + file.getName());
                    failed++;
                } else {
                    m.writeRow(0);
                    int biggest = m.getBiggestIdInRow(0);
                    if (biggest != expectedCategory) {
                        misses++;
                        getLogger().log("Mismatched file");
                    } else {
                        guesses++;
                        getLogger().log("Matched file");
                    }
                }
                getLogger().log("____");
            }
            expectedCategory++;
        }
        getLogger().log("+++++++++++++++++++++");
        getLogger().log("Finished categorization test");
        getLogger().log("+++++++++++++++++++++");
        getLogger().log("Checked files: " + (guesses + misses));
        getLogger().log("Correctly guessed: " + guesses);
        getLogger().log("Incorrectly guessed: " + misses);
        getLogger().log("Failed to load: " + failed);
    }

    /**
     * Categorizes unknown input and writes information to logger.
     */
    public void categorize(List<List<File>> input) {
        getLogger().log("==========");
        int failed = 0;
        for (List<File> category : input) {
            for (File file : category) {
                getLogger().log("Categorizing file " + file.getName());
                Matrix m = categorizeFile(file);
                if (m == null) {
                    getLogger().log("Failed to load file:" + file.getName());
                    failed++;
                } else {
                    m.writeRow(0);
                    int biggest = m.getBiggestIdInRow(0);
                    getLogger().log("Most likely category " + biggest + " with value " + m.get(biggest, 0));
                }
                getLogger().log("____");
            }
        }
        getLogger().log("+++++++++++++++++++++");
        getLogger().log("Finished categorization");
        getLogger().log("+++++++++++++++++++++");
        getLogger().log("Failed to load: " + failed);
    }

    private Matrix categorizeFile(File input) {
        Optional<BufferedImage> optionalBufferedImage = IO.getImage(input);
        if (!optionalBufferedImage.isPresent()) {
            return null;
        }
        BufferedImage image = optionalBufferedImage.get();
        double[] data = ImageConverter.imageToData(image, imageSize, colors);
        Matrix in = new Matrix(1, colors ? imageSize * imageSize * 3 : imageSize * imageSize);
        in.setRow(0, data);
        return neuralNetworkHelper.deduce(in);
    }

    public void setWorkingInterface(NeuralNetwork.WorkingInterface workingInterface) {
        neuralNetworkHelper.setWorkingInterface(workingInterface);
    }

    public int getCategoriesCount() {
        return neuralNetworkHelper.getCategoriesCount();
    }

    public int getInputSize() {
        return neuralNetworkHelper.getInputSize();
    }

    public int getImageSize() {
        return imageSize;
    }

    public boolean isColors() {
        return colors;
    }

    public LearningCaseHelper getLearningCaseHelper() {
        return learningCaseHelper;
    }

    public void setLearningRate(double val) {
        neuralNetworkHelper.setLearningRate(val);
    }

    public void setAdjustedLearningRate(double val) {
        neuralNetworkHelper.setAdjustedLearningRate(val);
    }

    public void setManaged(boolean val) {
        neuralNetworkHelper.setManaged(val);
    }

}
