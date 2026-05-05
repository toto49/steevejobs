package com.eseo.steevejobs.service;

public class SystemNotificationService {

    public static void send(String title, String message) {
        String os = System.getProperty("os.name").toLowerCase();

        try {
            if (os.contains("win")) {
                Runtime.getRuntime().exec(
                        "powershell -command \"[Windows.UI.Notifications.ToastNotificationManager, Windows.UI.Notifications, ContentType = WindowsRuntime];" +
                                "$template = [Windows.UI.Notifications.ToastNotificationManager]::GetTemplateContent([Windows.UI.Notifications.ToastTemplateType]::ToastText02);" +
                                "$textNodes = $template.GetElementsByTagName('text');" +
                                "$textNodes.Item(0).AppendChild($template.CreateTextNode('" + title + "')) | Out-Null;" +
                                "$textNodes.Item(1).AppendChild($template.CreateTextNode('" + message + "')) | Out-Null;" +
                                "$toast = [Windows.UI.Notifications.ToastNotification]::new($template);" +
                                "[Windows.UI.Notifications.ToastNotificationManager]::CreateToastNotifier('SteeveJobs').Show($toast);\""
                );

            } else if (os.contains("mac")) {
                Runtime.getRuntime().exec(
                        new String[]{"osascript", "-e",
                                "display notification \"" + message + "\" with title \"" + title + "\""}
                );
            } else if (os.contains("nux")) {
                Runtime.getRuntime().exec(
                        new String[]{"notify-send", title, message}
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

