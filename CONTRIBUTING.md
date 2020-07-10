To contribute to Artipie project you need JDK-11 and Maven 3.2+.
Some integration tests requires Docker to be installed.


## How to contribute

Fork the repository, make changes, and send us a
[pull request](https://www.yegor256.com/2014/04/15/github-guidelines.html). We will review
your changes and apply them to the `master` branch shortly, provided
they don't violate our quality standards. To avoid frustration, before
sending us your pull request please run full Maven build:

```
mvn clean verify -Pqulice
```

After submitting pull-request check CI status checks. If any check with "required" label fails,
pull-request will not be merged.

## How to run it locally

To run Artipie server locally, build it with
```
mvn clean package -Passembly
```
and run with *(change port if needed)*:
```java
java -jar target/artipie-jar-with-dependencies.jar --config=example/artipie.yaml --port=8080
```
Example configuration uses `org` layout of Artipie with two level hierarchy,
user `test` with password `123`, and `default` storage in `./example/storage` direcotry.
To access the dashboard open `http://localhost/test` in your browser and enter user credentials.
