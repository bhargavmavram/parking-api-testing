package com.parking.qa.steps;

import com.parking.qa.context.ScenarioContext;
import io.cucumber.java.en.Then;

import static org.assertj.core.api.Assertions.assertThat;

public class CommonResponseSteps {
    private final ScenarioContext context;

    public CommonResponseSteps(ScenarioContext context) {
        this.context = context;
    }

    @Then("the response status code should be {int}")
    public void theResponseStatusCodeShouldBe(int expectedStatusCode) {
        assertThat(context.response().statusCode()).isEqualTo(expectedStatusCode);
    }

    @Then("the response field {string} should be {string}")
    public void theResponseFieldShouldBe(String fieldName, String expectedValue) {
        assertThat(context.response().jsonPath().getString(fieldName)).isEqualTo(expectedValue);
    }
}
