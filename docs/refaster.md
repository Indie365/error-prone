---
title: Refaster templates
layout: documentation
---

In addition to [patching] your code using the checks built-in to Error Prone, we've developed a mechanism to refactor your code using before-and-after templates (we call them Refaster templates). Once you write these templates, you compile them into .refaster files, then use the Error Prone compiler to refactor your code according to those rules.

Refaster is described in more detail in a [research paper][lowasser-paper] presented by Louis Wasserman at the _Workshop for Refactoring Tools_.

## Building Refaster Templates

Explaining how to write refaster rules is best done by example:

```java
import com.google.errorprone.refaster.annotation.AfterTemplate;
import com.google.errorprone.refaster.annotation.AlsoNegation;
import com.google.errorprone.refaster.annotation.BeforeTemplate;

public class StringIsEmpty {
  @BeforeTemplate
  boolean equalsEmptyString(String s) {
  	return s.equals("");
  }

  @BeforeTemplate
  boolean lengthEquals0(String string) {
    return string.length() == 0;
  }

  @AfterTemplate
  @AlsoNegation
  boolean optimizedMethod(String string) {
    return string.isEmpty();
  }
}
```

Refaster templates are any class with multiple methods with the same return type and list of arguments. One of the methods should be annotated `@AfterTemplate`, and every other method should be annotated with `@BeforeTemplate`. With this template, any code calling `String#equals` passing in the empty string literal, or calling `String#length` and comparing it to 0 will be replaced by a call to `String#isEmpty`. Notably, no matter how the String expression is generated, Refaster will do the replacement:

```java
boolean b = someChained().methodCall().returningAString().length() == 0;
```

becomes

```java
boolean b = someChained().methodCall().returningAString().isEmpty();
```

while

```java
if (this.someStringField.equals(""))
```

becomes

```java 
if (this.someStringField.isEmpty())
```

There are other annotations in the [refaster.annotations] package that allow you to express more complex before-and-after refactorings, with examples about how to use them in their javadoc. See also the [advanced features](#advanced-features) section of this document.

In the above example, `@AlsoNegation` is used to signal that the rule can also match the logical negation of the `@BeforeTemplate` bodies (`string.length() != 0` becomes `!string.isEmpty()`);

## Running the Refaster refactoring

TIP: These instructions are valid as of the most recent snapshot at HEAD, and are subject to change

Use the error prone javac jar and the error_prone_refaster jar to compile the refaster template:

```shell
wget http://repo1.maven.org/maven2/com/google/errorprone/javac/9-dev-r3297-1/javac-9-dev-r3297-1.jar
wget https://oss.sonatype.org/content/repositories/snapshots/com/google/errorprone/error_prone_refaster/2.0.16-SNAPSHOT/error_prone_refaster-2.0.16-20170121.005350-7.jar

java -cp javac-9-dev-r3297-1.jar:error_prone_refaster-2.0.16-20170121.005350-7.jar \
  com.google.errorprone.refaster.RefasterRuleCompiler \
  StringIsEmpty.java --out `pwd`/myrule.refaster
 ```

 You should see a file named `myrule.refaster` in your current directory. To use this to refactor your code, add the following flags to the Error Prone compiler (this is similar to [patching]):

 ```
-XepPatchChecks:refaster:/full/path/to/myrule.refaster
-XepPatchLocation:/full/path/to/your/source/root
```

This will generate a unified diff file named `error-prone.patch` that you can apply similarly to how you would apply other patches:

```shell
cd /full/path/to/your/source/root
patch -p0 -u -i error-prone.patch
```

## Advanced features

### `Refaster.anyOf`

A particularly commonly used method is `Refaster.anyOf`, a "magic" method for
use in your `@BeforeTemplate` which indicates that any of the specified
expressions are allowed to match. For example:

```java
class AddAllArrayToBuilder<E> {
  @BeforeTemplate
  ImmutableCollection.Builder<E> addAllAsList(
      ImmutableCollection.Builder<E> builder, E[] elements) {
    return builder.addAll(Refaster.anyOf(
        Arrays.asList(elements),
        ImmutableList.copyOf(elements),
        Lists.newArrayList(elements)));
  }

  @AfterTemplate
  ImmutableCollection.Builder<E> addAll(
      ImmutableCollection.Builder<E> builder, E[] elements) {
    builder.add(elements);
  }
}
```

is equivalent to, but much shorter than,

```java
class AddAllArrayToBuilder {
  @BeforeTemplate
  ImmutableCollection.Builder<E> addAllArraysAsList(
      ImmutableCollection.Builder<E> builder, E[] elements) {
    return builder.addAll(Arrays.asList(elements));
  }

  @BeforeTemplate
  ImmutableCollection.Builder<E> addAllImmutableListCopyOf(
      ImmutableCollection.Builder<E> builder, E[] elements) {
    return builder.addAll(ImmutableList.copyOf(elements));
  }

  @BeforeTemplate
  ImmutableCollection.Builder<E> addAllNewArrayList(
      ImmutableCollection.Builder<E> builder, E[] elements) {
    return builder.addAll(Lists.newArrayList(elements));
  }

  @AfterTemplate
  ImmutableCollection.Builder<E> addAll(
      ImmutableCollection.Builder<E> builder, E[] elements) {
    builder.add(elements);
  }
}
```

### `Refaster.clazz()` and other methods

Consider the following refactoring, where calling X.class.cast(o) for any X is replaced with a simple cast to X.

```java
class ClassCast<T> {
  @BeforeTemplate
  T cast(Object o) {
    return Refaster.<T>clazz().cast(o);
  }

  @AfterTemplate
  T cast(Object o) {
    return (T) o;
  }
}
```

Here, `Refaster.<T>clazz()` is a "magic incantation" to substitute for the
impossible-to-compile code you would _want_ to write here, `T.class`. There are
a variety of these "magic incantations" in the [`Refaster` class][refaster-javadoc]
for code patterns that you might wish to write in a `@BeforeTemplate` but don't
technically compile as Java code.


### Placeholder methods

#### Expression placeholders

Placeholder methods are a particularly powerful feature of Refaster, allowing
you to match arbitrary chunks of code in terms of arguments, not just
expressions of a given type. Here is a basic example usage of a placeholder:

```java
abstract class ComputeIfAbsent<K, V> {
  /*
   * Represents an arbitrary expression in terms of an input, key.
   */
  @Placeholder
  abstract V function(K key);

  @BeforeTemplate
  void before(Map<K, V> map, K key) {
    if (!map.containsKey(key)) {
      map.put(key, function(key));
    }
  }

  @AfterTemplate
  void after(Map<K, V> map, K key) {
    map.computeIfAbsent(key, (K k) -> function(k));
  }
}
```

We annotate an `abstract` method in the Refaster template class to represent
"some function in terms of the specified input." By default, arguments to the
placeholder _must_ be used for the placeholder to match. For example, this
pattern would _not_ match

```java
if (!map.containsKey(k)) {
  map.put(k, 0);
}
```

because the expression `0` does not refer to `k`. To change this behavior, you
may annotate the arguments to the placeholder method with `@MayOptionallyUse`:

```java
@Placeholder
abstract V function(@MayOptionallyUse K key);
```

Note also that the code matched by the placeholder method _cannot_ refer to
variables in the `@BeforeTemplate` that are not explicitly passed in. So

```java
if (!map.containsKey(k)) {
  map.put(k, map.get(k - 1));
}
```

would _not_ be matched, because the expression in the `put` refers to `map`,
which is not passed into the placeholder method. The match _can_ refer to
variables that aren't explicitly mentioned in the Refaster pattern, e.g.

```java
if (!map.containsKey(k)) {
  map.put(k, k + ":" + suffixString);
}
```

because `suffixString` is not a variable in the `@BeforeTemplate`.

##### Matching the identity

By default, placeholder methods are not permitted to simply pass on one of their
arguments unchanged. This behavior can be overridden with the annotation:
`@Placeholder(allowsIdentity = true)`.

#### Block placeholders

The above example used placeholders to match single expressions in terms of
other expressions, but not multiple lines of code. These, too, are supported.
Consider the following refactoring:

```java
abstract class IfSetAdd<E> {
  @Placeholder
  abstract void doAfterAdd(E element);

  @BeforeTemplate
  void ifNotContainsThenAdd(Set<E> set, E elem) {
    if (!set.contains(elem)) {
      set.add(elem);
      doAfterAdd(elem);
    }
  }

  @AfterTemplate
  void ifAdd(Set<E> set, E elem) {
    if (set.add(elem)) {
      doAfterAdd(elem);
    }
  }
}
```

...which would e.g. rewrite

```java
if (!mySet.contains(e)) {
  mySet.add(e);
  log("added %s to set", e);
}
```

to

```java
if (mySet.add(e)) {
  log("added %s to set", e);
}
```

There is also some limited magic supported here with block versus expression
lambdas. For example, consider the refactoring

```java
abstract class MapEntryLoop<K, V> {
  @Placeholder
  abstract void doSomething(K k, V v);

  @BeforeTemplate
  void entrySetLoop(Map<K, V> map) {
    for (Map.Entry<K, V> entry : map.entrySet()) {
      doSomething(entry.getKey(), entry.getValue());
    }
  }

  @AfterTemplate
  void mapForEach(Map<K, V> map) {
    map.forEach((K key, V value) -> doSomething(key, value));
  }
}
```

would rewrite

```java
for (Map.Entry<String, Integer> e : map.entrySet()) {
  System.out.println(e.getKey() + ":" + e.getValue());
}
for (Map.Entry<String, Integer> e : map.entrySet()) {
  String str = e.getKey() + ":" + e.getValue();
  System.out.println(str);
}
```

to

```java
map.forEach(
    (String key, Integer value) ->
        System.out.println(key + ":" + value));
map.forEach(
    (String key, Integer value) -> { // multiple lines!
        String str = key + ":" + value;
        System.out.println(str);
    });
```

...that is, it will automatically bracket the lambda body, or not, as
appropriate to the placeholder body actually matched.


[patching]: patching
[refaster.annotations]: ../api/latest/com/google/errorprone/refaster/annotation/package-frame.html
[lowasser-paper]: https://research.google.com/pubs/archive/41876.pdf
[refaster-javadoc]: ../api/latest/com/google/errorprone/refaster/Refaster.html