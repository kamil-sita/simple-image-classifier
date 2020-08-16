package pl.ksitarski.imageclassifier.gui;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import pl.ksitarski.imageclassifier.imageclassifier.Classifier;
import pl.ksitarski.imageclassifier.neuralnetwork.Logger;
import pl.ksitarski.imageclassifier.neuralnetwork.NeuralNetwork;
import pl.ksitarski.imageclassifier.neuralnetwork.helper.LearningCaseHelper;
import pl.ksitarski.imageclassifier.othertools.IO;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static pl.ksitarski.imageclassifier.neuralnetwork.LoggerSettings.*;

/**
 * GUI controller in MVC model. Controlls interactions between UI and (View) and algorithms (model)
 */
public class Gui {

    @FXML
    private TextArea trainingFolderBox;

    @FXML
    private ChoiceBox<String> stopConditionChoice;

    @FXML
    private TextField stopConditionValueField;

    @FXML
    private CheckBox useColorsCheckBox;

    @FXML
    private TextField targetSizeField;

    @FXML
    private TextField layersField;

    @FXML
    private TextField neuronsField;

    @FXML
    private TextField learningRateField;

    @FXML
    private TextField learningRateAdjustmentField;

    @FXML
    private TextArea testingFolderBox;

    @FXML
    private TextArea infoBox;

    @FXML
    private CheckBox useBackupCheckbox;

    @FXML
    private TextField multistartField;

    private NeuralNetwork.LearningStopConditionType learningStopConditionType;
    private double stopConditionValue;
    private boolean useColors;
    private int targetSize;
    private int layers;
    private int[] neurons;
    private double learningRate;
    private double learningRateAdjustment;
    private boolean useBackups;
    private int multistart;
    private int categories;
    private List<List<File>> filesByClassifier;
    private LearningCaseHelper learningCaseHelper;


    private Classifier classifier;
    private WorkingInterface workingInterface;

    /**
     * Proposes sample values for network in GUI.
     */
    private void recalc() {
        int value = 1;
        int layers = 1;
        try {
            value = Integer.parseInt(targetSizeField.getText());
            layers = Integer.parseInt(layersField.getText());
        } catch (Exception e) {
        }
        int neurons = value * value * (useColorsCheckBox.isSelected() ? 3 : 1);
        StringBuilder s = new StringBuilder();
        if (layers < 100) {
            for (int i = 0; i < layers; i++) {
                s.append(neurons);
                if (i + 1 != layers) {
                    s.append(", ");
                }
            }
        }
        neuronsField.setText(s.toString());
    }

    /**
     * Function called by JavaFX after GUI initialization. Used to set up some variables and insert elements in GUI.
     */
    @FXML
    void initialize() {
        //logger implementation that writes text to GUI and console
        setLogger(new Logger() {
            int writtenMessagesCount = 0;

            @Override
            public void log(String msg) {
                Platform.runLater(() -> {
                    writtenMessagesCount++;
                    if (writtenMessagesCount > 1000) {
                        writtenMessagesCount = 0;
                        clear();
                    }
                    infoBox.setText(infoBox.getText() + msg + "\n");
                    infoBox.appendText("");
                });
                System.out.println(msg);
            }

            @Override
            public void logI(String msg) {
                log(msg);
            }

            @Override
            public void clear() {
                Platform.runLater(() -> {
                    infoBox.setText("");
                    infoBox.appendText("");
                });
            }
        });
        Platform.runLater(() -> {
            stopConditionChoice.getItems().addAll("Iterations", "Error difference", "Error");
            stopConditionChoice.getSelectionModel().select(0);
        });

        targetSizeField.textProperty().addListener((observable, oldValue, newValue) -> recalc());
        useColorsCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> recalc());
        layersField.textProperty().addListener((observable, oldValue, newValue) -> recalc());

        stopConditionChoice.selectionModelProperty().addListener(new ChangeListener<SingleSelectionModel<String>>() {
            @Override
            public void changed(ObservableValue<? extends SingleSelectionModel<String>> observable, SingleSelectionModel<String> oldValue, SingleSelectionModel<String> newValue) {
                switch (newValue.getSelectedIndex()) {
                    case 0: //iterations
                        stopConditionValueField.setText("200");
                        break;
                    case 1: //error difference
                        stopConditionValueField.setText("0.001");
                        break;
                    case 2:
                        stopConditionValueField.setText("0.01");
                        break;
                }
            }
        });
        infoBox.textProperty().addListener(new ChangeListener<Object>() {
            @Override
            public void changed(ObservableValue<?> observable, Object oldValue,
                                Object newValue) {
                infoBox.setScrollTop(Double.MAX_VALUE);
            }
        });
    }

    /**
     * Tries to parse arguments from GUI to representation that makes sense for the algorithm
     */
    private boolean parseArgs() {
        try {
            //simple inputs
            int index = stopConditionChoice.getSelectionModel().getSelectedIndex();
            switch (index) {
                case 0:
                    learningStopConditionType = NeuralNetwork.LearningStopConditionType.ITERATION;
                    break;
                case 1:
                    learningStopConditionType = NeuralNetwork.LearningStopConditionType.ERROR_DIFFERENCE;
                    break;
                case 2:
                    learningStopConditionType = NeuralNetwork.LearningStopConditionType.ERROR;
            }

            stopConditionValue = Double.parseDouble(stopConditionValueField.getText());
            useColors = useColorsCheckBox.isSelected();
            targetSize = Integer.parseInt(targetSizeField.getText());
            layers = Integer.parseInt(layersField.getText());
            learningRate = Double.parseDouble(learningRateField.getText());
            learningRateAdjustment = Double.parseDouble(learningRateAdjustmentField.getText());
            useBackups = useBackupCheckbox.isSelected();
            multistart = Integer.parseInt(multistartField.getText());
            if (multistart < 1) {
                infoBox.setText("Multistart has value of at least 1");
                return false;
            }

            if (layers < 1) {
                infoBox.setText("Sizes smaller than 1 are not supported");
                return false;
            }

            String[] neuronsString =  neuronsField.getText().split(", ");
            for (int i = 0; i < neuronsString.length; i++) {
                neuronsString[i] = neuronsString[i].trim();
            }
            neurons = new int[neuronsString.length];
            for (int i = 0; i < neuronsString.length; i++) {
                neurons[i] = Integer.parseInt(neuronsString[i]);
            }

            //training
            if (classifier == null) {
                String[] training = trainingFolderBox.getText().split("\n");
                filesByClassifier = new ArrayList<>();
                for (String folder : training) {
                    filesByClassifier.add(IO.filesInDirectory(folder));
                }

            }

            return true;
        } catch (Exception e) {
            infoBox.setText(e.toString());
        }
        return false;

    }

    /**
     * Displays dialog window that loads NNF (neural network state file) from selected by user file.
     */
    @FXML
    void loadPress(ActionEvent event) {
        getLogger().clear();
        classifier = IO.openNnf().orElse(null);
        if (classifier == null) {
            getLogger().log("Failed to load NNF");
            return;
        }
        getLogger().log("Loaded new classifier from file.");
        getLogger().log("Can be used as classifier for " + classifier.getCategoriesCount() + " categories. \r\nAccepts images of size " + classifier.getImageSize() +
                " x " + classifier.getImageSize() + " and " + (classifier.isColors() ? "uses colors." : "does not use colors."));
    }

    /**
     * Displays dialog window that saves NNF (neural network state file) to selected by user file.
     */
    @FXML
    void savePress(ActionEvent event) {
        IO.saveNnf(classifier);
    }


    /**
     * Starts training classifier. If it does not exist, creates it, and is able to use resources (images) that are already preloaded.
     */
    @FXML
    void startPress(ActionEvent event) {
        stopPress(null);
        if (!parseArgs()) {
            return;
        }
        new Thread(() -> {
            boolean alreadyExisting = true;
            if (classifier == null) {
                try {
                    if (learningCaseHelper != null) {
                        classifier = new Classifier(learningCaseHelper, targetSize, layers, neurons, learningRate, useColors, learningRateAdjustment, useBackups, multistart, categories);
                    } else {
                        classifier = new Classifier(filesByClassifier, targetSize, layers, neurons, learningRate, useColors, learningRateAdjustment, useBackups, multistart);
                    }
                } catch (Exception e) {
                    getLogger().log("Error was caught, check if your settings are correct:");
                    getLogger().log(e.toString());
                    return;
                }
                learningCaseHelper = classifier.getLearningCaseHelper();
                categories = classifier.getCategoriesCount();
                alreadyExisting = false;
            }
            getLogger().log("Starting...");
            if (alreadyExisting) {
                classifier.setLearningRate(learningRate);
                classifier.setAdjustedLearningRate(learningRate);
                classifier.setManaged(useBackups);
            }
            workingInterface = new WorkingInterface();
            NeuralNetwork.LearningStopConditionTarget learningStopConditionTarget = new NeuralNetwork.LearningStopConditionTarget();
            learningStopConditionTarget.learningStopConditionType = learningStopConditionType;
            learningStopConditionTarget.val = stopConditionValue;
            classifier.setWorkingInterface(workingInterface);
            try {
                classifier.train(learningStopConditionTarget);
            } catch (Exception e) {
                e.printStackTrace();
                getLogger().log(e.getMessage());
            }
        }).start();
    }

    /**
     * Fully removes classifier that is in memory
     */
    @FXML
    void removePress(ActionEvent event) {
        getLogger().log("Removed current network");
        learningCaseHelper = null;
        classifier = null;
    }

    /**
     * Removes classifier that is in memory, but preserves loaded (scaled down) images. Those images can be reused in
     * classifier of the same size.
     */
    @FXML
    void removeWithoutImagesPress(ActionEvent event) {
        getLogger().log("Removed current network (and preserved images)");
        classifier = null;
    }


    /**
     * Tries to safely stop current network. This cannot be done if network is in multistart phase.
     */
    @FXML
    void stopPress(ActionEvent event) {
        if (workingInterface != null) {
            workingInterface.isWorking = false;
        }
    }

    /**
     * Tests categorization of files against known categories.
     */
    @FXML
    void testPress(ActionEvent event) {
        getLogger().clear();
        new Thread(() -> {
            List<List<File>> filesByClassifierTest = new ArrayList<>();
            String[] testing = testingFolderBox.getText().split("\n");
            for (String folder : testing) {
                if (folder.trim().isEmpty()) {
                    continue;
                }
                filesByClassifierTest.add(IO.filesInDirectory(folder));
            }
            classifier.categorizeTest(filesByClassifierTest);
        }).start();
    }

    /**
     * Categorizes files against unknown categories.
     */
    @FXML
    void categorizePress(ActionEvent event) {
        getLogger().clear();
        new Thread(() -> {
            List<List<File>> filesByClassifierTest = new ArrayList<>();
            String[] testing = testingFolderBox.getText().split("\n");
            for (String folder : testing) {
                if (folder.trim().isEmpty()) {
                    continue;
                }
                filesByClassifierTest.add(IO.filesInDirectory(folder));
            }
            classifier.categorize(filesByClassifierTest);
        }).start();
    }

    /**
     * Implementation of interface that safely stops neural network mid-progress. This implementation depends on user
     * actions.
     */
    private static class WorkingInterface implements NeuralNetwork.WorkingInterface {
        private boolean isWorking = true;
        @Override
        public boolean isContinueLearning() {
            return isWorking;
        }
    }


}
