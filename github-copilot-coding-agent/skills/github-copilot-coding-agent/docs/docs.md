# Copilot Java setup guide

> Source: https://dev.to/brunoborges/how-to-configure-jdk-25-for-github-copilot-coding-agent-2ecn
> Source: https://docs.github.com/en/copilot/customizing-copilot/customizing-the-development-environment-for-copilot-coding-agent

The GitHub Copilot coding agent runs in an ephemeral GitHub Actions environment. By default it uses whatever JDK is pre-installed on the runner. To ensure it uses a specific version (such as Java 25), you must provide a `copilot-setup-steps.yml` workflow that runs before the agent starts.

## Critical constraints

- **File path**: `.github/workflows/copilot-setup-steps.yml` (exact)
- **Job name**: `copilot-setup-steps` (exact — Copilot ignores jobs with any other name)
- **Environment**: Use the `copilot` GitHub Actions environment for secrets (not the default environment)

---

## Template: Maven project (Java 25)

```yaml
name: "Copilot Setup Steps"

on:
  workflow_dispatch:
  push:
    paths:
      - .github/workflows/copilot-setup-steps.yml
  pull_request:
    paths:
      - .github/workflows/copilot-setup-steps.yml

jobs:
  copilot-setup-steps:
    runs-on: ubuntu-latest
    permissions:
      contents: read
    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 25
        uses: actions/setup-java@v4
        with:
          java-version: '25'
          distribution: 'temurin'
          cache: 'maven'

      - name: Download dependencies
        run: mvn dependency:go-offline -B
```

---

## Template: Gradle project (Java 25)

```yaml
name: "Copilot Setup Steps"

on:
  workflow_dispatch:
  push:
    paths:
      - .github/workflows/copilot-setup-steps.yml
  pull_request:
    paths:
      - .github/workflows/copilot-setup-steps.yml

jobs:
  copilot-setup-steps:
    runs-on: ubuntu-latest
    permissions:
      contents: read
    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 25
        uses: actions/setup-java@v4
        with:
          java-version: '25'
          distribution: 'temurin'
          cache: 'gradle'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Download dependencies
        run: ./gradlew dependencies --write-locks
```

---

## Template: Maven project with private repository

Add secrets to the `copilot` environment in your repository (Settings → Environments → copilot → Add secret), then reference them:

```yaml
name: "Copilot Setup Steps"

on:
  workflow_dispatch:
  push:
    paths:
      - .github/workflows/copilot-setup-steps.yml
  pull_request:
    paths:
      - .github/workflows/copilot-setup-steps.yml

jobs:
  copilot-setup-steps:
    runs-on: ubuntu-latest
    environment: copilot
    permissions:
      contents: read
    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 25
        uses: actions/setup-java@v4
        with:
          java-version: '25'
          distribution: 'temurin'
          cache: 'maven'
          server-id: private-repo
          server-username: ${{ secrets.MAVEN_USERNAME }}
          server-password: ${{ secrets.MAVEN_PASSWORD }}

      - name: Download dependencies
        run: mvn dependency:go-offline -B
```

---

## JDK distributions supported by actions/setup-java

| Distribution  | Vendor           | Notes                           |
|---------------|------------------|---------------------------------|
| `temurin`     | Eclipse Adoptium | Recommended, community standard |
| `zulu`        | Azul             | Good compatibility              |
| `corretto`    | Amazon           | AWS-optimized                   |
| `microsoft`   | Microsoft        | Azure-optimized                 |
| `oracle`      | Oracle           | Official Oracle JDK             |
| `liberica`    | BellSoft         | Full and lite versions          |

Default to `temurin` unless the user has a specific requirement.

---

## Firewall allowlist

The Copilot agent enforces a network firewall. Most public Java package repositories (Maven Central, Gradle Plugin Portal, etc.) are allowed by default.

To allow additional domains (e.g. a corporate Nexus or Artifactory instance):

1. Go to repository **Settings → Secrets and variables → Actions → Variables**.
2. Add a repository variable:
   - **Name**: `COPILOT_AGENT_FIREWALL_ALLOW_LIST_ADDITIONS`
   - **Value**: comma-separated hostnames, e.g. `nexus.corp.example.com,artifactory.internal.example.com`

If a domain is blocked, the Copilot agent will report the blocked host in a PR comment — use that to identify what to add.

---

## Optional enhancements

### Pre-compile the project (faster Copilot startup)

```yaml
- name: Compile project
  run: mvn compile test-compile -DskipTests -B
```

### Generate annotation-processed sources (Lombok, MapStruct, etc.)

```yaml
- name: Generate sources
  run: mvn generate-sources generate-test-sources -B
```

### Install multi-module project locally

```yaml
- name: Install modules
  run: mvn install -DskipTests -B
```

### Use a larger runner for big projects

```yaml
jobs:
  copilot-setup-steps:
    runs-on: ubuntu-4-core
```

---

## Validation

The workflow triggers automatically on push/PR when the workflow file changes, and can also be triggered manually from the **Actions** tab. Always validate it passes before relying on it for Copilot sessions.
