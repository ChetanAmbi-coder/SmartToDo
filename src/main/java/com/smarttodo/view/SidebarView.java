package com.smarttodo.view;

import com.smarttodo.repository.TaskRepository;
import com.smarttodo.service.TaskService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class SidebarView extends VBox {
    public enum NavItem {
        DASHBOARD("Dashboard", "📊"),
        MY_DAY("My Day", "☀"),
        INBOX("Inbox", "📥"),
        ALL_TASKS("All Tasks", "📋"),
        IMPORTANT("Important", "⭐"),
        UPCOMING("Upcoming", "⏰"),
        CALENDAR("Calendar", "📅"),
        PROJECTS("Projects", "📁"),
        FOCUS_MODE("Focus Mode", "⏱"),
        ANALYTICS("Analytics", "📈"),
        SETTINGS("Settings", "⚙");

        private final String title;
        private final String icon;

        NavItem(String title, String icon) {
            this.title = title;
            this.icon = icon;
        }

        public String getTitle() {
            return title;
        }

        public String getIcon() {
            return icon;
        }
    }

    private final TaskRepository taskRepository;
    private final Consumer<NavItem> onNavigation;
    private NavItem activeItem = NavItem.DASHBOARD;

    private final Map<NavItem, Button> navButtons = new HashMap<>();
    private final Map<NavItem, Label> navBadges = new HashMap<>();

    public SidebarView(TaskRepository taskRepository, Consumer<NavItem> onNavigation) {
        this.taskRepository = taskRepository;
        this.onNavigation = onNavigation;

        getStyleClass().add("sidebar");
        setPrefWidth(220);
        setMinWidth(200);
        setMaxWidth(260);
        setSpacing(4);

        // App Branding / Logo
        HBox brandBox = new HBox(8);
        brandBox.setAlignment(Pos.CENTER_LEFT);
        brandBox.setPadding(new Insets(8, 8, 20, 8));

        Label logoIcon = new Label("✓");
        logoIcon.setStyle("-fx-background-color: -color-accent; -fx-text-fill: white; -fx-background-radius: 8px; -fx-padding: 4 8 4 8; -fx-font-weight: bold; -fx-font-size: 16px;");

        VBox brandText = new VBox(0);
        Label titleLbl = new Label("SmartToDo");
        titleLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: -color-text-primary;");
        Label versionLbl = new Label("DESKTOP v1.0");
        versionLbl.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: -color-text-muted; -fx-letter-spacing: 1px;");
        brandText.getChildren().addAll(titleLbl, versionLbl);

        brandBox.getChildren().addAll(logoIcon, brandText);
        getChildren().add(brandBox);

        // Navigation Sections
        addSectionHeader("PRODUCTIVITY");
        createNavButton(NavItem.DASHBOARD);
        createNavButton(NavItem.MY_DAY);
        createNavButton(NavItem.FOCUS_MODE);
        createNavButton(NavItem.ANALYTICS);

        addSectionHeader("TASK REGISTRY");
        createNavButton(NavItem.INBOX);
        createNavButton(NavItem.ALL_TASKS);
        createNavButton(NavItem.IMPORTANT);
        createNavButton(NavItem.UPCOMING);
        createNavButton(NavItem.CALENDAR);

        addSectionHeader("WORKSPACE");
        createNavButton(NavItem.PROJECTS);
        createNavButton(NavItem.SETTINGS);

        // Spacer
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        getChildren().add(spacer);

        setActiveItem(NavItem.DASHBOARD);
        refreshBadges();
    }

    private void addSectionHeader(String title) {
        Label header = new Label(title);
        header.getStyleClass().add("sidebar-heading");
        getChildren().add(header);
    }

    private void createNavButton(NavItem item) {
        Button btn = new Button();
        btn.getStyleClass().add("nav-button");
        btn.setMaxWidth(Double.MAX_VALUE);

        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        Label iconLbl = new Label(item.getIcon());
        iconLbl.setStyle("-fx-font-size: 14px; -fx-min-width: 18px;");

        Label textLbl = new Label(item.getTitle());
        textLbl.setStyle("-fx-font-weight: 500;");
        HBox.setHgrow(textLbl, Priority.ALWAYS);

        Label badge = new Label();
        badge.getStyleClass().add("nav-badge");
        badge.setVisible(false);
        navBadges.put(item, badge);

        row.getChildren().addAll(iconLbl, textLbl, badge);
        btn.setGraphic(row);

        btn.setOnAction(e -> {
            setActiveItem(item);
            if (onNavigation != null) {
                onNavigation.accept(item);
            }
        });

        navButtons.put(item, btn);
        getChildren().add(btn);
    }

    public void setActiveItem(NavItem item) {
        this.activeItem = item;
        navButtons.forEach((navItem, btn) -> {
            btn.getStyleClass().remove("nav-button-active");
            Label badge = navBadges.get(navItem);
            if (badge != null) {
                badge.getStyleClass().remove("nav-badge-active");
            }
            if (navItem == item) {
                btn.getStyleClass().add("nav-button-active");
                if (badge != null) {
                    badge.getStyleClass().add("nav-badge-active");
                }
            }
        });
    }

    public void refreshBadges() {
        try {
            LocalDate today = LocalDate.now();

            int inboxCount = taskRepository.findInboxTasks().size();
            updateBadge(NavItem.INBOX, inboxCount);

            int todayCount = taskRepository.findTodayTasks(today).size();
            updateBadge(NavItem.MY_DAY, todayCount);

            int impCount = taskRepository.findImportant().size();
            updateBadge(NavItem.IMPORTANT, impCount);

            int upcomingCount = taskRepository.findUpcomingTasks(today, today.plusDays(7)).size();
            updateBadge(NavItem.UPCOMING, upcomingCount);

            int activeCount = taskRepository.countActive();
            updateBadge(NavItem.ALL_TASKS, activeCount);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateBadge(NavItem item, int count) {
        Label badge = navBadges.get(item);
        if (badge != null) {
            if (count > 0) {
                badge.setText(String.valueOf(count));
                badge.setVisible(true);
            } else {
                badge.setVisible(false);
            }
        }
    }
}
