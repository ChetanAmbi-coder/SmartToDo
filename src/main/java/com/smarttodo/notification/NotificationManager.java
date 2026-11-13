package com.smarttodo.notification;

import com.smarttodo.components.ToastNotification;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

public class NotificationManager {
    private static NotificationManager instance;
    private final VBox toastContainer;
    private int unreadCount = 0;
    private final List<Runnable> unreadListeners = new CopyOnWriteArrayList<>();

    public NotificationManager() {
        toastContainer = new VBox(8);
        toastContainer.setAlignment(Pos.TOP_RIGHT);
        toastContainer.setMouseTransparent(false);
        toastContainer.setPickOnBounds(false);
        toastContainer.setStyle("-fx-padding: 20;");
    }

    public static synchronized NotificationManager getInstance() {
        if (instance == null) {
            instance = new NotificationManager();
        }
        return instance;
    }

    public VBox getToastContainer() {
        return toastContainer;
    }

    public void addUnreadListener(Runnable listener) {
        unreadListeners.add(listener);
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void clearUnread() {
        unreadCount = 0;
        notifyListeners();
    }

    public void showNotification(String title, String message, ToastNotification.Type type) {
        Platform.runLater(() -> {
            unreadCount++;
            notifyListeners();

            ToastNotification[] toastHolder = new ToastNotification[1];
            ToastNotification toast = new ToastNotification(title, message, type, () -> {
                if (toastHolder[0] != null) {
                    toastContainer.getChildren().remove(toastHolder[0]);
                }
            });
            toastHolder[0] = toast;

            toastContainer.getChildren().add(0, toast);
        });
    }

    private void notifyListeners() {
        for (Runnable r : unreadListeners) {
            r.run();
        }
    }
}
