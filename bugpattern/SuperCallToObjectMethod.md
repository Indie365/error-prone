---
title: SuperCallToObjectMethod
summary: '`super.equals(obj)` and `super.hashCode()` are often bugs when they call
  the methods defined in `java.lang.Object`'
layout: bugpattern
tags: LikelyError
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

_Alternate names: SuperEqualsIsObjectEquals_

## The problem
Implementations of `equals` and `hashCode` should usually not delegate to
`Object.equals` and `Object.hashCode`.

Those two methods implement equality based on object identity. That
implementation *is* sometimes what the author intended. (This check attempts to
identify those cases and *not* report a warning for them. But in some cases, it
still produces a warning when it shouldn't.)

But when `super.equals` and `super.hashCode` call the methods defined in
`Object`, the developer often did *not* intend to use object identity. Often,
developers write something like:

```java
private final int id;

@Override
public boolean equals(Object obj) {
  if (obj instanceof Foo) {
    return super.equals(obj) && id == ((Foo) obj).id;
  }
  return obj;
}

@Override
public int hashCode() {
  return super.hashCode() ^ id;
}
```

This appears to be an attempt to define equality in terms of the `id` field in
this class and any fields in the superclass. However, when the superclass that
defines `equals` or `hashCode` is `Object`, the code instead defines equality in
terms of a mix of object identity and field values. The result is equivalent to
defining it in terms of identity alone—which is equivalent to not overriding
`equals` and `hashCode` at all!

Typically, the code should be rewritten to remove the `super` calls entirely:

```java
private final int id;

@Override
public boolean equals(Object obj) {
  if (obj instanceof Foo) {
    return id == ((Foo) obj).id;
  }
  return obj;
}

@Override
public int hashCode() {
  return id;
}
```

Note that the suggested edits for this check instead preserve behavior, which
likely means preserving bugs! However, in cases in which object identity *is*
intended, we recommend applying the suggested edit to make that behavior
explicit in the code:

```java
// This class's definition of equality is unusual and perhaps not ideal.
// But it is at least explicit.

private final Integer id;

@Override
public boolean equals(Object obj) {
  if (obj instanceof Foo) {
    if (id == null) {
      return this == obj;
    }
    return id.equals(((Foo) obj).id);
  }
  return obj;
}

@Override
public int hashCode() {
  return id != null ? id : System.identityHashCode(this);
}
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("SuperCallToObjectMethod")` to the enclosing element.
