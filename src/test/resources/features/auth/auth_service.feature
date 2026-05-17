@auth
Feature: Parking Auth Service
  The auth service registers users and issues JWT tokens for the parking platform.

  @smoke
  Scenario: Auth service status endpoint is public
    When I request the auth service status
    Then the response status code should be 200
    And the response field "service" should be "parking-auth-service"
    And the response field "message" should be "Parking Auth Service is running"

  @smoke @db
  Scenario: Register a new user with default USER role
    When I register a unique auth user with no explicit roles
    Then the response status code should be 201
    And the auth registration response should contain role "USER"
    And the registered auth user should exist in the database
    And the registered auth user should have role "USER" in the database

  @smoke
  Scenario: Register an admin user and login successfully
    Given I registered a unique auth user with roles:
      | ADMIN |
      | USER  |
    When I login as the registered auth user
    Then the response status code should be 200
    And the auth login response should contain a bearer token
    And the auth login response should contain role "ADMIN"
    And the auth login response should contain role "USER"

  @regression
  Scenario: Duplicate username is rejected
    Given I registered a unique auth user with roles:
      | USER |
    When I register another auth user with the same username
    Then the response status code should be 400
    And the response field "error" should be "Username is already registered"

  @regression
  Scenario: Duplicate email is rejected
    Given I registered a unique auth user with roles:
      | USER |
    When I register another auth user with the same email
    Then the response status code should be 400
    And the response field "error" should be "Email is already registered"
