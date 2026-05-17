package com.parking.qa.context;

import io.restassured.response.Response;
import java.util.ArrayList;
import java.util.List;

public class ScenarioContext {
    private Response response;
    private String username;
    private String email;
    private String password;
    private List<String> roles = new ArrayList<>();

    public Response response() {
        return response;
    }

    public void response(Response response) {
        this.response = response;
    }

    public String username() {
        return username;
    }

    public void username(String username) {
        this.username = username;
    }

    public String email() {
        return email;
    }

    public void email(String email) {
        this.email = email;
    }

    public String password() {
        return password;
    }

    public void password(String password) {
        this.password = password;
    }

    public List<String> roles() {
        return roles;
    }

    public void roles(List<String> roles) {
        this.roles = new ArrayList<>(roles);
    }
}
