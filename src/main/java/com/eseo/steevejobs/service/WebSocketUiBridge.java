package com.eseo.steevejobs.service;

import org.json.JSONArray;

/**
 * Pont entre {@link WebSocketService} et l'interface JavaFX.
 * <p>
 * Les contrôleurs enregistrent des callbacks ; le service WebSocket n'importe pas
 * les contrôleurs. Aucun effet de bord réseau : dispatch synchrone vers les handlers
 * enregistrés uniquement s'ils sont présents.
 * </p>
 */
public final class WebSocketUiBridge {

    private static final WebSocketUiBridge INSTANCE = new WebSocketUiBridge();

    /**
     * Callbacks UI pour les événements visioconférence reçus par WebSocket.
     */
    public interface VisioCallbacks {
        /**
         * Jeton Jitsi reçu avec succès.
         *
         * @param token    jeton d'accès à la salle
         * @param roomName nom de salon validé
         */
        void onTokenSuccess(String token, String roomName);

        /**
         * Message d'information ou d'erreur visio à afficher.
         *
         * @param message texte utilisateur
         */
        void onVisioMessage(String message);

        /**
         * Liste des réunions renvoyée par le serveur.
         *
         * @param reunions tableau JSON des réunions
         */
        void onReunionsList(JSONArray reunions);

        /**
         * Confirmation ou erreur après suppression de salon.
         *
         * @param message libellé de retour
         */
        void onSalonDeleted(String message);

        /**
         * Demande de rafraîchissement de la liste des réunions planifiées.
         */
        void onRefreshReunionsRequested();
    }

    /**
     * Callbacks UI pour les mises à jour de tickets en temps réel.
     */
    public interface TicketCallbacks {
        /**
         * Tente de rafraîchir le fil de discussion si le ticket est affiché.
         *
         * @param ticketId identifiant du ticket mis à jour
         * @return {@code true} si le chat actif correspond au ticket
         */
        boolean tryRefreshChatIfActive(int ticketId);

        /**
         * Demande le rafraîchissement de la liste des tickets.
         */
        void onRefreshTicketList();

        /**
         * Signale une notification ticket (badge ou notification bureau).
         *
         * @param targetType   type de cible (ex. ADMIN, AUTEUR)
         * @param pushEnabled  {@code true} si les notifications push sont activées
         */
        void onTicketNotification(String targetType, boolean pushEnabled);
    }

    private volatile VisioCallbacks visioCallbacks;
    private volatile TicketCallbacks ticketCallbacks;

    private WebSocketUiBridge() {
    }

    /**
     * Retourne l'instance singleton du pont UI.
     *
     * @return instance partagée
     */
    public static WebSocketUiBridge getInstance() {
        return INSTANCE;
    }

    /**
     * Enregistre les handlers visio (remplace l'enregistrement précédent).
     *
     * @param callbacks implémentation des callbacks, ou {@code null} via {@link #clearVisioCallbacks()}
     */
    public void setVisioCallbacks(VisioCallbacks callbacks) {
        this.visioCallbacks = callbacks;
    }

    /**
     * Supprime les callbacks visio (écran fermé ou déconnexion).
     */
    public void clearVisioCallbacks() {
        this.visioCallbacks = null;
    }

    /**
     * Enregistre les handlers tickets.
     *
     * @param callbacks implémentation des callbacks tickets
     */
    public void setTicketCallbacks(TicketCallbacks callbacks) {
        this.ticketCallbacks = callbacks;
    }

    /**
     * Supprime les callbacks tickets.
     */
    public void clearTicketCallbacks() {
        this.ticketCallbacks = null;
    }

    /**
     * Propage un succès d'obtention de jeton visio.
     *
     * @param token    jeton Jitsi
     * @param roomName nom de salon
     */
    public void dispatchVisioTokenSuccess(String token, String roomName) {
        VisioCallbacks cb = visioCallbacks;
        if (cb != null) {
            cb.onTokenSuccess(token, roomName);
        }
    }

    /**
     * Propage un message visio vers l'UI.
     *
     * @param message texte à afficher
     */
    public void dispatchVisioMessage(String message) {
        VisioCallbacks cb = visioCallbacks;
        if (cb != null) {
            cb.onVisioMessage(message);
        }
    }

    /**
     * Propage la liste des réunions vers l'UI.
     *
     * @param reunions tableau JSON ; ignoré si {@code null}
     */
    public void dispatchReunionsList(JSONArray reunions) {
        VisioCallbacks cb = visioCallbacks;
        if (cb != null && reunions != null) {
            cb.onReunionsList(reunions);
        }
    }

    /**
     * Propage le résultat d'une suppression de salon.
     *
     * @param message libellé succès ou erreur
     */
    public void dispatchSalonDeleted(String message) {
        VisioCallbacks cb = visioCallbacks;
        if (cb != null) {
            cb.onSalonDeleted(message);
        }
    }

    /**
     * Demande le rafraîchissement de la vue des réunions planifiées.
     */
    public void dispatchRefreshReunionsRequest() {
        VisioCallbacks cb = visioCallbacks;
        if (cb != null) {
            cb.onRefreshReunionsRequested();
        }
    }

    /**
     * Délègue au callback ticket le rafraîchissement du chat actif.
     *
     * @param ticketId identifiant du ticket
     * @return {@code true} si le callback a traité le ticket affiché
     */
    public boolean tryRefreshChatIfActive(int ticketId) {
        TicketCallbacks cb = ticketCallbacks;
        if (cb != null) {
            return cb.tryRefreshChatIfActive(ticketId);
        }
        return false;
    }

    /**
     * Alias de {@link #tryRefreshChatIfActive(int)} sans valeur de retour exploitée.
     *
     * @param ticketId identifiant du ticket
     */
    public void dispatchRefreshChatIfActive(int ticketId) {
        tryRefreshChatIfActive(ticketId);
    }

    /**
     * Demande le rafraîchissement de la liste des tickets.
     */
    public void dispatchRefreshTicketList() {
        TicketCallbacks cb = ticketCallbacks;
        if (cb != null) {
            cb.onRefreshTicketList();
        }
    }

    /**
     * Propage une notification ticket (liste ou notification système).
     *
     * @param targetType  cible métier
     * @param pushEnabled préférence notifications push
     */
    public void dispatchTicketNotification(String targetType, boolean pushEnabled) {
        TicketCallbacks cb = ticketCallbacks;
        if (cb != null) {
            cb.onTicketNotification(targetType, pushEnabled);
        }
    }
}
