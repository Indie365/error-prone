---
title: Installation
layout: documentation
---

Our goal is to make it simple to add Error Prone checks to your existing Java
compilation. Please note that Error Prone must be run on JDK 8 or newer. It can
be used to build Java 6 or 7 code by setting the appropriate `-source` /
`-target` / `-bootclasspath` flags.

Please join our
[mailing list](http://groups.google.com/group/error-prone-announce) to know when
a new version is released!

## Bazel

Error Prone works out of the box with [Bazel](http://bazel.io).

```
java_library(
    name = "hello",
    srcs = ["Hello.java"],
)
```

```
$ bazel build :hello
ERROR: example/myproject/BUILD:29:1: Java compilation in rule '//example/myproject:hello'
examples/maven/error_prone_should_flag/src/main/java/Main.java:20: error: [DeadException] Exception created but not thrown
    new Exception();
    ^
    (see http://errorprone.info/bugpattern/DeadException)
  Did you mean 'throw new Exception();'?
1 error
BazelJavaBuilder threw exception: java compilation returned status ERROR
INFO: Elapsed time: 1.989s, Critical Path: 1.69s
```

## Maven

Edit your `pom.xml` file to add settings to the maven-compiler-plugin:

```xml
  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.8.0</version>
        <configuration>
          <source>8</source>
          <target>8</target>
          <compilerArgs>
            <arg>-XDcompilePolicy=simple</arg>
            <arg>-Xplugin:ErrorProne</arg>
          </compilerArgs>
          <annotationProcessorPaths>
            <path>
              <groupId>com.google.errorprone</groupId>
              <artifactId>error_prone_core</artifactId>
              <version>2.3.4</version>
            </path>
          </annotationProcessorPaths>
        </configuration>
      </plugin>
    </plugins>
  </build>
```

Using the `error-prone-javac` is required when running on JDK 8, but using the
`-J-Xbootclasspath/p:` flag to override the system javac is not supported on JDK
9 and up. To support building with JDK 8, use the following profile for JDK 8
support:

```xml
  <properties>
    <javac.version>9+181-r4173-1</javac.version>
  </properties>

  <!-- using github.com/google/error-prone-javac is required when running on JDK 8 -->
  <profiles>
    <profile>
      <id>jdk8</id>
      <activation>
        <jdk>1.8</jdk>
      </activation>
      <build>
        <plugins>
          <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
              <fork>true</fork>
              <compilerArgs combine.children="append">
                <arg>-J-Xbootclasspath/p:${settings.localRepository}/com/google/errorprone/javac/${javac.version}/javac-${javac.version}.jar</arg>
              </compilerArgs>
            </configuration>
          </plugin>
        </plugins>
      </build>
    </profile>
  </profiles>
```

See the
[examples/maven](https://github.com/google/error-prone/tree/master/examples/maven)
directory for a working example:

```
../examples/maven/error_prone_should_flag$ mvn compile
[INFO] Compiling 1 source file to .../examples/maven/target/classes
.../examples/maven/src/main/java/Main.java:20: error: [DeadException] Exception created but not thrown
    new Exception();
    ^
    (see http://errorprone.info/bugpattern/DeadException)
  Did you mean 'throw new Exception();'?
1 error
[INFO] ------------------------------------------------------------------------
[INFO] BUILD FAILURE
[INFO] ------------------------------------------------------------------------
```

See the [flags documentation](http://errorprone.info/docs/flags#maven) for details on
how to customize the plugin's behavior.

## Gradle

The gradle plugin is an external contribution. The documentation and code is at
[tbroyer/gradle-errorprone-plugin](https://github.com/tbroyer/gradle-errorprone-plugin).

## Ant

Download the following artifacts from maven:

*   [error_prone_core-2.3.4-with-dependencies.jar](https://repo1.maven.org/maven2/com/google/errorprone/error_prone_core/2.3.4/error_prone_core-2.3.4-with-dependencies.jar)
*   [jFormatString-3.0.0.jar](https://repo1.maven.org/maven2/com/google/code/findbugs/jFormatString/3.0.0/jFormatString-3.0.0.jar)
*   [dataflow-2.5.7.jar](https://repo1.maven.org/maven2/org/checkerframework/dataflow/2.5.7/dataflow-2.5.7.jar)
*   [javacutil-2.5.7.jar](https://repo1.maven.org/maven2/org/checkerframework/javacutil/2.5.7/javacutil-2.5.7.jar)
*   [javac-9+181-r4173-1.jar](https://repo1.maven.org/maven2/com/google/errorprone/javac/9+181-r4173-1/javac-9+181-r4173-1.jar)

and add the following javac task to your project's `build.xml` file:

```xml
   <property name="javac.jar" location="${user.home}/.m2/repository/com/google/errorprone/javac/9+181-r4173-1/javac-9+181-r4173-1.jar"/>

    <!-- using github.com/google/error-prone-javac is required when running on JDK 8 -->
    <condition property="jdk9orlater">
      <javaversion atleast="9"/>
    </condition>

    <path id="processorpath.ref">
      <pathelement location="${user.home}/.m2/repository/com/google/errorprone/error_prone_core/2.3.4/error_prone_core-2.3.4-with-dependencies.jar"/>
      <pathelement location="${user.home}/.m2/repository/com/google/code/findbugs/jFormatString/3.0.0/jFormatString-3.0.0.jar"/>
      <pathelement location="${user.home}/.m2/repository/org/checkerframework/dataflow/2.5.7/dataflow-2.5.7.jar"/>
      <pathelement location="${user.home}/.m2/repository/org/checkerframework/javacutil/2.5.7/javacutil-2.5.7.jar"/>
      <!-- Add annotation processors and Error Prone custom checks here if needed -->
    </path>

    <javac srcdir="src" destdir="build" fork="yes" includeantruntime="no">
      <compilerarg value="-J-Xbootclasspath/p:${javac.jar}" unless:set="jdk9orlater"/>
      <compilerarg value="-XDcompilePolicy=simple"/>
      <compilerarg value="-processorpath"/>
      <compilerarg pathref="processorpath.ref"/>
      <compilerarg value="-Xplugin:ErrorProne -Xep:DeadException:ERROR" />
    </javac>
  </target>
```

See
[examples/ant](https://github.com/google/error-prone/tree/master/examples/ant)
for a working example.

```
examples/ant$ ant
Buildfile: .../examples/ant/build.xml

compile:
    [javac] Compiling 1 source file to .../examples/ant/build
    [javac] .../examples/ant/compilerclasspath/src/Main.java:20: error: [DeadException] Exception created but not thrown
    [javac]       new IllegalArgumentException("Missing required argument");
    [javac]       ^
    [javac]     (see http://errorprone.info/bugpattern/DeadException)
    [javac]   Did you mean 'throw new IllegalArgumentException("Missing required argument");'?
    [javac] 1 error

BUILD FAILED
```

## IntelliJ IDEA

To add the plugin, start the IDE and find the Plugins dialog. Browse
Repositories, choose Category: Build, and find the Error-prone plugin.
Right-click and choose "Download and install". The IDE will restart after you've
exited these dialogs.

To enable Error Prone, choose `Settings | Compiler | Java Compiler | Use
compiler: Javac with error-prone` and also make sure `Settings | Compiler | Use
external build` is NOT selected.

## Eclipse

Ideally, you should find out about failed Error Prone checks as you code in
eclipse, thanks to the continuous compilation by ECJ (eclipse compiler for
Java). But this is an architectural challenge, as Error Prone currently relies
heavily on the `com.sun.*` APIs for accessing the AST and symbol table.

For now, Eclipse users should use the Findbugs eclipse plugin instead, as it
catches many of the same issues.

## Command Line

### Java 9, 10, and 11

Error Prone supports the
[`com.sun.source.util.Plugin`](https://docs.oracle.com/javase/8/docs/jdk/api/javac/tree/com/sun/source/util/Plugin.html)
API, and can be used with JDK 9, 10 and 11 by adding Error Prone to the
`-processorpath` and setting the `-Xplugin` flag.

Example:

```bash
wget https://repo1.maven.org/maven2/com/google/errorprone/error_prone_core/2.3.4/error_prone_core-2.3.4-with-dependencies.jar
wget https://repo1.maven.org/maven2/org/checkerframework/dataflow/2.5.7/dataflow-2.5.7.jar
wget https://repo1.maven.org/maven2/org/checkerframework/javacutil/2.5.7/javacutil-2.5.7.jar
wget https://repo1.maven.org/maven2/com/google/code/findbugs/jFormatString/3.0.0/jFormatString-3.0.0.jar
javac \
  -XDcompilePolicy=simple \
  -processorpath error_prone_core-2.3.4-with-dependencies.jar:dataflow-2.5.7.jar:javacutil-2.5.7.jar:jFormatString-3.0.0.jar \
  '-Xplugin:ErrorProne -XepDisableAllChecks -Xep:CollectionIncompatibleType:ERROR' \
  ShortSet.java
```

```
ShortSet.java:8: error: [CollectionIncompatibleType] Argument 'i - 1' should not be passed to this method; its type int is not compatible with its collection's type argument Short
      s.remove(i - 1);
              ^
    (see http://errorprone.info/bugpattern/CollectionIncompatibleType)
1 error
```

### Java 8

To use Error Prone from the command line as a javac replacement:

```
wget https://repo1.maven.org/maven2/com/google/errorprone/error_prone_core/2.3.4/error_prone_core-2.3.4-with-dependencies.jar
wget https://repo1.maven.org/maven2/org/checkerframework/dataflow/2.5.7/dataflow-2.5.7.jar
wget https://repo1.maven.org/maven2/org/checkerframework/javacutil/2.5.7/javacutil-2.5.7.jar
wget https://repo1.maven.org/maven2/com/google/code/findbugs/jFormatString/3.0.0/jFormatString-3.0.0.jar
wget https://repo1.maven.org/maven2/com/google/errorprone/javac/9+181-r4173-1/javac-9+181-r4173-1.jar
javac \
  -J-Xbootclasspath/p:javac-9+181-r4173-1.jar \
  -XDcompilePolicy=simple \
  -processorpath error_prone_core-2.3.4-with-dependencies.jar:dataflow-2.5.7.jar:javacutil-2.5.7.jar:jFormatString-3.0.0.jar \
  '-Xplugin:ErrorProne -XepDisableAllChecks -Xep:CollectionIncompatibleType:ERROR' \
  ShortSet.java
```

## My build system isn't listed here

If you're an end-user of the build system, you can
[file a bug to request integration](https://github.com/google/error-prone/issues).

If you develop a build system, you should create an integration for your users!
Here are some basics to get you started:

Error-prone is implemented as a compiler hook, using an internal mechanism in
javac. To install our hook, we override the `main()` method in
`com.sun.tools.javac.main.Main`.

Find the spot in your build system where javac's main method is called. This is
assuming you call javac in-process, rather than shell'ing out to the javac
executable on the machine (which would be pretty lame since it's hard to know
where that's located).

First, add Error Prone's core library to the right classpath. It will need to be
visible to the classloader which currently locates the javac Main class. Then
replace the call of `javac.main.Main.main()` with the Error Prone compiler:

`return new ErrorProneCompiler.Builder().build().compile(args) == 0`

## Using ErrorProne in concert with other Annotation Processors

All of the above instructions use the `javac` option `-processorpath` which has
side-effect of causing `javac` to no longer scan the compile classpath for
annotation processors. If you are using other annotation processors in addition
to ErrorProne, such as Lombok, then you will need to add their JARs to your
`-processorpath` argument. The mechanics of this will vary according to the build
tool you are using.
