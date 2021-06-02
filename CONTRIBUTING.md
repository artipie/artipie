To contribute to Artipie project you need JDK-11 and Maven 3.2+.
Some integration tests requires Docker to be installed.


## How to contribute

Fork the repository, make changes, and send us a
[pull request (PR)](#pull-request-style). We will review
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
user `test` with password `123`, and `default` storage in `./example/storage` directory.
To access the dashboard open `http://localhost/test` in your browser and enter user credentials.


### Testing

This is a build and test pipeline for artipie main assembly verification:
 1. Unit testing (can be run with `mvn test`): it runs all unit tests. The unit test should not depend on any external component. The testing framework is a Junit5, Maven plugin is Surefire.
 2. Packaging (`mvn package`) - copy all dependencies into `target/dependency` directory and produce `artipie.jav` file. Then create Docker image based on dependencies and jar file, docker reuses cached layers if dependencies didn't change. It uses `docker-build` Maven profile activated by default if `/var/run/docker.sock` file exists.
 3. Integration testing (`mvn verify`) - it runs all integration tests against actual docker image of artipie. Maven ensures that the image is up to date and can be accessed by `artipie/artipie:1.0-SNAPSHOT` tag. We use Junit5 as a test framework, Failsafe maven plugin and Testcontainers for running Dockers.
 4. Smoke tests (`examples/run.sh`) - start preconfigured Artipie Docker container, attach data volumes and connect test network, then run small Docker-based test scripts withing same network against Artipie server. The server could be accessed via `artipie.artipie:8080` address.
 5. Deploy (`mvn deploy`) - uploading Docker image to registry.

## Code style

Code style is enforced by "qulice" Maven plugin which aggregates multiple rules for "checkstyle" and "PMD".

There are some additional recommendation for code style which are not covered by automatic checks:

1. Prefer Hamcrest matchers objects instead of static methods in unit tests:
```java
// use
MatcherAssert.assertThat(target, new IsEquals<>(expected));

// don't use
MatcherAssert.assertThat(target, Matchers.isEquals(expected));
```

2. Avoid adding reason to assertions in unit tests with single assertion:
```java
// use
MatcherAssert.assertThat(target, matcher);

// don't use
MatcherAssert.assertThat("Some reason", target, matcher);
```


3. Add reason to assertions in unit tests with multiple assertion. Prefer single assertion styles for unit tests where possible:
```java
MatcherAssert.assertThat("Reasone one", target1, matcher1);
MatcherAssert.assertThat("Reason two", target2, matcher2);
```

## Pull request style

Primary PR rule: it's the responsibility of PR author to bring the changes to the master branch.

Other important mandatory rule - it should refer to some ticket. The only exception is a minor type fix in documentation.

Pull request should consist of two mandatory parts:
 - "Title" - says **what** is the change, it should be one small and full enough sentence with only necessary information
 - "Description" - says **how** this pull request fixes a problem or implements a new feature

### Title

Title should be as small as possible but provide full enough information to understand what was done (not a process),
and where from this sentence.
It could be capitalized If needed, started from capital letter, and should not include links or references
(including tickets numbers).

Good PR titles examples:
 - Fixed Maven artifact upload - describes what was done: fixed, the what was the fixed: artifact upload, and where: Maven
 - Implemented GET blobs API for Docker - done: implemented, what: GET blobs API, where: Docker
 - Added integration test for Maven deploy - done: added, what: integration test for deploy, where: Maven

Bad PR titles:
 - Fixed NPE - not clear WHAT was the problem, and where; good title could be: "Fixed NPE on Maven artifact download"
 - Added more tests - too vague; good: "Added unit tests for Foo and Bar classes"
 - Implementing Docker registry - the process, not the result; good: "Implemented cache layer for Docker proxy"

### Description

Description starts with a ticket number prefixed with one of these keywords: (Fixed, Closes, For, Part of),
then a hyphen, and a description of the changes.
Changes description provides information about **how** the problem from title was fixed.
It should be a short summary of all changes to increase readability of changes before looking to code,
and provide some context. The format is
`(<keyword>For|Closes|Fixes|Part of) #(<ticket>\d+) - (<details>.+)`,
e.g.: `For #123 - check if the file exists before accessing it and return 404 code if doesn't`.

Good description describes the solution provided and may have technical details, it isn't just a copy of the title.
Examples of good descriptions:
 - Added a new class as storage implementation over S3 blob-storage, implemented `value()` method, throw exceptions on other methods, created unit test for value
 - Fixed FileNotFoundException on reading blob content by checking if file exists before reading it. Return 404 code if doesn't exist

### Merging

We merge PR only if all required CI checks passed and after approval of repository maintainers.
We merge using squash merge, where commit messages consists of two parts:
```
<PR title>

<PR description>
PR: <PR number>
```
GitHub automatically inserts title and description as commit messages, the only manual work is a PR number.

### Review

It's recommended to request review from `@artipie/contributors` if possible.
When the reviewers starts the review it should assign the PR to themselves,
when the review is completed and some changes are requested, then it should be assigned back to the author.
On approve: if reviewer and repository maintainer are two different persons,
then the PR should be assigned to maintainer, and maintainer can merge it or ask for more comments. 

The workflow:
```
<required> (optional)
        PR created |   Review   | Request changes | Fixed changes | Approves changes | Merge |
assignee: <none>  -> <reviewer> ->    (author)    ->  (reviewer)  ->   <maintainer>  -> <none>
```

When addressing review changes, two possible strategies could be used:
 - `git commit --ammend` + `git push --force` - in case of changes are minor or obvious, both sides agree
 - new commit - in case if author wants to describe review changes and keep it for history,
 e.g. if author doesn't agree with reviewer or maintainer, he|she may want to point that this changes was
 asked by a reviewer. This commit is not going to the master branch, but it will be linked into PR history.

### Commit style

Commit styles are similar to PR, PR could be created from commit message: first line goes to the title,
other lines to description:
```
Commit title - same as PR title

For #123 - description of the commit goes
to PR description. It could be multiline `and` include
*markdown* formatting.
```
