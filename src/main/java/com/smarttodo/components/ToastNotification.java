package com.smarttodo.components;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class ToastNotification extends HBox {
    public enum Type {
        INFO("#3B82F6", "ℹ"),
        SUCCESS("#10B981", "✓"),
        WARNING("#F59E0B", "⚠"),
        ERROR("#EF4444", "✕");

        private final String color;
        private final String icon;

        Type(String color, String icon) {
            this.color = color;
            this.icon = icon;
        }

        public String getColor() {
            return color;
        }

        public String getIcon() {
            return icon;
        }
    }

    public ToastNotification(String title, String message, Type type, Runnable onClose) {
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(12);
        setMaxWidth(380);
        setPrefWidth(350);

        setStyle("-fx-background-color: -color-bg-card; " +
                "-fx-background-radius: 10px; " +
                "-fx-border-color: " + type.getColor() + "; " +
                "-fx-border-radius: 10px; " +
                "-fx-border-width: 1.5px; " +
                "-fx-padding: 12 16 12 16; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 15, 0, 0, 4);");

        Label iconLabel = new Label(type.getIcon());
        iconLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + type.getColor() + ";");

        VBox textContainer = new VBox(2);
        HBox.setHgrow(textContainer, Priority.ALWAYS);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: -color-text-primary;");

        Label msgLabel = new Label(message);
        msgLabel.setWrapText(true);
        msgLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -color-text-secondary;");

        textContainer.getChildren().addAll(titleLabel, msgLabel);

        Button closeBtn = new Button("✕");
        closeBtn.getStyleClass().add("btn-icon");
        closeBtn.setStyle("-fx-font-size: 10px; -fx-padding: 4 6 4 6;");
        closeBtn.setOnAction(e -> {
            if (onClose != null) onClose.run();
        });

        getChildren().addAll(iconLabel, textContainer, closeBtn);

        // Auto fade out
        FadeTransition fadeIn = new FadeTransition(Duration.millis(250), this);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        PauseTransition pause = new PauseTransition(Duration.seconds(5));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(350), this);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            if (onClose != null) onClose.run();
        });

        SequentialTransition seq = new SequentialTransition(fadeIn, pause, fadeOut);
        seq.play();
    }
}
