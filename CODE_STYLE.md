This document contains code style decisions which may be treated as opinion based, and can't
be caught by static code analyzers.
To avoid long PR discussions we'll document it here.
This document will be updated on demand, when we get new opinion based issue about coding style.

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
