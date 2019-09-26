**ProvidesFix is now disabled by default, but you may choose to re-enable it for
your own BugCheckers with `-Xep:ProvidesFix:{WARN,ERROR}`.**

The [`providesFix`][annotation] field of `@BugPattern` was intended to keep
track of whether Error Prone bug checkers suggest a fix to the bug, or just
point out the presence of an antipattern. The default value is `UNSPECIFIED`,
and there are two meaningful values: `NO_FIX` for when we do not supply a
refactoring, and `REQUIRES_HUMAN_ATTENTION`[^1] for when we do.

ProvidesFixChecker helps keep these values updated. It verifies that bug
checkers do not return a description that includes a fix without setting
`providesFix = REQUIRES_HUMAN_ATTENTION`.

Note: this checker will have false negatives -- we cannot detect the *absence*
of a fix. So please keep this field updated to the best of your knowledge :)

[annotation]: https://github.com/google/error-prone/blob/4d7b19f3cb7b46a931856b0c3ca8b1f37c57508f/annotation/src/main/java/com/google/errorprone/BugPattern.java#L129-L135

[^1]: Theoretically, one day, we could have a tag that indicates we are 100%
    confident that a fix can be applied without review... but for now, we do
    not make any such claims, so we ask that users look at and test the fixes
    we suggest before submitting them.
