---
title: InlineMe
layout: documentation
---

`@InlineMe` is a deprecation mechanism that allows automated tools to cleanup
existing callers. This works similar to Kotlin's
[`ReplaceWith`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-replace-with/)
mechanism (more information
[here](https://readyset.build/kotlin-deprecation-goodies-a35a397aa9b5)).

## Background

Many deprecated APIs are implemented simply as calls to other, non-deprecated
public APIs. Pushing the implementation of a deprecated method "up into" the
caller is a process known as _inlining_. `@InlineMe` provides an easy to use
mechanism to automatically inline deprecated APIs. For example:

```java
import com.google.errorprone.annotations.InlineMe;

public final class MyClass {
  /** @deprecated Use {@link #setDeadline(Duration)} instead. */
  @Deprecated
  @InlineMe(
      replacement = "this.setDeadline(Duration.ofMillis(deadlineMs))",
      imports = {"java.time.Duration"})
  public void setDeadline(long deadlineMs) {
    setDeadline(Duration.ofMillis(deadlineMs));
  }
  public void setDeadline(Duration deadline) {
    this.deadline = checkNotNull(deadline);
  }
}
```

Users who call the method tagged with `@InlineMe` (e.g.,
`myClass.setDeadline(3000)`) will be migrated to the new method:
`myClass.setDeadline(Duration.ofMillis(3000))`

## `-parameters` flag is required for use!

InlineMe requires the use of the `-parameters` flag because it relies on
parameter name information at runtime --- see
[JEP 118](https://openjdk.org/jeps/118).

Maven users should add the `<parameters>true</parameters>` option to their
`maven-compiler-plugin`.

Gradle
[does not currently enable this flag by default](https://github.com/gradle/gradle/issues/14781),
so users should add the following to their `build.gradle`:

```
tasks.withType(JavaCompile) {
    options.compilerArgs << '-parameters'
}
```

<!-- TODO(kak): add instructions on how to run the Inliner in patch mode -->

## Example Usage

`@InlineMe` supports a handful of API inlinings: non-overridable instance
methods, static methods, and constructors.

All replacement strings must have the API access qualified. That is, if you are
inlining a constructor or instance method, you *must* qualify the replacement
code with `this`. If you are inlining a static method, you *must* qualify the
replacement code with the `EnclosingClassName`.

Both static imports and non-static imports are supported in the replacement
code.

See the usage examples below:

### Instance method with no new imports

```java
import com.google.errorprone.annotations.InlineMe;
import java.time.Duration;

public final class MyClass {
  public Duration getDeadline() {
    return deadline;
  }

  @Deprecated
  @InlineMe(replacement = "this.getDeadline().toMillis()")
  public long getDeadlineMillis() {
    return getDeadline().toMillis();
  }
}
```

### Instance method with new imports

```java
import com.google.errorprone.annotations.InlineMe;
import java.time.Duration;

public final class MyClass {
  public void setDeadline(Duration deadline) {
    this.deadline = deadline;
  }

  @Deprecated
  @InlineMe(
      replacement = "this.setDeadline(Duration.ofMillis(millis))",
      imports = {"java.time.Duration"})
  public void setDeadline(long millis) {
    return setDeadline(Duration.ofMillis(millis));
  }
}
```

### Instance method with new static imports

```java
import static com.google.thirdparty.jodatime.JavaTimeConversions.toJodaDuration;
import com.google.errorprone.annotations.InlineMe;
import java.time.Duration;

public final class MyClass {
  public Duration getDeadline() {
    return deadline;
  }

  @Deprecated
  @InlineMe(
      replacement = "toJodaDuration(this.getDeadline())",
      staticImports = {"com.google.thirdparty.jodatime.JavaTimeConversions.toJodaDuration"})
  public org.joda.time.Duration getDeadlineJoda() {
    return toJodaDuration(getDeadline());
  }
}
```

### Static method with no new imports

NOTE: you should always add the enclosing class as an `import` on the
`@InlineMe` annotation because the caller _may_ have static imported the old
method (and thus won't have an import for the replacement).

```java
import com.google.errorprone.annotations.InlineMe;

public final class Frobber {
  public static Frobber fromName(String name) {
    return new Frobber(name);
  }

  @Deprecated
  @InlineMe(
      replacement = "Frobber.fromName(name)",
      imports = {"com.google.frobber.Frobber"})
  public static Frobber create(String name) {
    return fromName(name);
  }
}
```

### Static method with new static imports

NOTE: you should always add the enclosing class as an `import` on the
`@InlineMe` annotation because the caller _may_ have static imported the old
method (and thus won't have an import for the replacement).

```java
import static com.google.thirdparty.jodatime.JavaTimeConversions.asTimeSource;
import com.google.common.time.Clock;
import com.google.common.time.TimeSource;
import com.google.errorprone.annotations.InlineMe;

public final class MyClass {
  public static void setTimeSource(TimeSource timeSource) {
    MyClass.timeSource = timeSource;
  }

  @Deprecated
  @InlineMe(
      replacement = "MyClass.setTimeSource(asTimeSource(clock))",
      imports = {"com.google.frobber.MyClass"},
      staticImports = {"com.google.thirdparty.jodatime.JavaTimeConversions.asTimeSource"})
  public static void setClock(Clock clock) {
    setTimeSource(asTimeSource(clock));
  }
}
```

### Constructor with new static imports

```java
import static com.google.thirdparty.jodatime.JavaTimeConversions.toJavaDuration;
import com.google.errorprone.annotations.InlineMe;
import java.time.Duration;

public final class MyClass {
  public MyClass(Duration deadline) {
    this.deadline = deadline;
  }

  @Deprecated
  @InlineMe(
      replacement = "this(toJavaDuration(jodaDeadline))",
      staticImports = {"com.google.thirdparty.jodatime.JavaTimeConversions.toJavaDuration"})
  public MyClass(org.joda.time.Duration jodaDeadline) {
    this(toJavaDuration(jodaDeadline));
  }
}
```

### Constructor to non-constructor

While it’s not possible to “inline” a constructor this way, InlineMe can still
help. The catch is that the InlineMe code will not match the implementation’s
code. You will need to manually disable the validator with
`@InlineMeValidationDisabled`.

```java
public final class MyClass {

  /** @deprecated Please use {@link #create} instead. */
  @InlineMeValidationDisabled("Migrating from constructor to factory method")
  @Deprecated
  @InlineMe(
      replacement = "MyClass.create()",
      imports = {"com.google.frobber.MyClass"})
  public MyClass() {
    ...
  }

  public static MyClass create() {
    return new MyClass();
  }
}
```
