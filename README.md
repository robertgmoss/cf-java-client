# Cloud Foundry Java Client
The `cf-java-client` project is a Java language binding for interacting with a Cloud Foundry instance.  The project is broken up into a number of components which expose different levels of abstraction depending on need.

* `cloudfoundry-client` – Interfaces, request, and response objects mapping to the [Cloud Foundry REST APIs][a].  This project has no implementation and therefore cannot connect a Cloud Foundry instance on its own.
* `cloudfoundry-client-spring` – The default implementation of the `cloudfoundry-client` project.  This implementation is based on the Spring Framework [`RestTemplate`][t].
* `cloudfoundry-operations` – An API and implementation that corresponds to the [Cloud Foundry CLI][c] operations.  This project builds on the `cloudfoundry-cli` and therefore has a single implementation.
* `cloudfoundry-maven-plugin` / `cloudfoundry-gradle-plugin` – Build plugins for [Maven][m] and [Gradle][g].  These projects build on `cloudfoundry-operations` and therefore have single implementations.

Most projects will need two dependencies; the Operations API and an implementation of the Client API.  For Maven, the dependencies would be defined like this:

```xml
<dependencies>
    <dependency>
        <groupId>org.cloudfoundry</groupId>
        <artifactId>cloudfoundry-client-spring</artifactId>
        <version>2.0.0.BUILD-SNAPSHOT</version>
    </dependency>
    <dependency>
        <groupId>org.cloudfoundry</groupId>
        <artifactId>cloudfoundry-operations</artifactId>
        <version>2.0.0.BUILD-SNAPSHOT</version>
    </dependency>
    <dependency>
        <groupId>io.projectreactor</groupId>
        <artifactId>reactor-core</artifactId>
        <version>2.5.0.M2</version>
    </dependency>
    ...
</dependencies>
```

The artifacts can be found in the Spring release and snapshot repositories:

```xml
<repositories>
    <repository>
        <id>spring-releases</id>
        <name>Spring Releases</name>
        <url>http://repo.spring.io/release</url>
    </repository>
    ...
</repositories>
```

```xml
<repositories>
    <repository>
        <id>spring-snapshots</id>
        <name>Spring Snapshots</name>
        <url>http://repo.spring.io/snapshot</url>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
    ...
</repositories>
```

For Gradle, the dependencies would be defined like this:

```groovy
dependencies {
    compile 'org.cloudfoundry:cloudfoundry-client-spring:2.0.0.BUILD-SNAPSHOT'
    compile 'org.cloudfoundry:cloudfoundry-operations:2.0.0.BUILD-SNAPSHOT'
    compile 'io.projectreactor:reactor-core:2.5.0.M2'
    ...
}
```

The artifacts can be found in the Spring release and snapshot repositories:

```groovy
repositories {
    maven { url 'http://repo.spring.io/release' }
    ...
}
```

```groovy
repositories {
    maven { url 'http://repo.spring.io/snapshot' }
    ...
}
```

## Usage
Both the `cloudfoundry-operations` and `cloudfoundry-client` projects follow a ["Reactive"][r] design pattern and expose their responses with [Project Reactor][p] `Monos`s and `Flux`s.

### `CloudFoundryClient` and `CloudFoundryOperations` Builders

The lowest-level building block of the API is a `CloudFoundryClient`.  This is only an interface and the default implementation of this is the `SpringCloudFoundryClient`.  To instantiate one, you configure it with a builder:

```java
SpringCloudFoundryClient.builder()
    .host("api.run.pivotal.io")
    .username("example-username")
    .password("example-password")
    .build();
```

In Spring-based applications, you'll want to encapsulate this in a bean definition:

```java
@Bean
CloudFoundryClient cloudFoundryClient(@Value("${cf.host}") String host,
                                      @Value("${cf.username}") String username,
                                      @Value("${cf.password}") String password) {
    return SpringCloudFoundryClient.builder()
            .host(host)
            .username(username)
            .password(password)
            .build();
}
```

The `CloudFoundryClient` provides direct access to the raw REST APIs.  This level of abstraction provides the most detailed and powerful access to the Cloud Foundry instance, but also requires users to perform quite a lot of orchestration on their own.  Most users will instead want to work at the `CloudFoundryOperations` layer.  Once again this is only an interface and the default implementation of this is the `DefaultCloudFoundryOperations`.  To instantiate one, you configure it with a builder:

```java
new CloudFoundryOperationsBuilder()
    .cloudFoundryClient(cloudFoundryClient)
    .target("example-organization", "example-space")
    .build();
```

In Spring-based applications, you'll want to encapsulate this in a bean definition as well:

```java
@Bean
CloudFoundryOperations cloudFoundryOperations(CloudFoundryClient cloudFoundryClient,
                                              @Value("${cf.organization}") String organization,
                                              @Value("${cf.space}") String space) {
    return new CloudFoundryOperationsBuilder()
            .cloudFoundryClient(cloudFoundryClient)
            .target(organization, space)
            .build();
}
```

### `CloudFoundryOperations` APIs

Once you've got a reference to the `CloudFoundryOperations`, it's time to start making calls to the Cloud Foundry instance.  One of the simplest possible operations is list all of the organizations the user is a member of.  The following example does three things:

1. Requests a list of all organizations
1. Extracts the name of each organization
1. Prints the name of the each organization to `System.out`

```java
cloudFoundryOperations.organizations()
    .list()
    .map(Organization::getName)
    .consume(System.out::println);
```

To relate the example to the description above the following happens:

1. `.list()` – Lists the Cloud Foundry organizations as a `Flux` of elements of type `Organization`.
1. `.map(...)` – Maps each organization to its name (type `String`).  This example uses a method reference; the equivalent lambda would look like `organization -> organization.getName()`.
1. `consume...` – The terminal operation that consumes each name in the `Flux`.  Again, this example uses a method reference and the equivalent lambda would look like `name -> System.out.println(name)`.

### `CloudFoundryClient` APIs

As mentioned earlier, the `cloudfoundry-operations` implementation builds upon the `cloudfoundry-client` API.  That implementation takes advantage of the same reactive style in the lower-level API.  The implementation of the `Organizations.list()` method (which was demonstrated above) looks like the following (roughly):

```java
cloudFoundryClient.organizations()
    .list(ListOrganizationsRequest.builder()
        .page(1)
        .build())
    .flatMap(response -> Flux.fromIterable(response.getResources))
    .map(resource -> Organization.builder()
        .id(resource.getMetadata().getId())
        .name(resource.getEntity().getName())
        .build());
```

The above example is more complicated:

1. `.list(...)` – Retrieves a page of Cloud Foundry organizations.
1. `.flatMap(...)` – Substitutes the original `Mono` with a `Flux` of the `Resource`s returned by the requested page.
1. `.map(...)` – Maps the `Resource` to an `Organization` type.

### Maven Plugin

TODO: Document once implemented

### Gradle Plugin

TODO: Document once implemented

## Documentation
API Documentation for each module can be found at the following locations:

* `cloudfoundry-client` – [`release`](http://cloudfoundry.github.io/cf-java-client/api/latest-release/cloudfoundry-client), [`milestone`](http://cloudfoundry.github.io/cf-java-client/api/latest-milestone/cloudfoundry-client), [`snapshot`](http://cloudfoundry.github.io/cf-java-client/api/latest-snapshot/cloudfoundry-client)
* `cloudfoundry-client-spring` – [`release`](http://cloudfoundry.github.io/cf-java-client/api/latest-release/cloudfoundry-client-spring), [`milestone`](http://cloudfoundry.github.io/cf-java-client/api/latest-milestone/cloudfoundry-client-spring), [`snapshot`](http://cloudfoundry.github.io/cf-java-client/api/latest-snapshot/cloudfoundry-client-spring)
* `cloudfoundry-operations` – [`release`](http://cloudfoundry.github.io/cf-java-client/api/latest-release/cloudfoundry-operations), [`milestone`](http://cloudfoundry.github.io/cf-java-client/api/latest-milestone/cloudfoundry-operations), [`snapshot`](http://cloudfoundry.github.io/cf-java-client/api/latest-snapshot/cloudfoundry-operations)
* `cloudfoundry-util` – [`release`](http://cloudfoundry.github.io/cf-java-client/api/latest-release/cloudfoundry-util), [`milestone`](http://cloudfoundry.github.io/cf-java-client/api/latest-milestone/cloudfoundry-util), [`snapshot`](http://cloudfoundry.github.io/cf-java-client/api/latest-snapshot/cloudfoundry-util)

## Development
The project depends on Java 8.  To build from source and install to your local Maven cache, run the following:

```shell
$ ./mvnw clean install
```

To run the integration tests, run the following:

```
$ ./mvnw -Pintegration-test clean test
```

**IMPORTANT**
Integration tests should be run against an empty Cloud Foundry instance. The integration tests are destructive, nearly everything on an instance given the chance.

The integration tests require a running instance of Cloud Foundry to test against.  We recommend using [PCF Dev][i] to start a local instance to test with.  To configure the integration tests with the appropriate connection information use the following environment variables:

Name | Description
---- | -----------
`TEST_HOST` | The host of Cloud Foundry instance.  Typically something like `api.local.pcfdev.io`.
`TEST_PASSWORD` | The test user's password
`TEST_PROTECTED_DOMAIN` | A domain that will not be cleaned up
`TEST_PROTECTED_FEATUREFLAGS` | A list of feature flags that will not be (re)set to standard values on cleanup
`TEST_PROTECTED_ORGANIZATION` | An organization who's contents will not be cleaned up
`TEST_SKIPSSLVALIDATION` | Whether to skip SSL validation when connecting to the Cloud Foundry instance.  Typically `true` when connecting to a PCF Dev instance.
`TEST_USERNAME` | The test user's username

## Contributing
[Pull requests][u] and [Issues][e] are welcome.

## License
This project is released under version 2.0 of the [Apache License][l].

[a]: https://apidocs.cloudfoundry.org/latest-release/
[c]: https://github.com/cloudfoundry/cli
[e]: https://github.com/cloudfoundry/cf-java-client/issues
[g]: https://gradle.org
[i]: https://github.com/pivotal-cf/micropcf
[l]: https://www.apache.org/licenses/LICENSE-2.0
[m]: https://maven.apache.org
[p]: https://projectreactor.io
[r]: http://reactivex.io
[t]: https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/client/RestTemplate.html
[u]: https://help.github.com/articles/using-pull-requests
