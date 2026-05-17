package com.parking.qa.clients;

import com.parking.qa.config.TestConfig;
import io.restassured.response.Response;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;

public class AuthApiClient extends ApiClient {
    public Response status() {
        return given()
                .spec(requestSpec(TestConfig.authBaseUrl()))
                .when()
                .get("/status");
    }

    public Response register(String username, String email, String password, List<String> roles) {
        Map<String, Object> payload = roles == null
                ? Map.of(
                        "username", username,
                        "email", email,
                        "password", password
                )
                : Map.of(
                        "username", username,
                        "email", email,
                        "password", password,
                        "roles", roles
                );

        return given()
                .spec(requestSpec(TestConfig.authBaseUrl()))
                .body(payload)
                .when()
                .post("/auth/register");
    }

    public Response login(String username, String password) {
        return given()
                .spec(requestSpec(TestConfig.authBaseUrl()))
                .body(Map.of(
                        "username", username,
                        "password", password
                ))
                .when()
                .post("/auth/login");
    }
}
