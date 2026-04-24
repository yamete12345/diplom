package ru.muiv.diplom;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import ru.muiv.diplom.ui.BankTheme;
import ru.muiv.diplom.ui.LoginFrame;

import javax.swing.*;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "false");
        // Тема применяется до инициализации Spring-бинов Swing-окон —
        // иначе фабрики компонентов FlatLaf подхватывают дефолты.
        SwingUtilities.invokeLater(BankTheme::install);

        ConfigurableApplicationContext ctx = new SpringApplication(Application.class).run(args);

        SwingUtilities.invokeLater(() -> {
            LoginFrame login = ctx.getBean(LoginFrame.class);
            login.setVisible(true);
        });
    }
}
