package com.yourorg;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class SpringBootBeanTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new SpringBootBean("com.bmuschko.MyApp"))
          .parser(JavaParser.fromJavaVersion()
            .classpath("spring-boot", "spring-boot-autoconfigure"));
    }

    @Test
    void ignoresForNonMatchingClassName() {
        rewriteRun(
          //language=java
          java(
            """
              package com.bmuschko;

              public class Other {
                  public void doSomething() {
                      System.out.println("Doing something");
                  }
              }
              """
          )
        );
    }

    @Test
    void addsMethodIntoNonSpringClass() {
        rewriteRun(
          //language=java
          java(
            """
              package com.bmuschko;

              public class MyApp {
                  public void doSomething() {
                      System.out.println("Doing something");
                  }
              }
              """,
            """
              package com.bmuschko;
              
              import org.springframework.context.annotation.Bean;
              
              public class MyApp {
                  public void doSomething() {
                      System.out.println("Doing something");
                  }

                  @Bean
                  public String helloWorld() {
                      return "Hello, World!";
                  }
              }
              """
          )
        );
    }

    @Test
    void addsMethodIntoSpringClass() {
        rewriteRun(
          //language=java
          java(
            """
              package com.bmuschko;

              import org.springframework.boot.SpringApplication;
              import org.springframework.boot.autoconfigure.SpringBootApplication;

              @SpringBootApplication
              public class MyApp {
                  public static void main(String[] args) {
                      SpringApplication.run(MyApp.class, args);
                  }
              }
              """,
            """
              package com.bmuschko;
              
              import org.springframework.boot.SpringApplication;
              import org.springframework.boot.autoconfigure.SpringBootApplication;
              import org.springframework.context.annotation.Bean;
              
              @SpringBootApplication
              public class MyApp {
                  public static void main(String[] args) {
                      SpringApplication.run(MyApp.class, args);
                  }

                  @Bean
                  public String helloWorld() {
                      return "Hello, World!";
                  }
              }
              """
          )
        );
    }
}
