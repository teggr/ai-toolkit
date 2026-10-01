//JAVA 21+
//DEPS info.picocli:picocli:4.7.6
//SOURCES ../AiToolkit.java

import java.io.IOException;
import java.nio.file.Path;

public class AiToolkitPathTest {
    public static void main(String[] args) throws Exception {
        String metadata = """
            ---
            source_owner: example
            source_repo: skills
            source_path: skills/nested
            install_path: skills/example
            ---
            """;
        AiToolkit.ResourceSpec valid = AiToolkit.parseRecommendedResourceSpec("example", "example.md", metadata);
        check(valid.sourcePath().equals("skills/nested"), "valid source path");
        check(valid.installPath().equals("skills/example"), "valid install path");

        for (String unsafe : new String[] {
                "../outside", "skills/../outside", "skills/./example",
                "/rooted", "\\rooted", "C:\\outside", "C:outside",
                "//server/share", "skills//example", "skills/"
        }) {
            for (String field : new String[] {"source_path", "install_path"}) {
                expectRejected(() -> AiToolkit.parseRecommendedResourceSpec(
                    "example", "example.md", metadata.replace(field + ": skills/"
                        + (field.equals("source_path") ? "nested" : "example"), field + ": " + unsafe)),
                    field + " accepted " + unsafe);
            }
        }

        Path root = Path.of("test-install-root").toAbsolutePath().normalize();
        check(AiToolkit.mapDestination(root, "skills/example/SKILL.md")
            .equals(root.resolve("skills/example/SKILL.md")), "ordinary destination");
        check(AiToolkit.mapDestination(root, "instructions/rules.instructions.md")
            .equals(root.resolve("instructions/rules.instructions.md")), "specific instruction destination");
        for (String unsafe : new String[] {"../outside", "skills/../../outside", "/outside",
                "C:\\outside", "skills\\..\\outside", "skills/./file.md"}) {
            expectRejected(() -> AiToolkit.mapDestination(root, unsafe),
                "destination accepted " + unsafe);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void expectRejected(CheckedAction action, String message) throws Exception {
        try {
            action.run();
        } catch (IOException expected) {
            return;
        }
        throw new AssertionError(message);
    }

    private interface CheckedAction {
        void run() throws Exception;
    }
}
