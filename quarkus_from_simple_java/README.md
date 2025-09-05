# Step-by-Step Quarkus Exploration

This guide walks you through building up a **Quarkus Java application** from a plain Java `main` method, 
adding dependencies one by one to observe how Quarkus initializes each piece.

---

## 0) Plain Java (no Quarkus)

**Layout**
```
basic-app/
  pom.xml
  src/main/java/com/example/Main.java
```

**Minimal `pom.xml`**
```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.example</groupId>
  <artifactId>basic-app</artifactId>
  <version>1.0.0</version>
  <properties>
    <maven.compiler.source>17</maven.compiler.source>
    <maven.compiler.target>17</maven.compiler.target>
  </properties>
</project>
```

**`Main.java`**
```java
package com.example;

public class Main {
  public static void main(String[] args) {
    System.out.println("Plain Java app. No Quarkus yet.");
  }
}
```

Run:
```bash
mvn -q -DskipTests package
java -cp target/classes com.example.Main
```

---

## 1) Add Quarkus platform + CDI only (ARC)

This step boots *Quarkus* and its CDI container, but does nothing else.

**Update `pom.xml`** (add BOM + plugin + ARC dep)
```xml
<properties>
  <maven.compiler.source>17</maven.compiler.source>
  <maven.compiler.target>17</maven.compiler.target>

  <quarkus.platform.group-id>io.quarkus.platform</quarkus.platform.group-id>
  <quarkus.platform.artifact-id>quarkus-bom</quarkus.platform.artifact-id>
  <quarkus.platform.version>3.x.y</quarkus.platform.version>
</properties>

<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>${quarkus.platform.group-id}</groupId>
      <artifactId>${quarkus.platform.artifact-id}</artifactId>
      <version>${quarkus.platform.version}</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<dependencies>
  <dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-arc</artifactId>
  </dependency>
</dependencies>

<build>
<plugins>
  <plugin>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-maven-plugin</artifactId>
    <version>${quarkus.platform.version}</version>
    <extensions>true</extensions>
    <executions>
      <execution>
        <goals>
          <goal>build</goal>
          <goal>generate-code</goal>
          <goal>generate-code-tests</goal>
        </goals>
      </execution>
    </executions>
  </plugin>
</plugins>
</build>
```

**Replace `Main.java`**
```java
package com.example;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@QuarkusMain
public class Main implements QuarkusApplication {

  @Inject Greeter greeter;

  @Override
  public int run(String... args) {
    System.out.println(greeter.hello("from Quarkus + CDI"));
    Quarkus.waitForExit();
    return 0;
  }
}

@ApplicationScoped
class Greeter {
  String hello(String who) { return "Hello " + who; }
}
```

**Log settings (`application.properties`)**
```
quarkus.log.category."io.quarkus".level=INFO
quarkus.log.category."io.quarkus.bootstrap".level=DEBUG
quarkus.log.category."io.quarkus.arc".level=DEBUG
```

Run:
```bash
mvn quarkus:dev
```

---

## 2) Add REST layer (Resteasy Reactive)

**Dependency**
```xml
<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-resteasy-reactive</artifactId>
</dependency>
```

**`HelloResource.java`**
```java
package com.example;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/hello")
public class HelloResource {

  @Inject Greeter greeter;

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  public String hello() {
    return greeter.hello("from REST");
  }
}
```

Run:
```bash
mvn quarkus:dev
```
Visit [http://localhost:8080/hello](http://localhost:8080/hello).

---

## 3) Externalize configuration

**`application.properties`**
```
greeting.prefix=Hi
```

**Config injection**
```java
import org.eclipse.microprofile.config.inject.ConfigProperty;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
class Greeter {
  @ConfigProperty(name = "greeting.prefix", defaultValue = "Hello")
  String prefix;

  String hello(String who) { return prefix + " " + who; }
}
```

---

## 4) Add persistence (H2 + Hibernate ORM Panache)

**Dependencies**
```xml
<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-hibernate-orm-panache</artifactId>
</dependency>
<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-jdbc-h2</artifactId>
</dependency>
```

**Config**
```
quarkus.datasource.jdbc.url=jdbc:h2:mem:test;DB_CLOSE_DELAY=-1
quarkus.datasource.username=sa
quarkus.datasource.password=sa
quarkus.hibernate-orm.database.generation=drop-and-create
quarkus.hibernate-orm.log.sql=true
```

**Entity**
```java
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;

@Entity
public class Message extends PanacheEntity {
  public String text;
}
```

**Resource**
```java
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/messages")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MessageResource {

  @POST
  @Transactional
  public Message create(Message m) {
    m.persist();
    return m;
  }

  @GET
  public List<Message> all() {
    return Message.listAll();
  }
}
```

Test:
```bash
curl -X POST -H "Content-Type: application/json"      -d '{"text":"first!"}' http://localhost:8080/messages
curl http://localhost:8080/messages
```

---

## 5) Observe lifecycle explicitly

```java
import jakarta.enterprise.event.Observes;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.ShutdownEvent;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class LifecycleObserver {
  void onStart(@Observes StartupEvent ev) {
    System.out.println(">>> StartupEvent observed");
  }
  void onStop(@Observes ShutdownEvent ev) {
    System.out.println(">>> ShutdownEvent observed");
  }
}
```

---

## 6) Add health endpoints (optional)

**Dependency**
```xml
<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-smallrye-health</artifactId>
</dependency>
```

Endpoints:
- `/q/health`
- `/q/health/live`
- `/q/health/ready`

---

## 7) Package & run as runnable JAR

```bash
mvn -DskipTests package
java -jar target/quarkus-app/quarkus-run.jar
```

---

## Gradle Notes

- Plugin:  
  `plugins { id 'java'; id 'io.quarkus' version '3.x.y' }`

- Import BOM:  
  `implementation(enforcedPlatform("io.quarkus.platform:quarkus-bom:3.x.y"))`

- Add extensions:  
  `implementation("io.quarkus:quarkus-arc")`, etc.

- Run:  
  `./gradlew quarkusDev`

---

## What to Observe

- **Bootstrap**: `io.quarkus.bootstrap` logs (build-time augmentation).
- **CDI**: `io.quarkus.arc` logs (bean discovery, init).
- **HTTP**: Vert.x server start + route registration.
- **Persistence**: Datasource creation, schema generation.
- **Lifecycle**: `StartupEvent` and `ShutdownEvent` observers.
