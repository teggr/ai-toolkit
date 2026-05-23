# ai-toolkit

**🔗 https://teggr.github.io/ai-toolkit/**

A collection of Copilot customization resources including prompts, skills, agents, and custom instructions.

## CLI installer

Use the JBang script `AiToolkit.java` to install discovery bundle resources from this repository.

```bash
jbang AiToolkit.java [--target <dir> | --global] [--force] <repo-root-name>
```

Examples:

```bash
jbang AiToolkit.java ai-toolkit
jbang AiToolkit.java --target /tmp/copilot-resources ai-toolkit
jbang AiToolkit.java --global --force ai-toolkit
```

## Starter structure

Resources are organized by **purpose** and then by **type**.

Example:

- `discovery/agents/discovery.md`
- `discovery/prompts/`
- `discovery/skills/`
- `discovery/instructions/`

## Skills

### spring-htmx-skill

**Path:** `spring-htmx/skills/spring-htmx-skill.md`

Reference skill for using the [htmx-spring-boot](https://github.com/wimdeblauwe/htmx-spring-boot) library. Covers Maven configuration, request/response headers, HTML fragments, Spring Security integration, and the Thymeleaf dialect. Not invocable — use as context when working with Spring Boot + htmx projects.

## References

- https://docs.github.com/en/copilot
- https://awesome-copilot.github.com/skills/
