# parking-api-testing

API automation framework for the Parking platform.

## Current Scope

The first implemented test area is `parking-auth-service`.

Covered scenarios:

- Public auth service status endpoint
- Register a user with the default `USER` role
- Verify registered user and role in PostgreSQL
- Register an admin user and login successfully
- Reject duplicate username
- Reject duplicate email

## Stack

- Java 21
- Maven
- Rest Assured
- TestNG
- Cucumber
- Allure Reports
- PostgreSQL JDBC

## Environment Configuration

Environment-specific values are stored in:

```text
src/test/resources/config/env
```

Available environment profiles:

```text
local.properties
aws.properties
aws-tunnel.properties
```

The `.properties` files are committed templates. They read real values from OS environment variables or local `.env` files. Local `.env*` files are ignored by Git so secrets do not get pushed.

The default environment is `local`.

Local auth service:

```text
AUTH_BASE_URL=http://localhost:8082
```

AWS auth service:

```text
AUTH_BASE_URL=http://34.240.100.223:8082
```

Choose an environment with `test.env`:

```powershell
mvn test -Dtest.env=local
```

```powershell
mvn test -Dtest.env=aws
```

Value resolution order:

```text
1. Maven system property, for example -Ddb.password=...
2. OS environment variable, for example DB_PASSWORD
3. Local .env file, for example .env.aws-tunnel
4. Default value in the selected .properties profile
```

Individual values can still be overridden with Maven system properties:

```powershell
mvn test -Dauth.baseUrl=http://localhost:8082
```

## Run Tests

From this folder:

```powershell
mvn test
```

Run only smoke tests:

```powershell
mvn test -D"cucumber.filter.tags=@auth and @smoke"
```

Run smoke tests against AWS:

```powershell
mvn test -Dtest.env=aws -D"cucumber.filter.tags=@auth and @smoke"
```

Run AWS smoke tests without direct database checks:

```powershell
mvn test -Dtest.env=aws -D"cucumber.filter.tags=@auth and @smoke and not @db"
```

Run AWS tests with database checks through an EC2 SSH tunnel:

The RDS database is in private subnets, so your laptop cannot connect to it directly. The test framework handles this by connecting to `localhost:15432`, while an SSH tunnel forwards that traffic through the EC2 instance to the private RDS endpoint.

Flow:

```text
Tests on laptop
  -> jdbc:postgresql://localhost:15432/parking_db
  -> SSH tunnel through EC2 34.240.100.223
  -> private RDS endpoint on port 5432
```

The `aws-tunnel` environment starts and stops the SSH tunnel automatically as part of the Cucumber test run.

Run the tests with:

```powershell
mvn test -Dtest.env=aws-tunnel -D"cucumber.filter.tags=@auth and @smoke"
```

No separate SSH terminal is needed for normal execution. The framework starts the tunnel before scenarios run and stops it after the run completes.

Tunnel setting templates are configured in:

```text
src/test/resources/config/env/aws-tunnel.properties
```

Manual tunnel troubleshooting:

If you want to test the tunnel outside Maven, open a separate PowerShell window and keep this tunnel running:

```powershell
ssh -i "D:\Project\Parking\docker\parking-ec2-key.pem" -N -L 15432:<RDS_OR_PRIVATE_DB_HOST>:5432 ubuntu@34.240.100.223
```

Use the real private RDS endpoint or private database host in place of `<RDS_OR_PRIVATE_DB_HOST>`.

You can also use the helper script:

```powershell
.\scripts\start-db-tunnel.ps1
```

The default RDS host used by the script is:

```text
park-db.czcq4k0w4b57.eu-west-1.rds.amazonaws.com
```

The `aws-tunnel` profile sends API traffic to AWS but sends DB traffic to:

```text
jdbc:postgresql://localhost:15432/parking_db
```

You can confirm the local tunnel port is open with:

```powershell
.\scripts\test-db-tunnel.ps1
```

Run only regression tests:

```powershell
mvn test -D"cucumber.filter.tags=@auth and @regression"
```

Run regression tests against AWS:

```powershell
mvn test -Dtest.env=aws -D"cucumber.filter.tags=@auth and @regression"
```

Database validation tests use the `@db` tag. If the AWS PostgreSQL port is not reachable from your machine, exclude DB tests with `and not @db` or override the DB settings with `-Ddb.url`, `-Ddb.username`, and `-Ddb.password`.

Local secret values can be stored in:

```text
.env.local
.env.aws
.env.aws-tunnel
```

Use `.env.example` as the template when creating or updating those files.

These files use uppercase keys and are ignored by Git:

```text
AUTH_BASE_URL
DB_URL
DB_USERNAME
DB_PASSWORD
DB_TUNNEL_ENABLED
DB_TUNNEL_EC2_HOST
DB_TUNNEL_EC2_USER
DB_TUNNEL_KEY_PATH
DB_TUNNEL_RDS_HOST
DB_TUNNEL_RDS_PORT
DB_TUNNEL_LOCAL_HOST
DB_TUNNEL_LOCAL_PORT
```

## Reports

Cucumber HTML report:

```text
target/cucumber-reports/cucumber.html
```

Allure raw results:

```text
target/allure-results
```

Generate the Allure HTML report after a test run:

```powershell
mvn allure:report
```

Open the generated Allure report from:

```text
target/allure-report/index.html
```

Surefire and TestNG CI reports:

```text
target/surefire-reports
```

Important CI files:

```text
target/surefire-reports/TEST-*.xml
target/surefire-reports/testng-results.xml
```
