/*
 * Copyright 2013 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.bugpatterns.inject.guice;

import static com.google.errorprone.BugPattern.Category.GUICE;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.ChildMultiMatcher.MatchType.AT_LEAST_ONE;
import static com.google.errorprone.matchers.InjectMatchers.ASSISTED_ANNOTATION;
import static com.google.errorprone.matchers.InjectMatchers.ASSISTED_INJECT_ANNOTATION;
import static com.google.errorprone.matchers.InjectMatchers.GUICE_SCOPE_ANNOTATION;
import static com.google.errorprone.matchers.InjectMatchers.JAVAX_SCOPE_ANNOTATION;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.annotations;
import static com.google.errorprone.matchers.Matchers.constructor;
import static com.google.errorprone.matchers.Matchers.hasAnnotation;
import static com.google.errorprone.matchers.Matchers.methodHasParameters;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.InjectMatchers;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.matchers.MultiMatcher;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;

/**
 * This checker matches iff *both* of the following conditions are true: 1) The class is assisted:
 * a) If there is a constructor that is annotated with @Inject and that constructor has at least one
 * parameter that is annotated with @Assisted. b) If there is no @Inject constructor and at least
 * one constructor is annotated with {@code @AssistedInject}. 2) There is an annotation on the
 * class, and the annotation is itself annotated with {@code @ScopeAnnotation}.
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@BugPattern(
  name = "GuiceAssistedInjectScoping",
  summary = "Scope annotation on implementation class of AssistedInject factory is not allowed",
  explanation =
      "Classes that AssistedInject factories create may not be annotated with scope "
          + "annotations, such as @Singleton.  This will cause a Guice error at runtime.\n\n"
          + "See [https://code.google.com/p/google-guice/issues/detail?id=742 this bug report] for "
          + "details.",
  category = GUICE,
  severity = ERROR
)
public class AssistedInjectScoping extends BugChecker implements ClassTreeMatcher {

  /**
   * Matches classes that have an annotation that itself is annotated with @ScopeAnnotation.
   */
  private static final MultiMatcher<ClassTree, AnnotationTree> classAnnotationMatcher =
      annotations(
          AT_LEAST_ONE,
          Matchers.<AnnotationTree>anyOf(
              hasAnnotation(GUICE_SCOPE_ANNOTATION), hasAnnotation(JAVAX_SCOPE_ANNOTATION)));

  /** Matches if any constructor of a class is annotated with an @Inject annotation. */
  private static final MultiMatcher<ClassTree, MethodTree> constructorWithInjectMatcher =
      constructor(AT_LEAST_ONE, InjectMatchers.<MethodTree>hasInjectAnnotation());

  /**
   * Matches if: 1) If there is a constructor that is annotated with @Inject and that constructor
   * has at least one parameter that is annotated with @Assisted. 2) If there is no @Inject
   * constructor and at least one constructor is annotated with @AssistedInject.
   */
  private static final Matcher<ClassTree> assistedMatcher =
      new Matcher<ClassTree>() {
        @Override
        public boolean matches(ClassTree classTree, VisitorState state) {
          if (constructorWithInjectMatcher.matches(classTree, state)) {
            // Check constructor with @Inject annotation for parameter with @Assisted annotation.
            return methodHasParameters(
                    AT_LEAST_ONE, Matchers.<VariableTree>hasAnnotation(ASSISTED_ANNOTATION))
                .matches(constructorWithInjectMatcher.getMatchingNodes().get(0), state);
          }

          return constructor(
                  AT_LEAST_ONE, Matchers.<MethodTree>hasAnnotation(ASSISTED_INJECT_ANNOTATION))
              .matches(classTree, state);
        }
      };
  public static final Matcher<ClassTree> MATCHER = allOf(classAnnotationMatcher, assistedMatcher);

  @Override
  public final Description matchClass(ClassTree classTree, VisitorState state) {
    if (!MATCHER.matches(classTree, state)) {
      return Description.NO_MATCH;
    }

    AnnotationTree annotationWithScopeAnnotation = classAnnotationMatcher.getMatchingNodes().get(0);
    if (annotationWithScopeAnnotation == null) {
      throw new IllegalStateException(
          "Expected to find an annotation that was annotated with @ScopeAnnotation");
    }

    return describeMatch(
        annotationWithScopeAnnotation, SuggestedFix.delete(annotationWithScopeAnnotation));
  }
}
