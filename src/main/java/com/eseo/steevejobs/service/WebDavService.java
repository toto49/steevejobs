package com.eseo.steevejobs.service;

import io.github.cdimascio.dotenv.Dotenv;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WebDavService {

    private static final Logger LOGGER = Logger.getLogger(WebDavService.class.getName());
    private static Dotenv dotenv;

    static {
        System.setProperty("jdk.internal.httpclient.disableHostnameVerification", "true");
    }

    private static Dotenv getDotenv() {
        if (dotenv == null) {
            try {
                dotenv = Dotenv.load();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Impossible de charger le fichier .env pour le WebDAV !", e);
                throw new RuntimeException("Fichier de configuration .env introuvable.");
            }
        }
        return dotenv;
    }

    /**
     * Envoie un fichier sur le NAS Synology dans le dossier spécifié.
     * * @param nomDossier Le nom du dossier (ex: "jean_dupont")
     *
     * @param nomFichier     Le nom du fichier avec son extension (ex: "devis_123.pdf")
     * @param contenuFichier Les données brutes du fichier (byte[])
     */
    public static void envoyerFichierSurNAS(String nomDossier, String nomFichier, byte[] contenuFichier) {
        String baseUrl = getDotenv().get("WEBDAV_BASE_URL");
        String username = getDotenv().get("WEBDAV_USERNAME");
        String password = getDotenv().get("WEBDAV_PASSWORD");

        if (baseUrl == null || username == null || password == null) {
            LOGGER.severe("Configuration WebDAV manquante dans le fichier .env !");
            throw new IllegalStateException("Configuration NAS introuvable. Veuillez vérifier le fichier .env.");
        }

        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }

        String urlDossier = baseUrl + nomDossier + "/";
        String urlFichier = urlDossier + nomFichier;

        try {
            HttpClient client = HttpClient.newBuilder()
                    .sslContext(creerSSLContextInsecable())
                    .build();

            String auth = username + ":" + password;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            String authHeader = "Basic " + encodedAuth;

            LOGGER.info("Vérification/Création du dossier NAS : " + urlDossier);
            HttpRequest requestDossier = HttpRequest.newBuilder()
                    .uri(URI.create(urlDossier))
                    .header("Authorization", authHeader)
                    .method("MKCOL", HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> responseDossier = client.send(requestDossier, HttpResponse.BodyHandlers.ofString());

            if (responseDossier.statusCode() == 201) {
                LOGGER.info("Dossier NAS créé avec succès.");
            } else if (responseDossier.statusCode() == 405) {
                LOGGER.fine("Le dossier NAS existe déjà (Action ignorée).");
            } else {
                LOGGER.warning("Réponse inattendue lors de la création du dossier (Code " + responseDossier.statusCode() + ")");
            }
            LOGGER.info("Envoi du fichier vers : " + urlFichier);
            HttpRequest requestFichier = HttpRequest.newBuilder()
                    .uri(URI.create(urlFichier))
                    .header("Authorization", authHeader)
                    .PUT(HttpRequest.BodyPublishers.ofByteArray(contenuFichier))
                    .build();

            HttpResponse<String> responseFichier = client.send(requestFichier, HttpResponse.BodyHandlers.ofString());

            int statusFichier = responseFichier.statusCode();
            if (statusFichier == 200 || statusFichier == 201 || statusFichier == 204) {
                LOGGER.info("Fichier stocké sur le NAS avec succès !");
            } else {
                LOGGER.severe("Erreur NAS (Code " + statusFichier + ") : " + responseFichier.body());
                throw new RuntimeException("Le serveur NAS a refusé le fichier (Code " + statusFichier + ").");
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Échec critique du transfert WebDAV", e);
            throw new RuntimeException("Erreur de connexion au NAS : " + e.getMessage(), e);
        }
    }

    public static void supprimerFichierDuNAS(String nomDossier, String nomFichier) {
        String baseUrl = getDotenv().get("WEBDAV_BASE_URL");
        String username = getDotenv().get("WEBDAV_USERNAME");
        String password = getDotenv().get("WEBDAV_PASSWORD");

        if (baseUrl == null || username == null || password == null) return;
        if (!baseUrl.endsWith("/")) baseUrl += "/";

        String urlFichier = baseUrl + nomDossier + "/" + nomFichier;

        try {
            HttpClient client = HttpClient.newBuilder()
                    .sslContext(creerSSLContextInsecable())
                    .build();

            String auth = username + ":" + password;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            String authHeader = "Basic " + encodedAuth;

            LOGGER.info("Demande de suppression du fichier NAS : " + urlFichier);
            HttpRequest requestDelete = HttpRequest.newBuilder()
                    .uri(URI.create(urlFichier))
                    .header("Authorization", authHeader)
                    .DELETE()
                    .build();

            HttpResponse<String> response = client.send(requestDelete, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 204) {
                LOGGER.info("✅ Fichier supprimé du NAS avec succès.");
            } else if (response.statusCode() == 404) {
                LOGGER.fine("ℹ️ Fichier déjà absent du NAS (Rien à supprimer).");
            } else {
                LOGGER.warning("⚠️ Échec de la suppression NAS (Code " + response.statusCode() + ").");
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Erreur critique lors de la suppression NAS", e);
        }
    }

    private static SSLContext creerSSLContextInsecable() throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
        };
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCerts, new SecureRandom());
        return sslContext;
    }

    public static void telechargerFichierDuNAS(String nomDossier, String nomFichier, String cheminLocal) throws Exception {
        String baseUrl = getDotenv().get("WEBDAV_BASE_URL");
        String username = getDotenv().get("WEBDAV_USERNAME");
        String password = getDotenv().get("WEBDAV_PASSWORD");

        if (baseUrl == null || username == null || password == null) {
            throw new IllegalStateException("Configuration NAS introuvable.");
        }

        if (!baseUrl.endsWith("/")) baseUrl += "/";
        String urlFichier = baseUrl + nomDossier + "/" + nomFichier;

        HttpClient client = HttpClient.newBuilder().sslContext(creerSSLContextInsecable()).build();
        String auth = username + ":" + password;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlFichier))
                .header("Authorization", "Basic " + encodedAuth)
                .GET()
                .build();

        HttpResponse<java.nio.file.Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(java.nio.file.Paths.get(cheminLocal)));

        if (response.statusCode() != 200) {
            java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(cheminLocal)); // Nettoie si échec
            throw new RuntimeException("Le NAS a refusé le téléchargement (Code " + response.statusCode() + ")");
        }
    }
}