package com.smarttodo.components;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class StatCard extends VBox {
    private final Label valueLabel;
    private final Label titleLabel;
    private final Label subLabel;

    public StatCard(String title, String initialValue, String subtitle, String accentColor) {
        getStyleClass().add("stat-card");
        setSpacing(6);
        setAlignment(Pos.CENTER_LEFT);
        setPrefWidth(180);
        setMinWidth(150);

        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label iconBar = new Label();
        iconBar.setPrefSize(4, 16);
        iconBar.setStyle("-fx-background-color: " + (accentColor != null ? accentColor : "#3B82F6") + "; -fx-background-radius: 2px;");

        titleLabel = new Label(title);
        titleLabel.getStyleClass().add("stat-label");
        topRow.getChildren().addAll(iconBar, titleLabel);

        valueLabel = new Label(initialValue);
        valueLabel.getStyleClass().add("stat-value");

        subLabel = new Label(subtitle);
        subLabel.setStyle("-fx-text-fill: -color-text-muted; -fx-font-size: 11px;");

        getChildren().addAll(topRow, valueLabel, subLabel);
    }

    public void setValue(String value) {
        valueLabel.setText(value);
    }

    public void setSubtitle(String text) {
        subLabel.setText(text);
    }
}
