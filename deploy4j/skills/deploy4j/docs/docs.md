# deploy4j installation and workflow guide

> Source: https://deploy4j.dev/installation/
> Source: https://deploy4j.dev/configuration/overview
> Source: https://deploy4j.dev/configuration/traefik
> Source: https://deploy4j.dev/hooks/overview

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
- https://deploy4j.dev/configuration/servers
- https://deploy4j.dev/configuration/traefik
- https://deploy4j.dev/configuration/env
- https://deploy4j.dev/configuration/registry
- https://deploy4j.dev/configuration/ssh
- https://deploy4j.dev/configuration/accessory
- https://deploy4j.dev/configuration/boot
- https://deploy4j.dev/configuration/healthcheck
- https://deploy4j.dev/configuration/logging
- https://deploy4j.dev/configuration/spring-boot
- https://deploy4j.dev/hooks/overview
- https://deploy4j.dev/commands/accessory
- https://deploy4j.dev/commands/spring-boot
- https://deploy4j.dev/guides/server-provisioning
- https://deploy4j.dev/guides/spring-boot

## Guide routing map (configuration, commands, hooks)

Use this quick router when users ask where to find specific deploy4j knowledge:

- **Routing and zero-downtime behavior**: https://deploy4j.dev/configuration/traefik
- **Server and role targeting**: https://deploy4j.dev/configuration/servers
- **Env/secrets**: https://deploy4j.dev/configuration/env
- **Registry auth and login**: https://deploy4j.dev/configuration/registry
- **SSH and connectivity**: https://deploy4j.dev/configuration/ssh
- **Accessory services (db/cache/etc.)**: https://deploy4j.dev/configuration/accessory
- **Boot strategy (parallel/batched)**: https://deploy4j.dev/configuration/boot
- **Healthcheck and readiness controls**: https://deploy4j.dev/configuration/healthcheck
- **Container logging config**: https://deploy4j.dev/configuration/logging
- **Spring Boot options and actuator integration**: https://deploy4j.dev/configuration/spring-boot
- **Hooks lifecycle and hook environment variables**: https://deploy4j.dev/hooks/overview
- **Accessory command family docs**: https://deploy4j.dev/commands/accessory
- **Spring Boot management command docs**: https://deploy4j.dev/commands/spring-boot

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

## 8) Command routing workflow

Use this sequence to route users to the right command set quickly:

```bash
deploy4j --help
deploy4j setup --help
deploy4j deploy --help
deploy4j accessory --help
deploy4j spring_boot --help
```

Useful operational commands often used after deployment:

```bash
deploy4j details
deploy4j app logs
deploy4j audit
```

For destination-specific deploys, include `-d` (for example `deploy4j deploy -d staging`).

## 9) Hooks workflow

Hooks are custom scripts that run at key lifecycle points.

- Default path: `.deploy4j/hooks`
- Configurable via `hooks_path` in `config/deploy.yml`
- Hooks abort the command if they exit non-zero
- Disable hooks for a run with `--skip_hooks`

Common lifecycle hooks (see hooks overview for full list and details):

- `docker-setup`
- `pre-connect`
- `pre-deploy`
- `post-deploy`
- `pre-app-boot`
- `post-app-boot`
- `pre-traefik-reboot`
- `post-traefik-reboot`
