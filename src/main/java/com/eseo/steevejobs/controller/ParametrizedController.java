package com.eseo.steevejobs.controller;

/**
 * Contrôleur FXML recevant un paramètre de navigation depuis {@link HomeController}
 * ou {@link MenuController} (ex. rôle utilisateur, service ticket).
 */
public interface ParametrizedController {

    /**
     * Initialise le contrôleur avec le paramètre transmis lors du chargement de la vue.
     *
     * @param parametre valeur contextuelle (rôle, service, etc.) ; peut être {@code null}
     */
    void initData(String parametre);
}