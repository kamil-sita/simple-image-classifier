package pl.ksitarski.imageclassifier.othertools;

import com.google.gson.GsonBuilder;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import pl.ksitarski.imageclassifier.imageclassifier.Classifier;
import pl.ksitarski.imageclassifier.neuralnetwork.LoggerSettings;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Stream;

/**
 * Contains input/output related functions.
 */
public class IO {

    private enum FileChooserType {
        open, save
    }

    //static variable so that the file opener does not have restarted location every time it is openend
    private static File lastFileDirectory = null;

    /**
     * Tries to load image from given file.
     */
    public static Optional<BufferedImage> getImage(File file) {
        BufferedImage bufferedImage = null;
        try {
            bufferedImage = ImageIO.read(file);
        } catch (IOException | IllegalArgumentException e) {
            return Optional.empty();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (bufferedImage == null) return Optional.empty();
        return Optional.of(bufferedImage);
    }

    /**
     * Returns a list of files in given directory.
     */
    public static List<File> filesInDirectory(String dir) {
        if (dir == null) {
            throw new NullPointerException("dir " + dir + " is null");
        }
        if (new File(dir).listFiles() == null) {
            throw new NullPointerException(dir + " has no files");
        }
        return new ArrayList<>(Arrays.asList(new File(dir).listFiles()));
    }

    /**
     * Saves given string to file with given name.
     */
    public static void toFile(String data, String fileName) {
        try (PrintWriter out = new PrintWriter(fileName)) {
            out.write(data);
            LoggerSettings.getLogger().log("saved");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            LoggerSettings.getLogger().log("saving failed!");
        }
    }

    /**
     * Saves given string to given file.
     */
    public static void toFile(String data, File file) {
        try (PrintWriter out = new PrintWriter(file)) {
            out.write(data);
            LoggerSettings.getLogger().log("saved");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            LoggerSettings.getLogger().log("saving failed!");
        }
    }

    /**
     * Reads string from file.
     */
    public static String fromFile(File file) {
        StringBuilder contentBuilder = new StringBuilder();

        try (Stream<String> stream = Files.lines(file.toPath(), StandardCharsets.UTF_8))
        {
            stream.forEach(s -> contentBuilder.append(s).append("\n"));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return contentBuilder.toString();
    }

    private static FileChooser getImageFileChooser(FileChooserType fileChooserType) {

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(fileChooserType == FileChooserType.open ? "Open image from file" : "Save image to file");
        fileChooser.setInitialDirectory(lastFileDirectory);
        if (fileChooserType == FileChooserType.open) {
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Neural Network File", "*.nnf"),
                    new FileChooser.ExtensionFilter("All Files", "*.*")
            );
        } else {
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Neural Network File", "*.nnf")
            );
        }

        return fileChooser;

    }

    /**
     * Loads NNF (neural network state file) using GUI.
     */
    public static Optional<Classifier> openNnf() {
        Stage stage = new Stage();

        File file = getImageFileChooser(FileChooserType.open).showOpenDialog(stage);

        if (file != null) {
            lastFileDirectory = file.getParentFile();
        } else {
            return Optional.empty();
        }

        String s = fromFile(file);
        try {
            return Optional.of(new GsonBuilder().create().fromJson(s, Classifier.class));
        } catch (Exception e) {
            LoggerSettings.getLogger().log(e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Saves NNF (neural network state file) using GUI.
     */
    public static void saveNnf(Classifier classifier) {
        Stage stage = new Stage();

        File file = getImageFileChooser(FileChooserType.save).showSaveDialog(stage);

        if (file != null) {
            try {
                String s = new GsonBuilder().setPrettyPrinting().create().toJson(classifier);
                toFile(s, file);
            } catch (Exception e) {
                e.printStackTrace();
            }
            lastFileDirectory = file.getParentFile();
        }
    }
}
