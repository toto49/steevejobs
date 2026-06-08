(function () {
    const config = window.VISIO_CONFIG || {};
    const CHAT_OVERLAY_MQ = window.matchMedia("(max-width: 900px)");
    const LiveKitClient = window.LivekitClient || window.LiveKitClient;

    const loadingScreen = document.getElementById("loading-screen");
    const errorScreen = document.getElementById("error-screen");
    const permissionScreen = document.getElementById("permission-screen");
    const roomScreen = document.getElementById("room-screen");
    const errorMessage = document.getElementById("error-message");
    const errorBackLink = document.getElementById("error-back-link");
    const permissionRoomName = document.getElementById("permission-room-name");
    const permissionStatus = document.getElementById("permission-status");
    const joinRoomButton = document.getElementById("join-room-button");
    const brandName = document.getElementById("brand-name");
    const brandLogo = document.getElementById("brand-logo");
    const roomTitle = document.getElementById("room-title");
    const participantCount = document.getElementById("participant-count");
    const videoGrid = document.getElementById("video-grid");
    const galleryNav = document.getElementById("gallery-nav");
    const galleryPrev = document.getElementById("gallery-prev");
    const galleryNext = document.getElementById("gallery-next");
    const galleryStatus = document.getElementById("gallery-status");
    const leaveButton = document.getElementById("leave-button");
    const toggleMic = document.getElementById("toggle-mic");
    const toggleCam = document.getElementById("toggle-cam");
    const toggleScreen = document.getElementById("toggle-screen");
    const micDeviceMenuBtn = document.getElementById("mic-device-menu");
    const camDeviceMenuBtn = document.getElementById("cam-device-menu");
    const micDeviceList = document.getElementById("mic-device-list");
    const camDeviceList = document.getElementById("cam-device-list");
    const chatPanel = document.getElementById("chat-panel");
    const chatMessages = document.getElementById("chat-messages");
    const chatForm = document.getElementById("chat-form");
    const chatInput = document.getElementById("chat-input");
    const chatSendBtn = document.getElementById("chat-send");
    const closeChat = document.getElementById("close-chat");
    const toggleChat = document.getElementById("toggle-chat");
    const chatBackdrop = document.getElementById("chat-backdrop");

    const tiles = new Map();
    const remoteAudio = new Map();
    const audioVolumes = new Map();
    let audioContext = null;
    let focusedTileKey = null;
    let galleryPageIndex = 0;
    let room = null;
    let connectionParams = null;
    let controlsBound = false;
    let deviceMenusBound = false;
    let openDeviceMenuKind = null;
    let connectPromise = null;
    let isConnecting = false;
    let intentionalLeave = false;
    let sessionIdentity = null;
    let sessionDisplayName = null;
    let sessionRoomCreatorId = null;
    let sessionKickToken = null;
    let sessionEndToken = null;
    let crossTabBound = false;

    const VISIO_TAB_SYNC_KEY = "steevejobs_visio_sync";
    const SESSION_TAB_ID = `${Date.now()}-${Math.random().toString(36).slice(2)}`;
    const TAB_HANDOFF_MS = 400;
    const CONNECT_TIMEOUT_MESSAGE =
        "Délai dépassé : le serveur LiveKit ne répond pas. Attendez quelques secondes, puis réessayez depuis l'application SteeveJobs.";
    const MIC_MUTED_ICON =
        '<svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M12 1a3 3 0 00-3 3v8a3 3 0 006 0V4a3 3 0 00-3-3z"/><path d="M19 10v2a7 7 0 01-14 0v-2"/><path d="M12 19v4"/><path d="M8 23h8"/><line x1="1" y1="1" x2="23" y2="23"/></svg>';
    const PERSON_ICON =
        '<svg viewBox="0 0 24 24" width="56" height="56" fill="currentColor" aria-hidden="true"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>';
    const SPEAKER_ICON =
        '<svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"/><path d="M15.54 8.46a5 5 0 010 7.07"/><path d="M19.07 4.93a10 10 0 010 14.14"/></svg>';
    const SPEAKER_OFF_ICON =
        '<svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"/><line x1="23" y1="9" x2="17" y2="15"/><line x1="17" y1="9" x2="23" y2="15"/></svg>';
    const KICK_ICON =
        '<svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M18 6L6 18"/><path d="M6 6l12 12"/></svg>';
    const VOLUME_MIN = 0;
    const VOLUME_MAX = 2;
    const VOLUME_DEFAULT = 1;
    const GALLERY_PAGE_SIZE = 9;
    const DEVICE_KIND = {
        audio: "audioinput",
        video: "videoinput",
    };

    applyBranding();
    bindStaticControls();
    bindPageLifecycle();
    boot();

    function boot() {
        if (document.readyState === "loading") {
            document.addEventListener("DOMContentLoaded", prepareJoinScreen);
        } else {
            prepareJoinScreen();
        }
    }

    function bindPageLifecycle() {
        window.addEventListener("pagehide", () => {
            intentionalLeave = true;
            notifyOtherVisioTabs("leave");
            if (room) {
                requestRoomEndIfLastParticipant(room);
                room.disconnect(true);
            }
        });

        window.addEventListener("resize", () => {
            if (videoGrid?.dataset.layout === "many" || videoGrid?.dataset.layout === "nine") {
                updateManyGridMetrics(getVisibleTileCountForLayout());
            }
        });
    }

    function bindCrossTabCleanup() {
        if (crossTabBound) {
            return;
        }

        crossTabBound = true;

        window.addEventListener("storage", (event) => {
            if (event.key !== VISIO_TAB_SYNC_KEY || !event.newValue) {
                return;
            }

            try {
                const data = JSON.parse(event.newValue);
                if (data.tabId === SESSION_TAB_ID) {
                    return;
                }
                if (sessionIdentity && data.identity === sessionIdentity) {
                    void forceLocalDisconnect();
                }
            } catch (_error) {
                // Ignore malformed sync payloads.
            }
        });
    }

    function notifyOtherVisioTabs(action) {
        if (!sessionIdentity) {
            return;
        }

        localStorage.setItem(
            VISIO_TAB_SYNC_KEY,
            JSON.stringify({
                identity: sessionIdentity,
                tabId: SESSION_TAB_ID,
                action,
                ts: Date.now(),
            })
        );
    }

    async function waitForOtherTabsToLeave() {
        notifyOtherVisioTabs("leave");
        await new Promise((resolve) => setTimeout(resolve, TAB_HANDOFF_MS));
        notifyOtherVisioTabs("connect");
        await new Promise((resolve) => setTimeout(resolve, TAB_HANDOFF_MS));
    }

    async function forceLocalDisconnect() {
        intentionalLeave = true;
        await resetRoom();
        intentionalLeave = false;
        showJoinScreen();
        clearPermissionStatus();
        setJoinButtonsDisabled(false);
        if (joinRoomButton) {
            joinRoomButton.textContent = "Rejoindre la salle";
        }
    }

    function bindStaticControls() {
        bindClick(joinRoomButton, handleJoinRoomClick);
        bindClick(galleryPrev, () => changeGalleryPage(-1));
        bindClick(galleryNext, () => changeGalleryPage(1));
        bindClick(toggleChat, () => {
            setChatOpen(chatPanel.hidden);
        });
        bindClick(chatBackdrop, () => setChatOpen(false));
        bindClick(closeChat, () => setChatOpen(false));

        CHAT_OVERLAY_MQ.addEventListener("change", () => {
            syncChatPresentation();
            requestAnimationFrame(() => updateVideoGridLayout());
        });
    }

    function isChatOverlayMode() {
        return CHAT_OVERLAY_MQ.matches;
    }

    function setChatOpen(open) {
        if (!chatPanel) {
            return;
        }

        chatPanel.hidden = !open;
        syncChatPresentation();

        if (open && chatInput) {
            chatInput.focus();
        }

        requestAnimationFrame(() => updateVideoGridLayout());
    }

    function syncChatPresentation() {
        const open = chatPanel && !chatPanel.hidden;

        if (roomScreen) {
            roomScreen.classList.toggle("chat-side-open", open && !isChatOverlayMode());
            roomScreen.classList.toggle("chat-overlay-open", open && isChatOverlayMode());
        }

        if (toggleChat) {
            toggleChat.classList.toggle("active", open);
        }

        if (chatBackdrop) {
            chatBackdrop.hidden = !open || !isChatOverlayMode();
        }
    }

    function bindClick(element, handler) {
        if (element) {
            element.addEventListener("click", handler);
        }
    }

    function applyBranding() {
        if (config.brandName && brandName) {
            brandName.textContent = config.brandName;
            document.title = `${config.brandName} - Visioconférence`;
        }

        if (config.backLink && errorBackLink) {
            errorBackLink.href = config.backLink;
        }

        if (config.logoUrl && brandLogo) {
            brandLogo.src = config.logoUrl;
            brandLogo.hidden = false;
            document.querySelector(".brand-icon").hidden = true;
        }
    }

    function prepareJoinScreen() {
        showJoinScreen();

        const params = new URLSearchParams(window.location.search);
        const serverUrl = config.livekitUrl || params.get("url");
        let token = params.get("token");
        const roomName = params.get("room") || readRoomFromToken(token) || "Salle de visio";

        if (token) {
            token = token.replace(/ /g, "+");
        }

        if (!LiveKitClient) {
            showError(
                "Bibliothèque LiveKit introuvable. Vérifiez que js/vendor/livekit-client.umd.min.js est bien déployé sur le serveur."
            );
            return;
        }

        if (!serverUrl) {
            showError(
                "URL du serveur LiveKit non configurée. Vérifiez js/config.js sur le serveur de visio."
            );
            return;
        }

        if (!token) {
            showError(
                "Paramètres de connexion manquants. Ouvrez cette page depuis l'application SteeveJobs après avoir rejoint une réunion."
            );
            return;
        }

        if (!window.isSecureContext) {
            showError(
                "La caméra et le micro ne fonctionnent qu'en HTTPS. Ouvrez la page via https://visio.atomgame.fr."
            );
            return;
        }

        if (!navigator.mediaDevices?.getUserMedia) {
            showError("Votre navigateur ne supporte pas l'accès à la caméra et au micro.");
            return;
        }

        sessionIdentity = readIdentityFromToken(token);
        sessionDisplayName = readDisplayNameFromToken(token);
        sessionRoomCreatorId = params.get("createurId");
        sessionKickToken = params.get("kickToken");
        sessionEndToken = params.get("endToken");
        connectionParams = {serverUrl, token, roomName};
        bindCrossTabCleanup();
        roomTitle.textContent = roomName;

        if (permissionRoomName) {
            permissionRoomName.textContent = `Salle : ${roomName}`;
        }

        clearPermissionStatus();
        setJoinButtonsDisabled(false);
    }

    function readTokenPayload(token) {
        if (!token) {
            return null;
        }

        try {
            const payload = token.split(".")[1];
            if (!payload) {
                return null;
            }

            const normalized = payload.replace(/-/g, "+").replace(/_/g, "/");
            return JSON.parse(atob(normalized));
        } catch (_error) {
            return null;
        }
    }

    function readRoomFromToken(token) {
        return readTokenPayload(token)?.video?.room || null;
    }

    function readIdentityFromToken(token) {
        return readTokenPayload(token)?.sub || null;
    }

    function readDisplayNameFromToken(token) {
        const name = readTokenPayload(token)?.name;
        return name && String(name).trim() ? String(name).trim() : null;
    }

    function showJoinScreen() {
        loadingScreen.hidden = true;
        errorScreen.hidden = true;
        roomScreen.hidden = true;
        permissionScreen.hidden = false;
    }

    function setJoinButtonsDisabled(disabled) {
        if (joinRoomButton) joinRoomButton.disabled = disabled;
    }

    async function handleJoinRoomClick() {
        if (!connectionParams || connectPromise || isConnecting) {
            return;
        }

        setJoinButtonsDisabled(true);
        clearPermissionStatus();
        joinRoomButton.textContent = "Connexion...";

        try {
            await ensureRoomConnected();
            resumeAudioContext();
            showRoom();
            refreshParticipantCount();
            syncParticipantTiles();
            syncControlStates();
            updateSpeakingIndicators(room.activeSpeakers);
            refreshAllTileStatuses();
            updateVideoGridLayout();
            void refreshDeviceMenus();
        } catch (error) {
            console.error("Connexion LiveKit impossible :", error);
            await resetRoom();
            showPermissionStatus(formatConnectionError(error, connectionParams.serverUrl), "error");
        } finally {
            joinRoomButton.textContent = "Rejoindre la salle";
            setJoinButtonsDisabled(false);
        }
    }

    function getOrCreateRoom() {
        if (room) {
            return room;
        }

        room = new LiveKitClient.Room({
            adaptiveStream: true,
            dynacast: true,
            disconnectOnPageLeave: true,
            reconnectPolicy: {
                nextRetryDelayInMs: () => null,
            },
        });

        bindRoomEvents(room);
        bindRoomControlsOnce();
        return room;
    }

    async function stopLocalMedia(currentRoom) {
        if (!currentRoom) {
            return;
        }

        try {
            await currentRoom.localParticipant.setCameraEnabled(false);
            await currentRoom.localParticipant.setMicrophoneEnabled(false);
            await currentRoom.localParticipant.setScreenShareEnabled(false);
        } catch (_error) {
            // Tracks may already be stopped.
        }
    }

    async function resetRoom() {
        const previousRoom = room;
        room = null;

        if (!previousRoom) {
            connectPromise = null;
            isConnecting = false;
            return;
        }

        const previousIntentional = intentionalLeave;
        intentionalLeave = true;

        try {
            await stopLocalMedia(previousRoom);
            await previousRoom.disconnect(true);
        } catch (_error) {
            // Ignore cleanup errors on stale sessions.
        } finally {
            intentionalLeave = previousIntentional;
            connectPromise = null;
            isConnecting = false;
        }

        clearMediaTiles();
    }

    function closeAfterLeave() {
        window.close();

        // Si le navigateur refuse de fermer l'onglet (non ouvert par script), repli vers l'accueil.
        setTimeout(() => {
            const target = config.backLink || window.location.pathname || "/";
            window.location.replace(target);
        }, 150);
    }

    async function leaveSession() {
        intentionalLeave = true;
        if (leaveButton) {
            leaveButton.disabled = true;
        }

        try {
            notifyOtherVisioTabs("leave");
            if (room) {
                await requestRoomEndIfLastParticipant(room);
            }
            await resetRoom();
        } finally {
            closeAfterLeave();
        }
    }

    function clearMediaTiles() {
        setChatOpen(false);
        if (chatMessages) {
            chatMessages.innerHTML = "";
        }
        closeDeviceMenus();

        for (const tile of tiles.values()) {
            tile.element.remove();
        }
        tiles.clear();

        for (const entry of remoteAudio.values()) {
            entry.element.remove();
        }
        remoteAudio.clear();
        audioVolumes.clear();
        focusedTileKey = null;
        galleryPageIndex = 0;

        if (videoGrid) {
            videoGrid.replaceChildren();
        }
    }

    function isRoomConnected(currentRoom) {
        return currentRoom?.state === LiveKitClient.ConnectionState.Connected;
    }

    function isRoomConnecting(currentRoom) {
        const states = LiveKitClient.ConnectionState || {};
        return (
            currentRoom?.state === states.Connecting ||
            currentRoom?.state === states.Reconnecting
        );
    }

    async function ensureRoomConnected() {
        if (isRoomConnected(room)) {
            return room;
        }

        if (connectPromise) {
            return connectPromise;
        }

        if (room && !isRoomConnecting(room)) {
            intentionalLeave = true;
            await resetRoom();
            intentionalLeave = false;
        }

        await waitForOtherTabsToLeave();

        const currentRoom = getOrCreateRoom();
        const {serverUrl, token} = connectionParams;
        let timeoutId;

        isConnecting = true;
        connectPromise = Promise.race([
            currentRoom.connect(serverUrl, token),
            new Promise((_, reject) => {
                timeoutId = setTimeout(() => {
                    reject(new Error(CONNECT_TIMEOUT_MESSAGE));
                }, config.connectTimeoutMs || 20000);
            }),
        ])
            .then(() => currentRoom)
            .catch(async (error) => {
                intentionalLeave = true;
                await resetRoom();
                intentionalLeave = false;
                throw error;
            })
            .finally(() => {
                clearTimeout(timeoutId);
                isConnecting = false;
                connectPromise = null;
            });

        return connectPromise;
    }

    function isParticipantMuted(participant) {
        return participant ? !participant.isMicrophoneEnabled : false;
    }

    function refreshAllTileStatuses() {
        for (const tile of tiles.values()) {
            updateTileStatus(tile);
        }
    }

    function withTimeout(promise, timeoutMs, timeoutMessage) {
        return Promise.race([
            promise,
            new Promise((_, reject) => {
                setTimeout(() => reject(new Error(timeoutMessage)), timeoutMs);
            }),
        ]);
    }

    function formatDisconnectReason(reason) {
        const reasons = LiveKitClient.DisconnectReason || {};

        if (reason === reasons.INVALID_TOKEN) {
            return "Token LiveKit invalide ou expiré. Rejoignez la salle depuis l'application SteeveJobs.";
        }

        if (reason === reasons.DUPLICATE_IDENTITY) {
            return "Une session est déjà ouverte avec votre compte. Fermez les autres onglets visio, attendez 30 secondes, puis réessayez depuis l'application.";
        }

        if (reason === reasons.PARTICIPANT_REMOVED) {
            return "Vous avez été expulsé de la salle par l'organisateur.";
        }

        if (reason === reasons.ROOM_DELETED) {
            return "La salle a été fermée.";
        }

        if (reason === reasons.JOIN_FAILURE) {
            return "Impossible de rester connecté à la salle LiveKit.";
        }

        return "Connexion à la visioconférence interrompue.";
    }

    function handleUnexpectedDisconnect(reason) {
        if (intentionalLeave || isConnecting) {
            return;
        }

        const previousRoom = room;
        console.error("Déconnexion LiveKit :", reason);
        if (previousRoom) {
            void requestRoomEndIfLastParticipant(previousRoom);
        }
        void resetRoom().then(() => {
            showJoinScreen();
            showPermissionStatus(formatDisconnectReason(reason), "error");
        });
    }

    function formatConnectionError(error, serverUrl) {
        const message = error?.message || String(error);

        if (
            message.includes("invalid token") ||
            message.includes("401") ||
            message.includes("cryptographic primitive")
        ) {
            return "Token de connexion invalide ou expiré. Rejoignez la salle depuis l'application SteeveJobs.";
        }

        if (
            message.includes("leave request") ||
            message.includes("DUPLICATE") ||
            message.includes("already connected")
        ) {
            return "Une session est déjà ouverte avec votre compte. Fermez tous les onglets visio, attendez 30 secondes, puis cliquez à nouveau sur Rejoindre dans l'application.";
        }

        if (
            message.includes("Délai dépassé") ||
            message.includes("Failed to fetch") ||
            message.includes("ne répond pas")
        ) {
            return CONNECT_TIMEOUT_MESSAGE;
        }

        if (message.includes("WebSocket") || message.includes("signal connection")) {
            return "Connexion WebSocket refusée. Vérifiez l'URL LiveKit (wss://) et le certificat SSL.";
        }

        return message || "Impossible de rejoindre la visioconférence.";
    }

    function showPermissionStatus(message, type) {
        if (!permissionStatus) {
            return;
        }

        permissionStatus.hidden = false;
        permissionStatus.textContent = message;
        permissionStatus.className = `permission-status ${type}`;
    }

    function clearPermissionStatus() {
        if (!permissionStatus) {
            return;
        }

        permissionStatus.hidden = true;
        permissionStatus.textContent = "";
        permissionStatus.className = "permission-status";
    }

    function isRoomCreator() {
        return Boolean(
            sessionIdentity &&
            sessionRoomCreatorId &&
            sessionKickToken &&
            String(sessionIdentity) === String(sessionRoomCreatorId)
        );
    }

    function getKickApiUrl() {
        if (config.kickApiUrl) {
            return config.kickApiUrl;
        }

        return new URL("/api/visio/kick", window.location.origin).href;
    }

    function getEndRoomApiUrl() {
        if (config.endRoomApiUrl) {
            return config.endRoomApiUrl;
        }

        return new URL("/api/visio/end-room", window.location.origin).href;
    }

    function countRoomParticipants(currentRoom) {
        if (!currentRoom) {
            return 0;
        }

        return currentRoom.remoteParticipants.size + 1;
    }

    function requestRoomEndIfLastParticipant(currentRoom) {
        if (!currentRoom || !connectionParams?.roomName || !sessionEndToken) {
            return Promise.resolve();
        }

        if (countRoomParticipants(currentRoom) > 1) {
            return Promise.resolve();
        }

        const payload = JSON.stringify({
            roomName: connectionParams.roomName,
            endToken: sessionEndToken,
        });

        const url = getEndRoomApiUrl();

        if (navigator.sendBeacon) {
            navigator.sendBeacon(url, new Blob([payload], {type: "application/json"}));
            return Promise.resolve();
        }

        return fetch(url, {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: payload,
            keepalive: true,
        }).catch((error) => {
            console.warn("Fermeture automatique du salon impossible :", error);
        });
    }

    async function kickParticipant(participant) {
        if (!isRoomCreator() || !connectionParams?.roomName || !participant?.identity) {
            return;
        }

        if (String(participant.identity) === String(sessionIdentity)) {
            return;
        }

        const participantName = formatParticipantName(participant);
        if (!window.confirm(`Expulser ${participantName} de la salle ?`)) {
            return;
        }

        try {
            const response = await fetch(getKickApiUrl(), {
                method: "POST",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({
                    kickToken: sessionKickToken,
                    roomName: connectionParams.roomName,
                    targetIdentity: participant.identity,
                }),
            });

            const data = await response.json().catch(() => ({}));
            if (!response.ok || data.status !== "SUCCESS") {
                console.error("Expulsion impossible :", data.message || response.statusText);
            }
        } catch (error) {
            console.error("Expulsion impossible :", error);
        }
    }

    function bindRoomEvents(currentRoom) {
        currentRoom.on(LiveKitClient.RoomEvent.TrackSubscribed, (track, _publication, participant) => {
            attachTrack(track, participant);
        });

        currentRoom.on(LiveKitClient.RoomEvent.TrackUnsubscribed, (track, publication, participant) => {
            if (publication?.source === LiveKitClient.Track.Source.Camera) {
                syncCameraTileMedia(participant);
                return;
            }
            detachTrack(track);
        });

        currentRoom.on(LiveKitClient.RoomEvent.LocalTrackPublished, (publication, participant) => {
            if (publication.source === LiveKitClient.Track.Source.Camera) {
                syncCameraTileMedia(participant);
                return;
            }
            if (publication.track) {
                attachTrack(publication.track, participant);
            }
        });

        currentRoom.on(LiveKitClient.RoomEvent.LocalTrackUnpublished, (publication, participant) => {
            if (publication.source === LiveKitClient.Track.Source.Camera) {
                syncCameraTileMedia(participant);
                return;
            }
            if (publication.track) {
                detachTrack(publication.track);
            }
        });

        currentRoom.on(LiveKitClient.RoomEvent.TrackUnpublished, (publication, participant) => {
            if (publication.source === LiveKitClient.Track.Source.Camera) {
                syncCameraTileMedia(participant);
                return;
            }
            refreshAllTileStatuses();
        });

        currentRoom.on(LiveKitClient.RoomEvent.ParticipantConnected, (participant) => {
            ensureCameraTile(participant);
            refreshParticipantCount();
            updateVideoGridLayout();
            refreshAllTileStatuses();
        });
        currentRoom.on(LiveKitClient.RoomEvent.ParticipantDisconnected, (participant) => {
            removeParticipantTiles(participant.identity);
            refreshParticipantCount();
            updateVideoGridLayout();
        });

        const refreshTileMedia = () => {
            refreshAllTileStatuses();
            syncAllCameraTiles();
            syncControlStates();
        };
        currentRoom.on(LiveKitClient.RoomEvent.TrackMuted, refreshTileMedia);
        currentRoom.on(LiveKitClient.RoomEvent.TrackUnmuted, refreshTileMedia);
        currentRoom.on(LiveKitClient.RoomEvent.TrackPublished, refreshTileMedia);
        currentRoom.localParticipant.on(
            LiveKitClient.ParticipantEvent.TrackMuted,
            refreshTileMedia
        );
        currentRoom.localParticipant.on(
            LiveKitClient.ParticipantEvent.TrackUnmuted,
            refreshTileMedia
        );

        currentRoom.on(LiveKitClient.RoomEvent.Disconnected, (reason) => {
            handleUnexpectedDisconnect(reason);
        });

        currentRoom.on(LiveKitClient.RoomEvent.MediaDevicesError, (error) => {
            console.warn("Erreur périphérique média :", error);
        });

        currentRoom.on(LiveKitClient.RoomEvent.ActiveSpeakersChanged, (speakers) => {
            updateSpeakingIndicators(speakers);
        });

        currentRoom.on(LiveKitClient.RoomEvent.MediaDevicesChanged, () => {
            void refreshDeviceMenus();
        });

        currentRoom.on(LiveKitClient.RoomEvent.ActiveDeviceChanged, () => {
            void refreshDeviceMenus();
        });

        // Chat events
        currentRoom.on("dataReceived", (payload, participant) => {
            try {
                const decoder = new TextDecoder();
                const message = decoder.decode(payload);
                const displayName = participant?.name || participant?.identity || "Utilisateur";
                addChatMessage(displayName, message, false);
            } catch (error) {
                console.error("Erreur décodage message :", error);
            }
        });

        // Setup chat UI
        setupChatBindings(currentRoom);
    }

    function getDeviceMenuElements(kind) {
        if (kind === DEVICE_KIND.audio) {
            return {button: micDeviceMenuBtn, list: micDeviceList, title: "Microphones"};
        }

        return {button: camDeviceMenuBtn, list: camDeviceList, title: "Caméras"};
    }

    function closeDeviceMenus() {
        openDeviceMenuKind = null;

        for (const kind of [DEVICE_KIND.audio, DEVICE_KIND.video]) {
            const {button, list} = getDeviceMenuElements(kind);
            if (button) {
                button.setAttribute("aria-expanded", "false");
            }
            if (list) {
                list.hidden = true;
            }
        }
    }

    function toggleDeviceMenu(kind) {
        if (openDeviceMenuKind === kind) {
            closeDeviceMenus();
            return;
        }

        closeDeviceMenus();
        openDeviceMenuKind = kind;
        void renderDeviceMenu(kind);

        const {button, list} = getDeviceMenuElements(kind);
        if (button) {
            button.setAttribute("aria-expanded", "true");
        }
        if (list) {
            list.hidden = false;
        }
    }

    function formatDeviceLabel(device, index, kind) {
        if (device?.label?.trim()) {
            return device.label.trim();
        }

        return kind === DEVICE_KIND.audio ? `Micro ${index + 1}` : `Caméra ${index + 1}`;
    }

    async function renderDeviceMenu(kind) {
        const {list, title} = getDeviceMenuElements(kind);
        if (!list || !room) {
            return;
        }

        list.innerHTML = "";

        const heading = document.createElement("p");
        heading.className = "device-menu-title";
        heading.textContent = title;
        list.appendChild(heading);

        let devices = [];
        try {
            devices = await LiveKitClient.Room.getLocalDevices(kind, true);
        } catch (error) {
            console.warn("Impossible de lister les périphériques :", error);
        }

        if (!devices.length) {
            const empty = document.createElement("p");
            empty.className = "device-menu-empty";
            empty.textContent = "Aucun périphérique détecté";
            list.appendChild(empty);
            return;
        }

        const activeDeviceId = room.getActiveDevice(kind) || devices[0]?.deviceId;

        devices.forEach((device, index) => {
            const item = document.createElement("button");
            item.type = "button";
            item.className = "device-menu-item";
            item.setAttribute("role", "option");
            item.textContent = formatDeviceLabel(device, index, kind);

            if (device.deviceId === activeDeviceId) {
                item.classList.add("is-active");
                item.setAttribute("aria-selected", "true");
            } else {
                item.setAttribute("aria-selected", "false");
            }

            item.addEventListener("click", (event) => {
                event.stopPropagation();
                void switchMediaDevice(kind, device.deviceId);
            });

            list.appendChild(item);
        });
    }

    async function refreshDeviceMenus() {
        if (!room) {
            return;
        }

        if (openDeviceMenuKind) {
            await renderDeviceMenu(openDeviceMenuKind);
        }
    }

    async function switchMediaDevice(kind, deviceId) {
        if (!room || !deviceId) {
            return;
        }

        try {
            await room.switchActiveDevice(kind, deviceId);

            if (kind === DEVICE_KIND.video) {
                syncCameraTileMedia(room.localParticipant);
            }

            syncControlStates();
            refreshAllTileStatuses();
            closeDeviceMenus();
        } catch (error) {
            console.error("Changement de périphérique impossible :", error);
        }
    }

    function updateSpeakingIndicators(speakers) {
        const speakingIds = new Set(
            (speakers || [])
                .filter((participant) => participant?.isMicrophoneEnabled)
                .map((participant) => participant.identity)
        );

        for (const tile of tiles.values()) {
            if (tile.isScreenShare) {
                tile.element.classList.remove("is-speaking");
                continue;
            }

            const isSpeaking = speakingIds.has(tile.participant.identity);
            tile.element.classList.toggle("is-speaking", isSpeaking);
        }
    }

    function bindDeviceMenus() {
        if (deviceMenusBound) {
            return;
        }

        deviceMenusBound = true;

        bindClick(micDeviceMenuBtn, (event) => {
            event.stopPropagation();
            toggleDeviceMenu(DEVICE_KIND.audio);
        });

        bindClick(camDeviceMenuBtn, (event) => {
            event.stopPropagation();
            toggleDeviceMenu(DEVICE_KIND.video);
        });

        document.addEventListener("click", (event) => {
            if (
                event.target.closest(".control-split") ||
                event.target.closest(".device-menu")
            ) {
                return;
            }

            closeDeviceMenus();
        });

        document.addEventListener("keydown", (event) => {
            if (event.key === "Escape") {
                closeDeviceMenus();
            }
        });
    }

    function bindRoomControlsOnce() {
        if (controlsBound) {
            return;
        }

        controlsBound = true;
        bindDeviceMenus();

        leaveButton.addEventListener("click", () => {
            void leaveSession();
        });

        toggleMic.addEventListener("click", async () => {
            if (!room) {
                return;
            }

            const enabled = room.localParticipant.isMicrophoneEnabled;
            const requestedState = !enabled;
            toggleMic.classList.toggle("off", !requestedState);
            const micIconOn = toggleMic.querySelector(".icon-mic-on");
            const micIconOff = toggleMic.querySelector(".icon-mic-off");
            if (micIconOn) {
                if (requestedState) {
                    micIconOn.removeAttribute("hidden");
                } else {
                    micIconOn.setAttribute("hidden", "");
                }
            }
            if (micIconOff) {
                if (requestedState) {
                    micIconOff.setAttribute("hidden", "");
                } else {
                    micIconOff.removeAttribute("hidden");
                }
            }

            await room.localParticipant.setMicrophoneEnabled(requestedState);
            syncControlStates();
            refreshAllTileStatuses();
        });

        toggleCam.addEventListener("click", async () => {
            if (!room) {
                return;
            }

            const enabled = room.localParticipant.isCameraEnabled;
            const requestedState = !enabled;
            toggleCam.classList.toggle("off", !requestedState);
            const camIconOn = toggleCam.querySelector(".icon-cam-on");
            const camIconOff = toggleCam.querySelector(".icon-cam-off");
            if (camIconOn) {
                if (requestedState) {
                    camIconOn.removeAttribute("hidden");
                } else {
                    camIconOn.setAttribute("hidden", "");
                }
            }
            if (camIconOff) {
                if (requestedState) {
                    camIconOff.setAttribute("hidden", "");
                } else {
                    camIconOff.removeAttribute("hidden");
                }
            }

            await room.localParticipant.setCameraEnabled(requestedState);
            syncControlStates();
            syncCameraTileMedia(room.localParticipant);
            refreshAllTileStatuses();
        });

        toggleScreen.addEventListener("click", async () => {
            if (!room) {
                return;
            }

            const enabled = room.localParticipant.isScreenShareEnabled;

            try {
                // Chrome plante souvent si on force audio:true (écran/fenêtre).
                // Le son d'onglet n'est pas fiable côté navigateur : partage vidéo seul.
                await room.localParticipant.setScreenShareEnabled(!enabled);
                syncControlStates();
            } catch (error) {
                console.error("Partage d'écran impossible :", error);
                syncControlStates();
            }
        });
    }

    function getCameraTileKey(identity) {
        return `${identity}-${LiveKitClient.Track.Source.Camera}`;
    }

    function getScreenTileKey(identity) {
        return `${identity}-${LiveKitClient.Track.Source.ScreenShare}`;
    }

    function getVolumeKey(identity, kind) {
        return `${identity}-${kind}`;
    }

    function isScreenShareAudioTrack(track) {
        return (
            track.source === LiveKitClient.Track.Source.ScreenShareAudio ||
            track.source === "screen_share_audio"
        );
    }

    function attachScreenShareAudioIfAvailable(participant) {
        if (participant.isLocal) {
            return;
        }

        const publication = participant.getTrackPublication(
            LiveKitClient.Track.Source.ScreenShareAudio
        );
        if (publication?.track) {
            attachRemoteAudio(publication.track, participant);
        }
    }

    function updateScreenTileVolumeVisibility(tile) {
        if (!tile?.isScreenShare || tile.participant?.isLocal) {
            return;
        }

        const hasScreenAudio = Boolean(
            tile.participant.getTrackPublication(LiveKitClient.Track.Source.ScreenShareAudio)?.track
        );
        tile.element.classList.toggle("no-stream-audio", !hasScreenAudio);
    }

    function getAudioContext() {
        if (!audioContext) {
            audioContext = new (window.AudioContext || window.webkitAudioContext)();
        }
        return audioContext;
    }

    function resumeAudioContext() {
        const context = getAudioContext();
        if (context.state === "suspended") {
            void context.resume();
        }
    }

    function getStoredVolume(volumeKey) {
        return audioVolumes.has(volumeKey) ? audioVolumes.get(volumeKey) : VOLUME_DEFAULT;
    }

    function setStoredVolume(volumeKey, value) {
        const clamped = Math.max(VOLUME_MIN, Math.min(VOLUME_MAX, value));
        audioVolumes.set(volumeKey, clamped);
        applyVolumeToAudio(volumeKey);
        refreshTilesForVolumeKey(volumeKey);
    }

    function ensureAudioGraph(entry) {
        if (entry.gainNode) {
            return;
        }

        resumeAudioContext();
        const context = getAudioContext();
        entry.sourceNode = context.createMediaElementSource(entry.element);
        entry.gainNode = context.createGain();
        entry.sourceNode.connect(entry.gainNode);
        entry.gainNode.connect(context.destination);
        entry.element.volume = 1;
    }

    function applyVolumeToAudio(volumeKey) {
        const entry = remoteAudio.get(volumeKey);
        if (!entry?.element) {
            return;
        }

        ensureAudioGraph(entry);
        entry.gainNode.gain.value = getStoredVolume(volumeKey);
    }

    function refreshTilesForVolumeKey(volumeKey) {
        for (const tile of tiles.values()) {
            if (tile.volumeKey === volumeKey) {
                updateTileStatus(tile);
            }
        }
    }

    function volumePercent(volume) {
        return Math.round(volume * 100);
    }

    function volumeFromPercent(percent) {
        return percent / 100;
    }

    function updateVolumeButton(tile) {
        if (!tile.volumeBtn) {
            return;
        }

        const volume = getStoredVolume(tile.volumeKey);
        const muted = volume === 0;

        tile.volumeBtn.classList.toggle("muted", muted);
        tile.volumeBtn.innerHTML = muted ? SPEAKER_OFF_ICON : SPEAKER_ICON;
        tile.volumeBtn.title = muted ? "Activer le son" : "Couper le son";

        if (tile.volumeSlider) {
            tile.volumeSlider.value = String(volumePercent(volume));
        }
    }

    function toggleVolumeMute(tile) {
        const current = getStoredVolume(tile.volumeKey);

        if (current > 0) {
            tile.volumeBeforeMute = current;
            setStoredVolume(tile.volumeKey, 0);
            return;
        }

        setStoredVolume(tile.volumeKey, tile.volumeBeforeMute ?? VOLUME_DEFAULT);
    }

    function hasActiveCameraVideo(participant) {
        const publication = participant.getTrackPublication(LiveKitClient.Track.Source.Camera);
        if (!publication?.track || publication.track.kind !== "video") {
            return false;
        }

        if (publication.isMuted) {
            return false;
        }

        if (!participant.isLocal && !publication.isSubscribed) {
            return false;
        }

        return true;
    }

    function syncAllCameraTiles() {
        if (!room) {
            return;
        }

        syncCameraTileMedia(room.localParticipant);
        for (const participant of room.remoteParticipants.values()) {
            syncCameraTileMedia(participant);
        }
    }

    function syncCameraTileMedia(participant) {
        const tileKey = getCameraTileKey(participant.identity);
        let tile = tiles.get(tileKey);

        if (!tile) {
            ensureCameraTile(participant);
            tile = tiles.get(tileKey);
        }

        if (!tile || tile.isScreenShare) {
            return;
        }

        tile.participant = participant;

        if (hasActiveCameraVideo(participant)) {
            const track = participant.getTrackPublication(LiveKitClient.Track.Source.Camera).track;

            if (tile.trackSid === track.sid && tile.mediaContainer.querySelector("video")) {
                tile.element.classList.remove("no-video");
                updateTileStatus(tile);
                return;
            }

            tile.trackSid = track.sid;
            applyTileSourceClasses(tile.element, participant, track);

            const videoEl = track.attach();
            videoEl.style.transform = "none";
            tile.mediaContainer.replaceChildren(videoEl);
            tile.element.classList.remove("no-video");
        } else {
            tile.trackSid = null;
            showAvatarOnTile(tile);
        }

        updateTileStatus(tile);
    }

    function getTileKey(participant, isScreenShare) {
        return isScreenShare
            ? getScreenTileKey(participant.identity)
            : getCameraTileKey(participant.identity);
    }

    function toggleTileFocus(tileKey) {
        if (!tiles.has(tileKey)) {
            return;
        }

        focusedTileKey = focusedTileKey === tileKey ? null : tileKey;
        applyTileFocusState();
    }

    function applyTileFocusState() {
        if (!videoGrid) {
            return;
        }

        const isFocused = Boolean(focusedTileKey && tiles.has(focusedTileKey));
        if (!isFocused) {
            focusedTileKey = null;
        }

        videoGrid.classList.toggle("is-focused", isFocused);

        if (galleryNav) {
            galleryNav.hidden = isFocused;
        }

        for (const [key, tile] of tiles.entries()) {
            const isTarget = isFocused && key === focusedTileKey;
            tile.element.classList.toggle("is-focused-tile", isTarget);
            tile.element.setAttribute("aria-pressed", isTarget ? "true" : "false");
            if (isFocused) {
                tile.element.hidden = !isTarget;
            }
        }

        if (isFocused) {
            videoGrid.dataset.layout = "focus";
            videoGrid.dataset.count = "1";
        } else {
            updateVideoGridLayout();
        }
    }

    function clearFocusIfTileRemoved(tileKey) {
        if (focusedTileKey === tileKey) {
            focusedTileKey = null;
        }
    }

    function syncParticipantTiles() {
        if (!room) {
            return;
        }

        ensureCameraTile(room.localParticipant);
        for (const participant of room.remoteParticipants.values()) {
            ensureCameraTile(participant);
        }
        syncAllCameraTiles();
        updateVideoGridLayout();
    }

    function ensureCameraTile(participant) {
        const tileKey = getCameraTileKey(participant.identity);
        let tile = tiles.get(tileKey);

        if (!tile) {
            tile = createTile(participant, null, {
                isScreenShare: false,
                tileKey,
            });
            tiles.set(tileKey, tile);
            videoGrid.appendChild(tile.element);
            showAvatarOnTile(tile);
        }

        tile.participant = participant;
        updateTileStatus(tile);
        return tile;
    }

    function showAvatarOnTile(tile) {
        tile.mediaContainer.replaceChildren();
        tile.element.classList.add("no-video");
        tile.mediaContainer.appendChild(createAvatarPlaceholder());
    }

    function attachTrack(track, participant) {
        if (track.kind === "audio") {
            if (participant.isLocal) {
                return;
            }
            attachRemoteAudio(track, participant);
            return;
        }

        if (track.kind !== "video") {
            return;
        }

        const isScreenShare = track.source === LiveKitClient.Track.Source.ScreenShare;
        const tileKey = isScreenShare
            ? getScreenTileKey(participant.identity)
            : getCameraTileKey(participant.identity);
        let tile = tiles.get(tileKey);

        if (!tile) {
            tile = createTile(participant, track, {isScreenShare, tileKey});
            tiles.set(tileKey, tile);
            videoGrid.appendChild(tile.element);
        } else {
            tile.trackSid = track.sid;
            tile.isScreenShare = isScreenShare;
            applyTileSourceClasses(tile.element, participant, track);
        }

        const videoEl = track.attach();
        videoEl.style.transform = "none";
        tile.mediaContainer.replaceChildren(videoEl);
        tile.element.classList.remove("no-video");
        tile.participant = participant;
        updateTileStatus(tile);
        if (isScreenShare) {
            attachScreenShareAudioIfAvailable(participant);
            updateScreenTileVolumeVisibility(tile);
        }
        updateVideoGridLayout();
    }

    function attachRemoteAudio(track, participant) {
        const isScreenAudio = isScreenShareAudioTrack(track);
        const volumeKey = getVolumeKey(
            participant.identity,
            isScreenAudio ? "screen" : "mic"
        );

        detachRemoteAudioKey(volumeKey);

        const audioEl = track.attach();
        audioEl.hidden = true;
        document.body.appendChild(audioEl);

        const entry = {element: audioEl, trackSid: track.sid, volumeKey};
        remoteAudio.set(volumeKey, entry);
        resumeAudioContext();
        applyVolumeToAudio(volumeKey);

        for (const tile of tiles.values()) {
            if (tile.participant?.identity === participant.identity && tile.isScreenShare) {
                updateScreenTileVolumeVisibility(tile);
            }
        }
    }

    function detachRemoteAudioKey(volumeKey) {
        const entry = remoteAudio.get(volumeKey);
        if (!entry) {
            return;
        }

        entry.element.remove();
        remoteAudio.delete(volumeKey);
    }

    function detachRemoteAudioByIdentity(identity) {
        detachRemoteAudioKey(getVolumeKey(identity, "mic"));
        detachRemoteAudioKey(getVolumeKey(identity, "screen"));
    }

    function detachTrack(track) {
        if (track.kind === "audio") {
            for (const [volumeKey, entry] of remoteAudio.entries()) {
                if (entry.trackSid === track.sid) {
                    entry.element.remove();
                    remoteAudio.delete(volumeKey);
                    return;
                }
            }
            return;
        }

        for (const [key, tile] of tiles.entries()) {
            if (tile.trackSid !== track.sid) {
                continue;
            }

            track.detach();

            if (track.kind === "video") {
                if (track.source === LiveKitClient.Track.Source.ScreenShare) {
                    clearFocusIfTileRemoved(key);
                    tile.element.remove();
                    tiles.delete(key);
                    applyTileFocusState();
                    updateVideoGridLayout();
                } else {
                    tile.trackSid = null;
                    showAvatarOnTile(tile);
                    updateTileStatus(tile);
                }
            }
        }
    }

    function removeParticipantTiles(identity) {
        detachRemoteAudioByIdentity(identity);

        for (const [key, tile] of tiles.entries()) {
            if (tile.participant.identity === identity) {
                clearFocusIfTileRemoved(key);
                tile.element.remove();
                tiles.delete(key);
            }
        }

        for (const volumeKey of [...audioVolumes.keys()]) {
            if (volumeKey.startsWith(`${identity}-`)) {
                audioVolumes.delete(volumeKey);
            }
        }

        applyTileFocusState();
        updateVideoGridLayout();
    }

    function getOrderedTiles() {
        return Array.from(tiles.values()).sort((tileA, tileB) => {
            const localRank = (tile) => {
                if (tile.participant.isLocal && !tile.isScreenShare) {
                    return 0;
                }
                if (tile.isScreenShare) {
                    return 2;
                }
                return 1;
            };

            const rankDiff = localRank(tileA) - localRank(tileB);
            if (rankDiff !== 0) {
                return rankDiff;
            }

            return formatParticipantName(tileA.participant).localeCompare(
                formatParticipantName(tileB.participant),
                "fr"
            );
        });
    }

    function getGalleryPageCount(tileCount = tiles.size) {
        return Math.max(1, Math.ceil(tileCount / GALLERY_PAGE_SIZE));
    }

    function getVisibleTileCountForLayout() {
        if (tiles.size > GALLERY_PAGE_SIZE) {
            return GALLERY_PAGE_SIZE;
        }
        return tiles.size;
    }

    function changeGalleryPage(delta) {
        const pageCount = getGalleryPageCount();
        const nextPage = galleryPageIndex + delta;
        if (nextPage < 0 || nextPage >= pageCount) {
            return;
        }

        galleryPageIndex = nextPage;
        applyGalleryPagination();
        updateVideoGridLayout();
    }

    function applyGalleryPagination() {
        const ordered = getOrderedTiles();
        const total = ordered.length;

        if (total <= GALLERY_PAGE_SIZE) {
            galleryPageIndex = 0;
            ordered.forEach((tile) => {
                tile.element.hidden = false;
            });
            if (galleryNav) {
                galleryNav.hidden = true;
            }
            return;
        }

        const pageCount = getGalleryPageCount(total);
        galleryPageIndex = Math.min(galleryPageIndex, pageCount - 1);
        galleryPageIndex = Math.max(galleryPageIndex, 0);

        const start = galleryPageIndex * GALLERY_PAGE_SIZE;
        const visibleKeys = new Set(
            ordered.slice(start, start + GALLERY_PAGE_SIZE).map((tile) => tile.tileKey)
        );

        ordered.forEach((tile) => {
            tile.element.hidden = !visibleKeys.has(tile.tileKey);
        });

        if (galleryNav) {
            galleryNav.hidden = false;
        }
        if (galleryStatus) {
            galleryStatus.textContent = `${galleryPageIndex + 1} / ${pageCount}`;
        }
        if (galleryPrev) {
            galleryPrev.disabled = galleryPageIndex === 0;
        }
        if (galleryNext) {
            galleryNext.disabled = galleryPageIndex >= pageCount - 1;
        }
    }

    function updateVideoGridLayout() {
        if (!videoGrid) {
            return;
        }

        if (focusedTileKey) {
            applyTileFocusState();
            return;
        }

        const tileCount = tiles.size;
        let layout = "solo";

        if (tileCount === 2) {
            layout = "duo";
        } else if (tileCount === 3) {
            layout = "triple";
        } else if (tileCount === 4) {
            layout = "quad";
        } else if (tileCount === 5) {
            layout = "five";
        } else if (tileCount === 6) {
            layout = "six";
        } else if (tileCount > GALLERY_PAGE_SIZE) {
            layout = "nine";
        } else if (tileCount > 6) {
            layout = "many";
        }

        applyGalleryPagination();

        videoGrid.dataset.layout = layout;
        videoGrid.dataset.count = String(tileCount);

        if (layout === "many" || layout === "nine") {
            updateManyGridMetrics(getVisibleTileCountForLayout());
        } else {
            videoGrid.style.removeProperty("--grid-rows");
            videoGrid.style.removeProperty("--grid-cols");
            if (galleryNav) {
                galleryNav.hidden = true;
            }
        }
    }

    function getManyGridCols() {
        if (window.matchMedia("(max-width: 720px)").matches) {
            return 1;
        }
        if (window.matchMedia("(max-width: 960px)").matches) {
            return 2;
        }
        return 3;
    }

    function updateManyGridMetrics(visibleCount) {
        if (!videoGrid) {
            return;
        }

        if (videoGrid.dataset.layout === "nine") {
            videoGrid.style.setProperty("--grid-rows", "3");
            videoGrid.style.setProperty("--grid-cols", "3");
            return;
        }

        const cols = getManyGridCols();
        const rows = Math.max(1, Math.ceil(visibleCount / cols));
        videoGrid.style.setProperty("--grid-rows", String(rows));
        videoGrid.style.setProperty("--grid-cols", String(cols));
    }

    function updateTileStatus(tile) {
        if (!tile?.nameElement || !tile.participant) {
            return;
        }

        tile.nameElement.textContent = participantLabel(tile.participant);

        if (!tile.muteBadge) {
            return;
        }

        if (tile.isScreenShare) {
            tile.muteBadge.hidden = true;
            if (tile.kickBtn) {
                tile.kickBtn.hidden = true;
            }
            updateScreenTileVolumeVisibility(tile);
            updateVolumeButton(tile);
            return;
        }

        tile.muteBadge.hidden = !isParticipantMuted(tile.participant);
        tile.muteBadge.title = "Micro coupé";

        if (tile.kickBtn) {
            tile.kickBtn.hidden = !(isRoomCreator() && !tile.participant.isLocal && !tile.isScreenShare);
        }

        updateVolumeButton(tile);
    }

    function applyTileSourceClasses(element, participant, track) {
        element.classList.remove("local", "camera", "screen-share");

        const isScreenShare = track.source === LiveKitClient.Track.Source.ScreenShare;

        if (isScreenShare) {
            element.classList.add("screen-share");
            return;
        }

        element.classList.add("camera");
        if (participant.isLocal) {
            element.classList.add("local");
        }
    }

    function createTile(participant, track, options = {}) {
        const isScreenShare = track
            ? track.source === LiveKitClient.Track.Source.ScreenShare
            : options.isScreenShare === true;
        const volumeKey = getVolumeKey(
            participant.identity,
            isScreenShare ? "screen" : "mic"
        );
        const tileKey = options.tileKey || getTileKey(participant, isScreenShare);

        const element = document.createElement("article");
        element.className = "video-tile";
        element.setAttribute("role", "button");
        element.tabIndex = 0;
        element.setAttribute("aria-pressed", "false");
        element.title = "Cliquer pour agrandir";

        if (track) {
            applyTileSourceClasses(element, participant, track);
        } else {
            element.classList.add("camera");
            if (participant.isLocal) {
                element.classList.add("local");
            }
        }

        const mediaContainer = document.createElement("div");
        mediaContainer.className = "media-container";

        const footer = document.createElement("div");
        footer.className = "tile-footer";

        const nameEl = document.createElement("span");
        nameEl.className = "tile-name";
        nameEl.textContent = participantLabel(participant);

        const muteBadge = document.createElement("span");
        muteBadge.className = "tile-badge tile-badge-muted";
        muteBadge.title = "Micro coupé";
        muteBadge.innerHTML = MIC_MUTED_ICON;
        muteBadge.hidden = true;

        footer.appendChild(nameEl);
        footer.appendChild(muteBadge);

        let kickBtn = null;
        if (!participant.isLocal && !isScreenShare) {
            kickBtn = document.createElement("button");
            kickBtn.type = "button";
            kickBtn.className = "tile-kick-btn";
            kickBtn.title = "Expulser de la salle";
            kickBtn.setAttribute("aria-label", "Expulser de la salle");
            kickBtn.innerHTML = KICK_ICON;
            kickBtn.hidden = true;
            kickBtn.addEventListener("click", (event) => {
                event.preventDefault();
                event.stopPropagation();
                void kickParticipant(participant);
            });
            footer.appendChild(kickBtn);
        }

        let volumeSlider = null;
        let volumeBtn = null;
        if (!participant.isLocal) {
            const volumeWrap = document.createElement("div");
            volumeWrap.className = "tile-volume";

            volumeBtn = document.createElement("button");
            volumeBtn.type = "button";
            volumeBtn.className = "tile-volume-btn";
            volumeBtn.innerHTML = SPEAKER_ICON;
            volumeBtn.setAttribute(
                "aria-label",
                isScreenShare ? "Couper le son du partage" : "Couper le son du participant"
            );

            volumeSlider = document.createElement("input");
            volumeSlider.type = "range";
            volumeSlider.className = "tile-volume-slider";
            volumeSlider.min = "0";
            volumeSlider.max = "200";
            volumeSlider.step = "1";
            volumeSlider.value = String(volumePercent(getStoredVolume(volumeKey)));
            volumeSlider.setAttribute("aria-label", isScreenShare ? "Volume du partage" : "Volume du participant");

            volumeSlider.addEventListener("input", (event) => {
                event.stopPropagation();
                const nextVolume = volumeFromPercent(Number(event.target.value));
                if (nextVolume > 0) {
                    tile.volumeBeforeMute = nextVolume;
                }
                setStoredVolume(volumeKey, nextVolume);
            });

            volumeWrap.addEventListener("click", (event) => {
                event.stopPropagation();
            });

            volumeWrap.appendChild(volumeBtn);
            volumeWrap.appendChild(volumeSlider);
            footer.appendChild(volumeWrap);
        }

        element.appendChild(mediaContainer);
        element.appendChild(footer);

        const tile = {
            element,
            mediaContainer,
            participant,
            nameElement: nameEl,
            muteBadge,
            kickBtn,
            volumeBtn,
            volumeSlider,
            volumeKey,
            tileKey,
            volumeBeforeMute: getStoredVolume(volumeKey) || VOLUME_DEFAULT,
            isScreenShare,
            trackSid: track?.sid || null,
        };

        if (volumeBtn) {
            volumeBtn.addEventListener("click", (event) => {
                event.preventDefault();
                event.stopPropagation();
                toggleVolumeMute(tile);
            });
        }

        element.addEventListener("click", (event) => {
            if (event.target.closest(".tile-volume") || event.target.closest(".tile-kick-btn")) {
                return;
            }
            toggleTileFocus(tile.tileKey);
        });

        element.addEventListener("keydown", (event) => {
            if (event.key === "Enter" || event.key === " ") {
                event.preventDefault();
                toggleTileFocus(tile.tileKey);
            }
        });

        updateTileStatus(tile);
        return tile;
    }

    function createAvatarPlaceholder() {
        const avatar = document.createElement("div");
        avatar.className = "avatar-placeholder";
        avatar.innerHTML = PERSON_ICON;
        return avatar;
    }

    function formatParticipantName(participant) {
        if (participant?.name?.trim()) {
            return participant.name.trim();
        }

        if (participant?.isLocal && sessionDisplayName) {
            return sessionDisplayName;
        }

        const legacyName = parseLegacyIdentity(participant?.identity);
        if (legacyName) {
            return legacyName;
        }

        const identity = participant?.identity || "";
        if (/^\d+$/.test(identity)) {
            return "Participant";
        }

        return identity || "Participant";
    }

    function parseLegacyIdentity(identity) {
        if (!identity) {
            return null;
        }

        const legacyMatch = identity.match(/^(.+)_(\d+)$/);
        if (!legacyMatch) {
            return null;
        }

        return legacyMatch[1].replace(/_/g, " ");
    }

    function participantLabel(participant) {
        const name = formatParticipantName(participant);
        return participant.isLocal ? `${name} (vous)` : name;
    }

    function refreshParticipantCount() {
        if (!room || !participantCount) {
            return;
        }

        const total = room.remoteParticipants.size + 1;
        participantCount.textContent =
            total <= 1 ? "1 participant" : `${total} participants`;
    }

    function syncControlStates() {
        if (!room) {
            return;
        }

        const micEnabled = Boolean(room.localParticipant.isMicrophoneEnabled);
        const camEnabled = Boolean(room.localParticipant.isCameraEnabled);
        const screenEnabled = Boolean(room.localParticipant.isScreenShareEnabled);

        toggleMic.classList.toggle("off", !micEnabled);
        const micIconOn = toggleMic.querySelector(".icon-mic-on");
        const micIconOff = toggleMic.querySelector(".icon-mic-off");
        if (micIconOn) {
            if (micEnabled) {
                micIconOn.removeAttribute("hidden");
            } else {
                micIconOn.setAttribute("hidden", "");
            }
        }
        if (micIconOff) {
            if (micEnabled) {
                micIconOff.setAttribute("hidden", "");
            } else {
                micIconOff.removeAttribute("hidden");
            }
        }

        toggleCam.classList.toggle("off", !camEnabled);
        const camIconOn = toggleCam.querySelector(".icon-cam-on");
        const camIconOff = toggleCam.querySelector(".icon-cam-off");
        if (camIconOn) {
            if (camEnabled) {
                camIconOn.removeAttribute("hidden");
            } else {
                camIconOn.setAttribute("hidden", "");
            }
        }
        if (camIconOff) {
            if (camEnabled) {
                camIconOff.setAttribute("hidden", "");
            } else {
                camIconOff.removeAttribute("hidden");
            }
        }

        toggleScreen.classList.toggle("active", screenEnabled);
        const screenIconOn = toggleScreen.querySelector(".icon-screen-on");
        const screenIconOff = toggleScreen.querySelector(".icon-screen-off");
        if (screenIconOn) {
            if (screenEnabled) {
                screenIconOn.setAttribute("hidden", "");
            } else {
                screenIconOn.removeAttribute("hidden");
            }
        }
        if (screenIconOff) {
            if (screenEnabled) {
                screenIconOff.removeAttribute("hidden");
            } else {
                screenIconOff.setAttribute("hidden", "");
            }
        }
    }

    function showRoom() {
        loadingScreen.hidden = true;
        errorScreen.hidden = true;
        permissionScreen.hidden = true;
        roomScreen.hidden = false;
        updateVideoGridLayout();
    }

    function showError(message) {
        loadingScreen.hidden = true;
        permissionScreen.hidden = true;
        roomScreen.hidden = true;
        errorScreen.hidden = false;
        if (errorMessage) errorMessage.textContent = message;
    }

    function setupChatBindings(currentRoom) {
        // Chat panel starts hidden, can be toggled with button

        // Handle send message
        chatForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            const messageText = chatInput.value.trim();
            if (!messageText) {
                return;
            }

            try {
                // Send message
                const encoder = new TextEncoder();
                const data = encoder.encode(messageText);
                await currentRoom.localParticipant.publishData(data, {reliable: true});

                // Add to own chat display
                addChatMessage(sessionDisplayName || "Vous", messageText, true);

                // Clear input
                chatInput.value = "";
                chatInput.focus();
            } catch (error) {
                console.error("Erreur envoi message :", error);
            }
        });

        // Close chat button handled in bindStaticControls via setChatOpen(false)
    }

    function addChatMessage(author, text, isOwn) {
        const messageDiv = document.createElement("div");
        messageDiv.className = `chat-message ${isOwn ? "own" : ""}`;

        const authorSpan = document.createElement("div");
        authorSpan.className = "chat-message-author";
        authorSpan.textContent = author;

        const textSpan = document.createElement("div");
        textSpan.className = "chat-message-text";
        textSpan.textContent = text;

        messageDiv.appendChild(authorSpan);
        messageDiv.appendChild(textSpan);

        chatMessages.appendChild(messageDiv);

        // Auto-scroll to bottom
        chatMessages.scrollTop = chatMessages.scrollHeight;
    }
})();