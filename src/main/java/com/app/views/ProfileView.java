package com.app.views;

import com.app.session.UserSession;
import java.util.stream.Collectors;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** Displays the identity and roles of the signed-in user. */
public class ProfileView extends VBox {

    public ProfileView() {
        super(12);
        setPadding(new Insets(24));
        setStyle("-fx-background-color: #F8FAFC;");

        UserSession session = UserSession.getInstance();
        Label title = new Label("Mi perfil");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        getChildren().addAll(
                title,
                field("Nombre", session.getDisplayName()),
                field("Usuario", session.getUsername()),
                field("Roles", session.getGroups().stream().sorted().collect(Collectors.joining(", "))));
    }

    private static VBox field(String name, String value) {
        Label label = new Label(name);
        label.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");
        Label data = new Label(value == null || value.isBlank() ? "No disponible" : value);
        data.setStyle("-fx-font-size: 15px; -fx-text-fill: #0F172A;");
        return new VBox(4, label, data);
    }
}
