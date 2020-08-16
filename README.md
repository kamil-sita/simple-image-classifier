# Simple Image Classifier

## About project

This project contains application that is a simple image classifier backed by artificial neural network trained via backpropagation algorithm. This ANN implementation supports multiple hidden layers.

The aim of this project was to made an easy to use image classifier for "everyone". Because of that, application contains JavaFX GUI. However, tuning the network might require at least some knowledge in machine learning.

## Features

* easy to use
* takes folders as input
* GUI
* written in Java 8
* multiple training strategies (iterations, deviation, deviation difference)
* configurable
* starting and stopping of training at will
* multithreaded multistart
* optional automatic learning rate adjustment and restoration of prior model with better accuracy

## Potential problems

Project uses JSON files for saving and loading data which are not really optimal because of their raw, uncompressed format and the fact that image data is also stored there. You might drastically reduce their size by basic compression (zip, 7z, rar, etc) and by removing image data if you do not need to train classifier further. I might in future implement those functionalities.

## Libraries used
* GSON
* JavaFX

## Future of the project
I do not plan to further develop this version of the project (apart from maybe fixing issues mentioned above). However, I have some ideas that might turn into new version of the project:

* client - server architecture
* support for more models (different than artificial neural network)
*  SIMD support once Java supports it
