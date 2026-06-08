package com.eseo.steevejobs.config;

import javafx.scene.paint.Color;

/**
 * Choisit une couleur de texte lisible (noir ou blanc) selon la luminosité du fond.
 * Texte noir uniquement sur les ~30 % de fonds les plus clairs (luminance &gt; 0,70).
 */
public final class ColorContrastUtil {

    private ColorContrastUtil() {
    }

    /** Seuil : au-dessus → fond clair → texte noir ; en dessous → texte blanc. */
    private static final double BLACK_TEXT_MIN_LUMINANCE = 0.70;

    /**
     * Retourne la couleur de texte (#000000 ou #ffffff) adaptée au fond.
     *
     * @param backgroundColor couleur de fond (hex, rgb, nom CSS)
     * @return {@code "#000000"} sur fond clair, {@code "#ffffff"} sinon
     */
    public static String textFillForBackground(String backgroundColor) {
        return isLightBackground(backgroundColor) ? "#000000" : "#ffffff";
    }

    /**
     * Détermine si le fond est suffisamment clair pour un texte noir.
     *
     * @param backgroundColor couleur de fond à analyser
     * @return {@code true} si la luminance dépasse le seuil interne ; {@code true} par défaut si la couleur est invalide
     */
    public static boolean isLightBackground(String backgroundColor) {
        Color color = parseColor(backgroundColor);
        if (color == null) {
            return true;
        }

        double r = color.getRed();
        double g = color.getGreen();
        double b = color.getBlue();
        double luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b;
        return luminance > BLACK_TEXT_MIN_LUMINANCE;
    }

    private static Color parseColor(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Color.web(value.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
