package com.parking.qa.steps;

import com.parking.qa.clients.AuthApiClient;
import com.parking.qa.context.ScenarioContext;
import com.parking.qa.db.DbClient;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class AuthSteps {
    private final AuthApiClient authApiClient = new AuthApiClient();
    private final DbClient dbClient = new DbClient();
    private final ScenarioContext context;

    public AuthSteps(ScenarioContext context) {
        this.context = context;
    }

    @When("I request the auth service status")
    public void iRequestTheAuthServiceStatus() {
        context.response(authApiClient.status());
    }

    @When("I register a unique auth user with no explicit roles")
    public void iRegisterAUniqueAuthUserWithNoExplicitRoles() {
        setUniqueUser("qa_user");
        context.roles(List.of("USER"));
        context.response(authApiClient.register(
                context.username(),
                context.email(),
                context.password(),
                null
        ));
    }

    @Given("I registered a unique auth user with roles:")
    public void iRegisteredAUniqueAuthUserWithRoles(DataTable dataTable) {
        List<String> roles = dataTable.asList();
        setUniqueUser("qa_user");
        context.roles(roles);
        context.response(authApiClient.register(
                context.username(),
                context.email(),
                context.password(),
                roles
        ));
        assertThat(context.response().statusCode()).isEqualTo(201);
    }

    @When("I login as the registered auth user")
    public void iLoginAsTheRegisteredAuthUser() {
        context.response(authApiClient.login(context.username(), context.password()));
    }

    @When("I register another auth user with the same username")
    public void iRegisterAnotherAuthUserWithTheSameUsername() {
        String newEmail = "qa_duplicate_email_" + uniqueSuffix() + "@parking.local";
        context.response(authApiClient.register(
                context.username(),
                newEmail,
                context.password(),
                context.roles()
        ));
    }

    @When("I register another auth user with the same email")
    public void iRegisterAnotherAuthUserWithTheSameEmail() {
        String newUsername = "qa_duplicate_user_" + uniqueSuffix();
        context.response(authApiClient.register(
                newUsername,
                context.email(),
                context.password(),
                context.roles()
        ));
    }

    @Then("the auth registration response should contain role {string}")
    public void theAuthRegistrationResponseShouldContainRole(String expectedRole) {
        assertThat(context.response().jsonPath().getList("roles", String.class)).contains(expectedRole);
    }

    @Then("the auth login response should contain a bearer token")
    public void theAuthLoginResponseShouldContainABearerToken() {
        assertThat(context.response().jsonPath().getString("tokenType")).isEqualTo("Bearer");
        assertThat(context.response().jsonPath().getString("accessToken")).isNotBlank();
        assertThat(context.response().jsonPath().getString("expiresAt")).isNotBlank();
        assertThat(context.response().jsonPath().getString("username")).isEqualTo(context.username());
    }

    @Then("the auth login response should contain role {string}")
    public void theAuthLoginResponseShouldContainRole(String expectedRole) {
        assertThat(context.response().jsonPath().getList("roles", String.class)).contains(expectedRole);
    }

    @Then("the registered auth user should exist in the database")
    public void theRegisteredAuthUserShouldExistInTheDatabase() {
        assertThat(dbClient.userExists(context.username())).isTrue();
    }

    @Then("the registered auth user should have role {string} in the database")
    public void theRegisteredAuthUserShouldHaveRoleInTheDatabase(String role) {
        assertThat(dbClient.userRoleExists(context.username(), role)).isTrue();
    }

    private void setUniqueUser(String prefix) {
        String suffix = uniqueSuffix();
        context.username(prefix + "_" + suffix);
        context.email(prefix + "_" + suffix + "@parking.local");
    }

    private String uniqueSuffix() {
        return String.valueOf(Instant.now().toEpochMilli());
    }
}
