package com.yourorg;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;

import java.util.Arrays;

@Value
@EqualsAndHashCode(callSuper = false)
public class SpringBootBean extends Recipe {
    private static final String[] IMPORTS = new String[] { "org.springframework.context.annotation.Bean" };
    private static final String[] CLASSPATH = new String[] { "spring-context" };
    private static final String POLL_MESSAGE = "helloWorld-exists";

    @Option(displayName = "The matching class name",
            description = "Declare the class name to match on for adding the bean method.")
    @NonNull
    String className;

    @JsonCreator
    public SpringBootBean(@JsonProperty String className) {
        this.className = className;
    }

    @Override
    public String getDisplayName() {
        return "Type table example";
    }

    @Override
    public String getDescription() {
        return "Shows the usage of a type table.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                J.MethodDeclaration mi = super.visitMethodDeclaration(method, ctx);

                if (mi.getSimpleName().equals("helloWorld")) {
                    getCursor().dropParentUntil(J.ClassDeclaration.class::isInstance).putMessage(POLL_MESSAGE, true);
                }

                return mi;
            }

            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);

                if (c.getType() == null || !c.getType().getFullyQualifiedName().equals(className)) {
                    return c;
                }

                if (getCursor().pollMessage(POLL_MESSAGE) == null) {
                    c = c.withBody(createJavaTemplate()
                            .apply(new Cursor(getCursor(), c.getBody()),
                                    c.getBody().getCoordinates().lastStatement()));

                    Arrays.stream(IMPORTS).forEach(this::maybeAddImport);
                }

                return c;
            }
        };
    }

    private JavaTemplate createJavaTemplate() {
        //language=java
        return JavaTemplate.builder("@Bean\n" +
                        "public String helloWorld() {\n" +
                        "    return \"Hello, World!\";\n" +
                        "}")
                .imports(IMPORTS)
                .javaParser(JavaParser
                        .fromJavaVersion()
                        .classpath(CLASSPATH))
                .build();
    }
}