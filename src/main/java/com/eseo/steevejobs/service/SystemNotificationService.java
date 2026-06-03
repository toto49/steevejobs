package com.eseo.steevejobs.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/**
 * Notifications système natives (bureau) selon l'OS hôte.
 * <p>
 * Effet de bord : lancement d'un processus externe (PowerShell Toast sur Windows,
 * {@code osascript} sur macOS, {@code notify-send} sur Linux) dans un fil dédié.
 * Aucune persistance ; échec silencieux côté appelant (journalisation stderr).
 * </p>
 */
public class SystemNotificationService {

    /**
     * Affiche une notification locale avec titre et corps.
     *
     * @param title   titre affiché
     * @param message corps du message
     */
    public static void send(String title, String message) {
        String os = System.getProperty("os.name").toLowerCase();

        new Thread(() -> {
            ProcessBuilder pb;
            try {
                if (os.contains("win")) {
                    String psCommand = String.format(
                            "Add-Type -AssemblyName System.Runtime.WindowsRuntime;" +
                                    "[Windows.UI.Notifications.ToastNotificationManager, Windows.UI.Notifications, ContentType=WindowsRuntime];" +
                                    "[Windows.UI.Notifications.ToastNotification, Windows.UI.Notifications, ContentType=WindowsRuntime];" +
                                    "[void][Windows.UI.Notifications.ToastTemplateType];" +
                                    "$template = [Windows.UI.Notifications.ToastNotificationManager]::GetTemplateContent([Windows.UI.Notifications.ToastTemplateType]::ToastText02);" +
                                    "$textNodes = $template.GetElementsByTagName('text');" +
                                    "$textNodes.Item(0).AppendChild($template.CreateTextNode('%s')) | Out-Null;" +
                                    "$textNodes.Item(1).AppendChild($template.CreateTextNode('%s')) | Out-Null;" +
                                    "$toast = [Windows.UI.Notifications.ToastNotification]::new($template);" +
                                    "[Windows.UI.Notifications.ToastNotificationManager]::CreateToastNotifier('SteeveJobs').Show($toast);",
                            title.replace("'", "''"), message.replace("'", "''")
                    );
                    pb = new ProcessBuilder("powershell", "-NoProfile", "-Command", psCommand);
                } else if (os.contains("mac")) {
                    String script = String.format("display notification \"%s\" with title \"%s\"",
                            message.replace("\"", "\\\""), title.replace("\"", "\\\""));
                    pb = new ProcessBuilder("osascript", "-e", script);

                } else if (os.contains("nux")) {
                    pb = new ProcessBuilder("notify-send", "-a", "SteeveJobs", title, message);
                } else {
                    return;
                }

                Process p = pb.start();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.err.println("Log système (" + os + ") : " + line);
                    }
                }

                if (!p.waitFor(5, TimeUnit.SECONDS)) {
                    p.destroyForcibly();
                }

            } catch (IOException | InterruptedException e) {
                System.err.println("Échec notification : " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        }, "NotificationThread").start();
    }
}
