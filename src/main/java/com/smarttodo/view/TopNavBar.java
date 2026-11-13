package com.smarttodo.view;

import com.smarttodo.config.ThemeManager;
import com.smarttodo.notification.NotificationManager;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class TopNavBar extends HBox {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEE, MMM dd, yyyy");

    private final TextField globalSearchField = new TextField();
    private final Button quickAddBtn = new Button("+ New Task");
    private final Button notificationBtn = new Button("🔔");
    private final Label notifBadge = new Label("0");
    private final Button themeToggleBtn = new Button("🌓");
    private final Consumer<String> onGlobalSearch;
    private final Runnable onQuickAdd;

    public TopNavBar(ThemeManager themeManager,
                     NotificationManager notificationManager,
                     Consumer<String> onGlobalSearch,
                     Runnable onQuickAdd) {
        this.onGlobalSearch = onGlobalSearch;
        this.onQuickAdd = onQuickAdd;

        getStyleClass().add("top-nav");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(16);

        // Global Search Input
        globalSearchField.setPromptText("Global search tasks (press Enter)...");
        globalSearchField.getStyleClass().add("search-field");
        globalSearchField.setPrefWidth(280);
        globalSearchField.setOnAction(e -> {
            if (onGlobalSearch != null) {
                onGlobalSearch.accept(globalSearchField.getText());
            }
        });

        // Current Date
        Label dateLabel = new Label("📅 " + LocalDate.now().format(DATE_FMT));
        dateLabel.setStyle("-fx-text-fill: -color-text-muted; -fx-font-weight: 500; -fx-font-size: 12px;");

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Quick Add Button
        quickAddBtn.getStyleClass().add("btn-primary");
        quickAddBtn.setOnAction(e -> {
            if (onQuickAdd != null) onQuickAdd.run();
        });

        // Notification Bell with Badge
        StackPane notifPane = new StackPane();
        notificationBtn.getStyleClass().add("btn-icon");
        notificationBtn.setStyle("-fx-font-size: 15px;");
        notificationBtn.setOnAction(e -> {
            notificationManager.clearUnread();
            updateNotificationBadge(0);
        });

        notifBadge.setStyle("-fx-background-color: -color-danger; -fx-text-fill: white; -fx-background-radius: 8px; -fx-padding: 1 5 1 5; -fx-font-size: 9px; -fx-font-weight: bold;");
        StackPane.setAlignment(notifBadge, Pos.TOP_RIGHT);
        notifBadge.setVisible(false);

        notifPane.getChildren().addAll(notificationBtn, notifBadge);

        notificationManager.addUnreadListener(() -> {
            updateNotificationBadge(notificationManager.getUnreadCount());
        });

        // Theme Toggle Button
        themeToggleBtn.getStyleClass().add("btn-icon");
        themeToggleBtn.setStyle("-fx-font-size: 15px;");
        themeToggleBtn.setTooltip(new javafx.scene.control.Tooltip("Toggle Dark/Light Theme"));
        themeToggleBtn.setOnAction(e -> themeManager.toggleTheme());

        getChildren().addAll(globalSearchField, dateLabel, spacer, quickAddBtn, notifPane, themeToggleBtn);
    }

    private void updateNotificationBadge(int count) {
        javafx.application.Platform.runLater(() -> {
            if (count > 0) {
                notifBadge.setText(String.valueOf(count));
                notifBadge.setVisible(true);
            } else {
                notifBadge.setVisible(false);
            }
        });
    }

    public void setSearchText(String text) {
        globalSearchField.setText(text);
    }
}
