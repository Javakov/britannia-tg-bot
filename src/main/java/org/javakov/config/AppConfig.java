package org.javakov.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {
    private static final String PROFILE = System.getProperty("profile", "dev");
    private final Properties properties;

    public AppConfig() {
        properties = new Properties();
        System.out.println("Текущий профиль: " + PROFILE);
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(PROFILE + ".properties")) {
            if (input == null) {
                throw new IllegalStateException("Не найден файл настроек для профиля: " + PROFILE);
            }
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка загрузки настроек", e);
        }
    }

    public String getBotUsername() {
        return properties.getProperty("BOT_USERNAME");
    }

    public String getBotToken() {
        return properties.getProperty("BOT_TOKEN");
    }

    public String getBotHandle() {
        return properties.getProperty("BOT_HANDLE");
    }
}

