# j2html examples reference

Source: https://j2html.com/examples.html

This document contains a local copy of the examples content for offline/reference use.

## Basic example

Creating a basic HTML structure in j2html is pretty similar to plain HTML. This Java code:

- version 1.0.0 +
- earlier versions

```java
html(
    head(
        title("Title"),
        link().withRel("stylesheet").withHref("/css/main.css")
    ),
    body(
        main(attrs("#main.content"),
            h1("Heading!")
        )
    )
)
```

```java
html().with(
    head().with(
        title("Title"),
        link().withRel("stylesheet").withHref("/css/main.css")
    ),
    body().with(
        main().withId("main").withClass("content").with(
            h1("Heading!")
        )
    )
)
```

Becomes this HTML:

```html
<html>
    <head>
        <title>Title</title>
        <link rel="stylesheet" href="/css/main.css">
    </head>
    <body>
        <main id="main" class="content">
            <h1>Heading!</h1>
        </main>
    </body>
<html>
```

It's literally impossible to forget to close a div, mistype an attribute name, or forget an attribute quote! Remember to include the Java wrapping code though, j2html is not a template language, all files are .java.

## Core concepts

```java
TagCreator.class // Static utility class for creating all tags
import static j2html.TagCreator.*; // Use static star import


Config.class // Holds all configuration.  Offers global configuration or customizable instances
Config.closeEmptyTags = true // Global options are public, static and mutable.
Config.global() // Copy all static Config fields into an instance.  Instances are immutable
Config.defaults() // A Config with defaults that are independent of global options
Config.global().withEmptyTagsClosed(true) // A Config that is different from the global options
Config.defaults().withEmptyTagsClosed(true) // A Config that is different from the default options


TagCreator.join() // Method for joining small snippets, like:
p(join("This paragraph has", b("bold"), "and", i("italic"), "text."))


TagCreator.iff(boolean condition, T ifValue) // If-expression for use in method calls
div().withClasses("menu-element", iff(isActive, "active"))


TagCreator.iffElse(boolean condition, T ifValue, T elseValue) // If/else-expression for use in method calls
div().withClasses("menu-element", iffElse(isActive, "active", "not-active"))


Tag.class // Is extended by ContainerTag (ex <div></div> and EmptyTag (ex <br>)
Tag.attr(String attribute, Object value) // Set an attribute on the tag
Tag.withXyz(String value) // Calls attr with predefined attribute (ex .withId, .withClass, etc.)
Tag.render(HtmlBuilder builder) // Render HTML using the given builder.
Tag.render() // Shortcut for rendering flat HTML into a string using global Config.
ContainerTag.renderFormatted() // Shortcut for rendering indented HTML into a string using global Config.

HtmlBuilder.class // Interface for composing HTML. Implemented by FlatHtml and IndentedHtml
FlatHtml.into(Appendable) // Render into a stream, file, etc. without indentation or line breaks
FlatHtml.into(Appendable appendable, Config config) // Customize rendering of flat html
IndentedHtml.into(Appendable) // Render human-readable HTML into an stream, file, etc.
IndentedHtml.into(Appendable appendable, Config config) // Customize rendering of intended html
ul(li("one"), li("two")).render(IndentedHtml.inMemory()).toString() // Similar to renderFormatted()
ul(li("one"), li("two")).render(IndentedHtml.into(filewriter)) // Write HTML into a file
```

## Loops, each() and filter()

Using Java 8's lambda syntax, you can write loops (via streams) inside your HTML-builder:

- version 1.0.0 +
- earlier versions

```java
body(
    div(attrs("#employees"),
        employees.stream().map(employee ->
            div(attrs(".employee"),
                h2(employee.getName()),
                img().withSrc(employee.getImgPath()),
                p(employee.getTitle())
            )
        ).toArray(ContainerTag[]::new)
    )
)
```

```java
body().with(
    div().withId("employees").with(
        employees.stream().map(employee ->
            div().withClass("employee").with(
                h2(employee.getName()),
                img().withSrc(employee.getImgPath()),
                p(employee.getTitle())
            )
        ).toArray(ContainerTag[]::new)
    )
)
```

j2html also offers a custom each method, which is slightly more powerful:

```java
// each() lets you iterate through a collection and returns the generated HTML
// as a DomContent object, meaning you can add siblings, which is not possible
// using the stream api in the previous example
body(
    div(attrs("#employees"),
        p("Some sibling element"),
        each(employees, employee ->
            div(attrs(".employee"),
                h2(employee.getName()),
                img().withSrc(employee.getImgPath()),
                p(employee.getTitle())
            )
        )
    )
)
```

If you need to filter your collection, j2html has a built in filter function too:

```java
// filter() is meant to be used with each(). It just calls the normal
// stream().filter() method, but hides all the boilerplate Java code
// to keep your j2html code neat
body(
    div(attrs("#employees"),
        p("Some sibling element"),
        each(filter(employees, employee -> employee != null), employee ->
            div(attrs(".employee"),
                h2(employee.getName()),
                img().withSrc(employee.getImgPath()),
                p(employee.getTitle())
            )
        )
    )
)
```

Given three random employees, all the above approaches would give the same HTML:

```html
<body>
    <div id="employees">
        <div class="employee">
            <h2>David</h2>
            <img src="/img/david.png">
            <p>Creator of Bad Libraries</p>
        </div>
        <div class="employee">
            <h2>Christian</h2>
            <img src="/img/christian.png">
            <p>Fanboi of Jenkins</p>
        </div>
        <div class="employee">
            <h2>Paul</h2>
            <img src="/img/paul.png">
            <p>Hater of Lambda Expressions</p>
        </div>
    </div>
</body>
```

## Two dimensional table example

```java
List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 9, 10);
...
table(attr("#table-example"),
    tbody(
        each(numbers, i -> tr(
            each(numbers, j -> td(
                String.valueOf(i * j)
            ))
        ))
    )
)
```

```java
List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 9, 10);
...
table().withId("table-example").with(
    tbody().with(
        each(numbers, i -> tr().with(
            each(numbers, j -> td().with(
                String.valueOf(i * j)
            ))
        ))
    )
)
```

## Partials

You can create partials for elements you use a lot:

```java
public static Tag enterPasswordInput(String placeholder) {
    return passwordInput("enterPassword", placeholder);
}

public static Tag choosePasswordInput(String placeholder) {
    return passwordInput("choosePassword", placeholder);
}

public static Tag repeatPasswordInput(String placeholder) {
    return passwordInput("repeatPassword", placeholder);
}

public static Tag passwordInput(String identifier, String placeholder) {
    return input()
        .withType("password")
        .withId(identifier)
        .withName(identifier)
        .withPlaceholder(placeholder)
        .isRequired();
}

public static Tag emailInput(String placeholder) {
    return input()
        .withType("email")
        .withId("email")
        .withName("email")
        .withPlaceholder(placeholder)
        .isRequired();
}

public static Tag submitButton(String text) {
    return button(text).withType("submit");
}
```

You can then use these partials, for example in a registration form:

```java
h1("Please sign up"),
form().withMethod("post").with(
    emailInput("Email address"),
    choosePasswordInput("Choose Password"),
    repeatPasswordInput("Repeat Password"),
    submitButton("Sign up")
)
```

## Dynamic views

Once you've set up partials, you can call them from wherever, which greatly reduces potential errors. If we want to insert our form in a header/footer frame, we can create a MainView and make it take our view as an argument:

```java
public class MainView {
    public static String render(String pageTitle, Tag... tags) {
        return document(
            html(
                head(
                    title(pageTitle)
                ),
                body(
                    header(
                        ...
                    ),
                    main(
                        tags //the view from the partials example
                    ),
                    footer(
                        ...
                    )
                )
            )
        );
    }
}

MainView.render(
    "Signup page",
    h1("Please sign up"),
    form().withMethod("post").with(
        emailInput("Email address"),
        choosePasswordInput("Choose Password"),
        repeatPasswordInput("Repeat Password"),
        submitButton("Sign up")
    )
);
```

```html
<html>
    <head>
        <title>Signup page</title>
    </head>
    <body>
    <header>
        ...
    </header>
    <main>
        <h1>Please sign up</h1>
        <form method="post">
            <input type="email" id="email" name="email" placeholder="Email address" required>
            <input type="password" id="choosePassword" name="choosePassword" placeholder="Choose password" required>
            <input type="password" id="repeatPassword" name="repeatPassword" placeholder="Repeat password" required>
            <button type="submit">Sign up</button>
        </form>
    </main>
    <footer>
        ...
    </footer>
    </body>
</html>
```
