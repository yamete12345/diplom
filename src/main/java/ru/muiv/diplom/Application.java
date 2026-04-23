package ru.muiv.diplom;

import com.formdev.flatlaf.FlatLightLaf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import ru.muiv.diplom.ui.LoginFrame;

import javax.swing.*;

@SpringBootApplication
public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "false");

        ConfigurableApplicationContext ctx = new SpringApplication(Application.class).run(args);

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(new FlatLightLaf());
            } catch (Exception e) {
                log.warn("Не удалось установить FlatLaf", e);
            }
            LoginFrame login = ctx.getBean(LoginFrame.class);
            login.setVisible(true);
        });
    }
}
