package com.eseo.steevejobs.service;

import org.json.JSONArray;

/**
 * Pont entre {@link WebSocketService} et l'interface JavaFX.
 * Les controllers enregistrent leurs callbacks ; le service WebSocket n'importe plus les controllers.
 */
public final class WebSocketUiBridge {

    private static final WebSocketUiBridge INSTANCE = new WebSocketUiBridge();

    public interface VisioCallbacks {
        void onTokenSuccess(String token, String roomName);

        void onVisioMessage(String message);

        void onReunionsList(JSONArray reunions);

        void onSalonDeleted(String message);

        void onRefreshReunionsRequested();
    }

    public interface TicketCallbacks {
        boolean tryRefreshChatIfActive(int ticketId);

        void onRefreshTicketList();

        void onTicketNotification(String targetType, boolean pushEnabled);
    }

    private volatile VisioCallbacks visioCallbacks;
    private volatile TicketCallbacks ticketCallbacks;

    private WebSocketUiBridge() {
    }

    public static WebSocketUiBridge getInstance() {
        return INSTANCE;
    }

    public void setVisioCallbacks(VisioCallbacks callbacks) {
        this.visioCallbacks = callbacks;
    }

    public void clearVisioCallbacks() {
        this.visioCallbacks = null;
    }

    public void setTicketCallbacks(TicketCallbacks callbacks) {
        this.ticketCallbacks = callbacks;
    }

    public void clearTicketCallbacks() {
        this.ticketCallbacks = null;
    }

    public void dispatchVisioTokenSuccess(String token, String roomName) {
        VisioCallbacks cb = visioCallbacks;
        if (cb != null) {
            cb.onTokenSuccess(token, roomName);
        }
    }

    public void dispatchVisioMessage(String message) {
        VisioCallbacks cb = visioCallbacks;
        if (cb != null) {
            cb.onVisioMessage(message);
        }
    }

    public void dispatchReunionsList(JSONArray reunions) {
        VisioCallbacks cb = visioCallbacks;
        if (cb != null && reunions != null) {
            cb.onReunionsList(reunions);
        }
    }

    public void dispatchSalonDeleted(String message) {
        VisioCallbacks cb = visioCallbacks;
        if (cb != null) {
            cb.onSalonDeleted(message);
        }
    }

    public void dispatchRefreshReunionsRequest() {
        VisioCallbacks cb = visioCallbacks;
        if (cb != null) {
            cb.onRefreshReunionsRequested();
        }
    }

    public boolean tryRefreshChatIfActive(int ticketId) {
        TicketCallbacks cb = ticketCallbacks;
        if (cb != null) {
            return cb.tryRefreshChatIfActive(ticketId);
        }
        return false;
    }

    public void dispatchRefreshChatIfActive(int ticketId) {
        tryRefreshChatIfActive(ticketId);
    }

    public void dispatchRefreshTicketList() {
        TicketCallbacks cb = ticketCallbacks;
        if (cb != null) {
            cb.onRefreshTicketList();
        }
    }

    public void dispatchTicketNotification(String targetType, boolean pushEnabled) {
        TicketCallbacks cb = ticketCallbacks;
        if (cb != null) {
            cb.onTicketNotification(targetType, pushEnabled);
        }
    }
}
