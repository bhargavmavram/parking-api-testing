package com.parking.qa.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TestConfig {
    private static final String DEFAULT_ENVIRONMENT = "local";
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}:]+)(?::([^}]*))?}");
    private static final Map<String, String> DOTENV = loadDotEnv();
    private static final Properties PROPERTIES = loadProperties();

    private TestConfig() {
    }

    public static String authBaseUrl() {
        return get("auth.baseUrl");
    }

    public static String environment() {
        String systemValue = System.getProperty("test.env");
        if (systemValue != null && !systemValue.isBlank()) {
            return systemValue;
        }

        String envValue = System.getenv("TEST_ENV");
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }

        return DEFAULT_ENVIRONMENT;
    }

    public static String dbUrl() {
        return get("db.url");
    }

    public static String dbUsername() {
        return get("db.username");
    }

    public static String dbPassword() {
        return get("db.password");
    }

    public static boolean dbTunnelEnabled() {
        return Boolean.parseBoolean(getOptional("db.tunnel.enabled", "false"));
    }

    public static String dbTunnelEc2Host() {
        return get("db.tunnel.ec2Host");
    }

    public static String dbTunnelEc2User() {
        return get("db.tunnel.ec2User");
    }

    public static String dbTunnelKeyPath() {
        return get("db.tunnel.keyPath");
    }

    public static String dbTunnelRdsHost() {
        return get("db.tunnel.rdsHost");
    }

    public static int dbTunnelRdsPort() {
        return Integer.parseInt(getOptional("db.tunnel.rdsPort", "5432"));
    }

    public static String dbTunnelLocalHost() {
        return getOptional("db.tunnel.localHost", "127.0.0.1");
    }

    public static int dbTunnelLocalPort() {
        return Integer.parseInt(getOptional("db.tunnel.localPort", "15432"));
    }

    private static String get(String key) {
        String systemValue = System.getProperty(key);
        if (systemValue != null && !systemValue.isBlank()) {
            return systemValue;
        }

        String envValue = System.getenv(toEnvironmentKey(key));
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }

        String dotEnvValue = DOTENV.get(toEnvironmentKey(key));
        if (dotEnvValue != null && !dotEnvValue.isBlank()) {
            return dotEnvValue;
        }

        String fileValue = propertyValue(key);
        if (fileValue == null || fileValue.isBlank()) {
            throw new IllegalStateException("Missing test configuration value: " + key);
        }
        return resolvePlaceholders(fileValue);
    }

    private static String getOptional(String key, String defaultValue) {
        String systemValue = System.getProperty(key);
        if (systemValue != null && !systemValue.isBlank()) {
            return systemValue;
        }

        String envValue = System.getenv(toEnvironmentKey(key));
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }

        String dotEnvValue = DOTENV.get(toEnvironmentKey(key));
        if (dotEnvValue != null && !dotEnvValue.isBlank()) {
            return dotEnvValue;
        }

        String fileValue = propertyValue(key);
        if (fileValue != null && !fileValue.isBlank()) {
            return resolvePlaceholders(fileValue);
        }

        return defaultValue;
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        String environment = environment();
        String configPath = "config/env/" + environment + ".properties";
        try (InputStream inputStream = TestConfig.class.getClassLoader()
                .getResourceAsStream(configPath)) {
            if (inputStream == null) {
                throw new IllegalStateException("Could not find " + configPath);
            }
            properties.load(inputStream);
            return properties;
        } catch (IOException ex) {
            throw new IllegalStateException("Could not load test configuration", ex);
        }
    }

    private static Map<String, String> loadDotEnv() {
        Map<String, String> values = new LinkedHashMap<>();
        loadDotEnvFile(values, Path.of(".env"));
        loadDotEnvFile(values, Path.of(".env." + environment()));
        return values;
    }

    private static void loadDotEnvFile(Map<String, String> values, Path path) {
        if (!Files.exists(path)) {
            return;
        }

        try {
            for (String rawLine : Files.readAllLines(path)) {
                String line = rawLine.trim();
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }

                int separatorIndex = line.indexOf('=');
                if (separatorIndex < 1) {
                    continue;
                }

                String key = line.substring(0, separatorIndex).trim();
                String value = stripOptionalQuotes(line.substring(separatorIndex + 1).trim());
                values.put(key, value);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Could not load .env file: " + path, ex);
        }
    }

    private static String resolvePlaceholders(String value) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(value);
        StringBuilder resolved = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            String defaultValue = matcher.group(2);
            String replacement = System.getenv(key);
            if (replacement == null || replacement.isBlank()) {
                replacement = DOTENV.get(key);
            }
            if ((replacement == null || replacement.isBlank()) && defaultValue != null) {
                replacement = defaultValue;
            }
            if (replacement == null) {
                throw new IllegalStateException("Missing environment value for placeholder: " + key);
            }
            matcher.appendReplacement(resolved, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(resolved);
        return resolved.toString();
    }

    private static String stripOptionalQuotes(String value) {
        if (value.length() >= 2
                && ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static String propertyValue(String key) {
        String envStyleKey = toEnvironmentKey(key);
        String value = PROPERTIES.getProperty(envStyleKey);
        if (value != null && !value.isBlank()) {
            return value;
        }
        return PROPERTIES.getProperty(key);
    }

    private static String toEnvironmentKey(String key) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < key.length(); index++) {
            char current = key.charAt(index);
            if (current == '.') {
                builder.append('_');
                continue;
            }
            if (Character.isUpperCase(current) && index > 0 && key.charAt(index - 1) != '.') {
                builder.append('_');
            }
            builder.append(Character.toUpperCase(current));
        }
        return builder.toString();
    }
}
