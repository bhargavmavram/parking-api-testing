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
aws-ec2.properties
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
mvn clean test
```

Run only smoke tests:

```powershell
mvn clean test -D"cucumber.filter.tags=@auth and @smoke"
```

Run smoke tests against AWS:

```powershell
mvn clean test -Dtest.env=aws -D"cucumber.filter.tags=@auth and @smoke"
```

Run AWS smoke tests without direct database checks:

```powershell
mvn clean test -Dtest.env=aws -D"cucumber.filter.tags=@auth and @smoke and not @db"
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
mvn clean test -Dtest.env=aws-tunnel -D"cucumber.filter.tags=@auth and @smoke"
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
mvn clean test -D"cucumber.filter.tags=@auth and @regression"
```

Run regression tests against AWS:

```powershell
mvn clean test -Dtest.env=aws -D"cucumber.filter.tags=@auth and @regression"
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

## GitHub Actions

The API tests can run from GitHub Actions using:

```text
.github/workflows/api-tests.yml
```

The workflow supports:

- Manual runs with `workflow_dispatch`
- Pull request smoke runs
- Scheduled daily smoke runs
- Self-hosted runner execution from EC2
- Cucumber HTML artifact upload
- Allure HTML artifact upload
- Surefire/TestNG report artifact upload

The workflow uses:

```yaml
runs-on: self-hosted
```

This means tests run on your EC2 GitHub Actions runner instead of GitHub-hosted infrastructure. This is the recommended setup because EC2 can reach the private RDS database.

Manual run:

```text
GitHub -> Actions -> Parking API Tests -> Run workflow
```

Inputs:

```text
test_env: local | aws | aws-ec2 | aws-tunnel
cucumber_tags: @auth and @smoke
```

Recommended first GitHub run:

```text
test_env: aws-ec2
cucumber_tags: @auth and @smoke
```

Required GitHub Secrets for EC2/RDS test runs:

```text
AUTH_BASE_URL
DB_URL
DB_USERNAME
DB_PASSWORD
```

Suggested self-hosted EC2 values:

```text
AUTH_BASE_URL=http://localhost:8082
DB_URL=jdbc:postgresql://park-db.czcq4k0w4b57.eu-west-1.rds.amazonaws.com:5432/parking_db
DB_USERNAME=parking_user
DB_PASSWORD=<RDS password>
```

Optional GitHub Secrets for `aws-tunnel`:

```text
SSH_PRIVATE_KEY
DB_TUNNEL_ENABLED
DB_TUNNEL_EC2_HOST
DB_TUNNEL_EC2_USER
DB_TUNNEL_RDS_HOST
DB_TUNNEL_RDS_PORT
```

Suggested values:

```text
AUTH_BASE_URL=http://34.240.100.223:8082
DB_USERNAME=parking_user
DB_PASSWORD=<RDS password>
DB_TUNNEL_ENABLED=true
DB_TUNNEL_EC2_HOST=34.240.100.223
DB_TUNNEL_EC2_USER=ubuntu
DB_TUNNEL_RDS_HOST=park-db.czcq4k0w4b57.eu-west-1.rds.amazonaws.com
DB_TUNNEL_RDS_PORT=5432
```

`SSH_PRIVATE_KEY` should contain the private key content, not the file path.

Set up the self-hosted runner:

```text
GitHub -> parking-api-testing -> Settings -> Actions -> Runners -> New self-hosted runner
```

Choose Linux and run the generated commands on EC2. Start the runner as a service so it stays online after logout.

EC2 runner prerequisites:

```bash
java -version
mvn -version
git --version
```

If Java or Maven are missing on EC2, install them before running the workflow.
