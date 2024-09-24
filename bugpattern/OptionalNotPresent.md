---
title: OptionalNotPresent
summary: This Optional has been confirmed to be empty at this point, so the call to
  `get()` or `orElseThrow()` will always throw.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Calling `get()` on an `Optional` that is not present will result in a
`NoSuchElementException`.

This check detects cases where `get()` is called when the optional is definitely
not present, e.g.:

```java
if (!o.isPresent()) {
  return o.get(); // this will throw a NoSuchElementException
}
```

```java
if (o.isEmpty()) {
  return o.get(); // this will throw a NoSuchElementException
}
```

These cases are almost definitely bugs; the intent may have been to invert the
test:

```java
if (o.isPresent()) {
  return o.get();
}
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("OptionalNotPresent")` to the enclosing element.
