package com.eseo.steevejobs;

/**
 * Point d'entrée de l'application pour les outils de build JavaFX (jlink, jpackage).
 * Délègue le démarrage à {@link HelloApplication}.
 */
public class Launcher {

    /**
     * Lance l'application JavaFX via {@link HelloApplication#main(String[])}.
     *
     * @param args arguments de la ligne de commande transmis à l'application
     */
    public static void main(String[] args) {
        HelloApplication.main(args);
    }
}
