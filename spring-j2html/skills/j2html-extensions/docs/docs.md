# j2html-extensions for Spring Boot

Source: https://github.com/teggr/j2html-extensions

A collection of extensions for the [j2html](https://j2html.com) library, used for building HTML with Java code. The suite integrates cleanly with Spring Boot, Spring Web MVC, Bootstrap, and HTMX.

## Modules

| Artifact | Purpose |
|---|---|
| `j2html-extensions-core` | Extra HTML tags (`svg`, `path`, `use`, `symbol`) and ARIA helpers |
| `bootstrap-j2html-extension` | Bootstrap CSS class constants and component creators |
| `htmx-j2html-extension` | HTMX attribute helpers and tag creator |
| `j2html-extensions-spring-boot-starter` | Spring Boot auto-configuration wiring all of the above |
| `j2html-engine-spring-boot-starter` | Template engine with `@HtmlTemplate` scanning and Spring MVC view resolution |

---

## Maven configuration

### BOM (recommended)

Add the BOM to `<dependencyManagement>` to align all artifact versions:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>dev.rebelcraft</groupId>
            <artifactId>j2html-extensions-bom</artifactId>
            <version>0.0.3</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### Spring Boot starter (extensions)

Provides auto-configuration for the core, Bootstrap, and HTMX extensions:

```xml
<dependency>
    <groupId>dev.rebelcraft</groupId>
    <artifactId>j2html-extensions-spring-boot-starter</artifactId>
</dependency>
```

### Bootstrap extension

```xml
<dependency>
    <groupId>dev.rebelcraft</groupId>
    <artifactId>bootstrap-j2html-extension</artifactId>
</dependency>
```

### HTMX extension

```xml
<dependency>
    <groupId>dev.rebelcraft</groupId>
    <artifactId>htmx-j2html-extension</artifactId>
</dependency>
```

### Template engine Spring Boot starter

Enables `@HtmlTemplate` scanning and MVC view resolution:

```xml
<dependency>
    <groupId>dev.rebelcraft</groupId>
    <artifactId>j2html-engine-spring-boot-starter</artifactId>
</dependency>
```

---

## Core extensions (`j2html-extensions-core`)

### Extra HTML tags

```java
import dev.rebelcraft.j2html.ext.ExtendedTagCreator;

ExtendedTagCreator.svg();
ExtendedTagCreator.path();
ExtendedTagCreator.use();
ExtendedTagCreator.symbol();
```

### ARIA attributes

```java
import dev.rebelcraft.j2html.ext.aria.AriaRoles;
import dev.rebelcraft.j2html.ext.aria.AriaStatesAndProperties;
import static j2html.TagCreator.*;

div().attr(AriaRoles.roleButton);                         // <div role="button">
div().attr(AriaStatesAndProperties.aria_current("page")); // <div aria-current="page">
```

---

## Bootstrap extension (`bootstrap-j2html-extension`)

### CSS class constants

```java
import dev.rebelcraft.j2html.bootstrap.Bootstrap;
import dev.rebelcraft.j2html.ext.aria.AriaRoles;
import static j2html.TagCreator.*;

div()
    .withClasses(Bootstrap.alert, Bootstrap.alert_primary)
    .attr(AriaRoles.roleAlert)
    .with(text("A simple primary alert—check it out!"));
```

Key constant classes:
- `Bootstrap` — all Bootstrap utility and component CSS classes
- `BootstrapConfig` — CDN and WebJar URLs
- `BootstrapIcons` — Bootstrap Icons CSS classes
- `BootstrapTagCreator` — pre-built Bootstrap component templates

---

## HTMX extension (`htmx-j2html-extension`)

### Attribute-based approach

```java
import static dev.rebelcraft.j2html.htmx.HtmxAttributes.*;
import static j2html.TagCreator.*;

button()
    .attr(hxPost("/clicked"))
    .attr(hxTrigger(click))
    .attr(hxTarget("#parent-div"))
    .attr(hxSwap(outerHTML))
    .with(text("Click Me!"));
```

### Fluent approach

```java
import static dev.rebelcraft.j2html.htmx.Htmx.*;
import static j2html.TagCreator.*;

hx(button(), (hx) -> hx
    .post("/clicked")
    .trigger(click)
    .target("#parent-div")
    .swap(outerHTML))
    .with(text("Click Me!"));
```

---

## Template engine (`j2html-engine-spring-boot-starter`)

### Spring Boot configuration

Set the base package(s) to scan for `@HtmlTemplate` components in `application.properties`:

```properties
j2html.base-packages=com.example.views
```

If not set, the starter defaults to the package of your `@SpringBootApplication` class.

### Logging

```properties
logging.level.dev.rebelcraft.engine=DEBUG
logging.level.dev.rebelcraft.engine.spring=DEBUG
logging.level.dev.rebelcraft.engine.spring.boot=INFO
```

### Defining a template

Annotate a class with `@HtmlTemplate` and implement `HtmlComponent`:

```java
import dev.rebelcraft.engine.HtmlComponent;
import dev.rebelcraft.engine.RenderContext;
import dev.rebelcraft.engine.spring.HtmlTemplate;
import j2html.tags.DomContent;
import static j2html.TagCreator.*;

@HtmlTemplate("home/index")
public class HomeIndexTemplate implements HtmlComponent {
    @Override
    public DomContent render(RenderContext ctx) {
        String title = ctx.require("title", String.class);
        return html(
            head(title(title)),
            body(
                h1(title),
                div().withId("content").with(text("Hello from j2html!"))
            )
        );
    }
}
```

Return the view name from a Spring MVC controller:

```java
@Controller
@RequestMapping("/")
public class HomeController {
    @GetMapping
    public String home(Model model) {
        model.addAttribute("title", "Home");
        return "home/index";
    }
}
```

### RenderContext API

| Method | Description |
|---|---|
| `ctx.require("key", Type.class)` | Mandatory value; throws if absent |
| `ctx.find("key", Type.class)` | Optional value as `Optional<T>` |
| `ctx.has("key")` | Boolean presence check |
| `ctx.locale()` | Active `Locale` |
| `ctx.message("key", args...)` | Localized message via configured `MessageResolver` |
| `ctx.include("templateName")` | Render a child template inline |
| `ctx.include("templateName", Map.of(...))` | Render a child template with extra variables |
| `ctx.include("templateName", child -> child.with(...))` | Fluent child context setup |
| `ctx.childContext().with(...).build()` | Create a derived context for branching |

### Template composition

```java
@HtmlTemplate("layout/page")
public class PageLayoutTemplate implements HtmlComponent {
    @Override
    public DomContent render(RenderContext ctx) {
        return div(
            header(ctx.include("layout/header")),
            main(ctx.include("content/home")),
            footer(ctx.include("layout/footer"))
        );
    }
}
```

---

## HTMX + j2html patterns in Spring Boot

### HTMX fragment endpoint

Return a partial view from a controller method triggered by an HTMX request. The template renders only the fragment, not the full page:

```java
@Controller
@RequestMapping("/items")
public class ItemController {

    @GetMapping("/{id}")
    public String itemDetail(@PathVariable Long id, Model model) {
        model.addAttribute("item", itemService.findById(id));
        return "items/detail";  // renders full page
    }

    @GetMapping("/{id}/fragment")
    public String itemDetailFragment(@PathVariable Long id, Model model) {
        model.addAttribute("item", itemService.findById(id));
        return "items/detail-fragment";  // renders only the fragment
    }
}
```

### HTMX-enabled template

Combine `htmx-j2html-extension` attributes inside an `@HtmlTemplate`:

```java
import dev.rebelcraft.j2html.bootstrap.Bootstrap;
import dev.rebelcraft.j2html.ext.aria.AriaRoles;
import static dev.rebelcraft.j2html.htmx.HtmxAttributes.*;
import static dev.rebelcraft.j2html.htmx.Htmx.*;
import static j2html.TagCreator.*;

@HtmlTemplate("items/list")
public class ItemListTemplate implements HtmlComponent {
    @Override
    public DomContent render(RenderContext ctx) {
        List<Item> items = ctx.require("items", List.class);
        return div().withClasses(Bootstrap.container).with(
            ul().withClasses(Bootstrap.list_group).with(
                each(items, item ->
                    li().withClasses(Bootstrap.list_group_item).with(
                        span(item.getName()),
                        hx(button(), hx -> hx
                            .get("/items/" + item.getId() + "/fragment")
                            .target("#detail-panel")
                            .swap(innerHTML))
                            .withClasses(Bootstrap.btn, Bootstrap.btn_sm, Bootstrap.btn_primary)
                            .with(text("View"))
                    )
                )
            ),
            div().withId("detail-panel")
        );
    }
}
```

### Out-of-band swaps

Return multiple fragments for HTMX out-of-band swaps using Spring's `FragmentsRendering`:

```java
@GetMapping("/items")
public View items(Model model) {
    model.addAttribute("items", itemRepository.findAll());
    model.addAttribute("count", itemRepository.count());
    return FragmentsRendering
        .with("items/list")
        .fragment("items/count")
        .build();
}
```
