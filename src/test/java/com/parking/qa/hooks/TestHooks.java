package com.parking.qa.hooks;

import com.parking.qa.config.TestConfig;
import com.parking.qa.context.ScenarioContext;
import com.parking.qa.tunnel.SshTunnelManager;
import io.cucumber.java.AfterAll;
import io.cucumber.java.Before;
import io.cucumber.java.BeforeAll;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class TestHooks {
    private final ScenarioContext context;

    public TestHooks(ScenarioContext context) {
        this.context = context;
    }

    @BeforeAll
    public static void beforeAll() {
        SshTunnelManager.startIfEnabled();
        writeAllureEnvironment();
        logTestEnvironment();
    }

    @AfterAll
    public static void afterAll() {
        SshTunnelManager.stopIfStartedByTests();
    }

    private static void logTestEnvironment() {
        System.out.println();
        System.out.println("==================================================");
        System.out.println("Parking API Test Environment");
        System.out.println("Environment : " + TestConfig.environment());
        System.out.println("Auth URL    : " + TestConfig.authBaseUrl());
        System.out.println("DB URL      : " + TestConfig.dbUrl());
        System.out.println("DB Username : " + TestConfig.dbUsername());
        System.out.println("DB Password : ********");
        System.out.println("==================================================");
        System.out.println();
    }

    private static void writeAllureEnvironment() {
        Path allureResults = Path.of("target", "allure-results");
        try {
            Files.createDirectories(allureResults);
            Files.write(allureResults.resolve("environment.properties"), List.of(
                    "Test Environment=" + TestConfig.environment(),
                    "Auth Base URL=" + TestConfig.authBaseUrl(),
                    "Database URL=" + TestConfig.dbUrl(),
                    "Database Username=" + TestConfig.dbUsername(),
                    "Database Tunnel Enabled=" + TestConfig.dbTunnelEnabled()
            ));
        } catch (IOException ex) {
            throw new IllegalStateException("Could not write Allure environment metadata", ex);
        }
    }

    @Before
    public void resetScenarioData() {
        context.response(null);
        context.username(null);
        context.email(null);
        context.password("Password@123");
        context.roles(java.util.List.of());
    }
}
