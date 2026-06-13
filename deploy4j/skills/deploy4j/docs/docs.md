# deploy4j installation and workflow guide

> Source: https://deploy4j.dev/installation/

Use this guide to install deploy4j and run common setup/deploy workflows.

## CLI-first rule (always)

When helping a user, prefer the installed CLI help over static docs so guidance matches the exact installed version.

Recommended command discovery flow:

```bash
deploy4j --help
deploy4j init --help
deploy4j setup --help
deploy4j deploy --help
```

If a command or flag is unclear, consult the website pages as fallback:

- https://deploy4j.dev/installation/
- https://deploy4j.dev/configuration/overview
- https://deploy4j.dev/guides/server-provisioning
- https://deploy4j.dev/guides/spring-boot

## 1) Install deploy4j

Install via JBang:

```bash
jbang app install --name deploy4j --force --fresh dev.deploy4j:deploy4j-cli:0.0.6
```

Verify:

```bash
deploy4j --help
```

## 2) Initialize project config

From your application directory:

```bash
deploy4j init
```

This creates `config/deploy.yml` and `.deploy4j/secrets` scaffolding.

## 3) Configure deployment

Edit `config/deploy.yml` with your service, image, target servers, registry credentials, and runtime environment variables.

Minimal example:

```yaml
service: hey
image: 37s/hey
servers:
  - 192.168.0.1
  - 192.168.0.2
registry:
  username: registry-user-name
  password:
    - DEPLOY4J_REGISTRY_PASSWORD
env:
  secret:
    - DB_PASSWORD
```

## 4) Manage secrets

Set referenced secret values in `.deploy4j/secrets` (or provide them via environment variables):

```bash
DEPLOY4J_REGISTRY_PASSWORD=***
DB_PASSWORD=***
```

Keep `.deploy4j/secrets` out of version control.

## 5) First-time setup deploy

For initial host bootstrapping and first deploy:

```bash
deploy4j setup
```

Expected setup behavior includes SSH connection, remote Docker/curl install when missing, registry login, image pull, Traefik readiness checks, health check validation (`GET /up`), rolling container replacement, and cleanup.

## 6) Ongoing deploys

After initial setup (or when hosts already satisfy prerequisites), use:

```bash
deploy4j deploy
```

## 7) Operational checks

- Use command help for command-specific options (`deploy4j <command> --help`).
- Confirm app health endpoint returns `200` for `/up`.
- Confirm DNS/load balancer routing for multi-server deployments.
- Re-run `setup` when server prerequisites or base tooling drift.
