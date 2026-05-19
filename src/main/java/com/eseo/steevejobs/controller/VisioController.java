package com.eseo.steevejobs.controller;

import com.github.sarxos.webcam.Webcam;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;

import java.awt.image.BufferedImage;

public class VisioController {

    @FXML
    private ImageView imageView;
    private Webcam webcam;
    private boolean isRunning = true;

    @FXML
    public void initialize() {
        // 1. Initialiser la caméra par défaut
        webcam = Webcam.getDefault();
        if (webcam != null) {
            webcam.open();

            // 2. Lancer un thread pour lire les images sans bloquer l'UI
            Thread thread = new Thread(() -> {
                while (isRunning) {
                    BufferedImage image = webcam.getImage();
                    if (image != null) {
                        // 3. Convertir l'image AWT en image JavaFX
                        WritableImage fxImage = SwingFXUtils.toFXImage(image, null);

                        // 4. Mettre à jour l'UI sur le thread principal
                        Platform.runLater(() -> imageView.setImage(fxImage));
                        image.flush(); // Libère la mémoire
                    }
                    try {
                        Thread.sleep(33);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            });
            thread.setDaemon(true);
            thread.start();
        }
    }

    // Très important : ferme la caméra quand on quitte la vue !
    public void stopPreview() {
        isRunning = false;
        if (webcam != null) {
            webcam.close();
        }
    }
}