package com.smarttodo.components;

import com.smarttodo.model.FocusSession;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;

public class CircularTimerView extends StackPane {
    private final Canvas canvas;
    private final Label timeLabel;
    private final Label statusLabel;
    private double progress = 0.0;
    private String colorHex = "#3B82F6";

    public CircularTimerView(double size) {
        setPrefSize(size, size);
        setMaxSize(size, size);
        setMinSize(size, size);

        canvas = new Canvas(size, size);

        VBox centerBox = new VBox(6);
        centerBox.setAlignment(Pos.CENTER);

        timeLabel = new Label("25:00");
        timeLabel.setStyle("-fx-font-size: 38px; -fx-font-weight: bold; -fx-text-fill: -color-text-primary;");

        statusLabel = new Label("FOCUS TIME");
        statusLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: -color-text-muted; -fx-letter-spacing: 2px;");

        centerBox.getChildren().addAll(timeLabel, statusLabel);

        getChildren().addAll(canvas, centerBox);
        draw();
    }

    public void update(int remainingSeconds, double progress, FocusSession.SessionType type) {
        this.progress = Math.max(0.0, Math.min(1.0, progress));
        this.colorHex = type != null ? type.getColorHex() : "#3B82F6";

        int mins = remainingSeconds / 60;
        int secs = remainingSeconds % 60;
        timeLabel.setText(String.format("%02d:%02d", mins, secs));

        String labelText = type != null ? type.getDisplayName().toUpperCase() : "FOCUS TIME";
        statusLabel.setText(labelText);

        draw();
    }

    private void draw() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        gc.clearRect(0, 0, w, h);

        double cx = w / 2.0;
        double cy = h / 2.0;
        double strokeWidth = 14.0;
        double radius = (w - strokeWidth - 10) / 2.0;

        // Background track circle
        gc.setLineWidth(strokeWidth);
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setStroke(Color.web("#334155", 0.35));
        gc.strokeOval(cx - radius, cy - radius, radius * 2, radius * 2);

        // Progress arc
        gc.setStroke(Color.web(colorHex));
        double angle = progress * 360.0;
        gc.strokeArc(cx - radius, cy - radius, radius * 2, radius * 2, 90, -angle, javafx.scene.shape.ArcType.OPEN);
    }
}
