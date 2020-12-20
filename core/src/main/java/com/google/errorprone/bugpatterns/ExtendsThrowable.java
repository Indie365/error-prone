/*
 * Copyright 2020 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;

/** Makes sure that you are not extending {@link Throwable} directly. */
@BugPattern(
    name = "ExtendsThrowable",
    summary = "Not recommended extend Throwable directly.",
    severity = SeverityLevel.WARNING)
public final class ExtendsThrowable extends BugChecker implements ClassTreeMatcher {

  private static final String THROWABLE_CLASS = "java.lang.Throwable";

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    if (tree.getExtendsClause() == null) {
      // Doesn't extend anything
      return Description.NO_MATCH;
    }

    Tree extendsClause = tree.getExtendsClause();
    if (ASTHelpers.isSameType(
        ASTHelpers.getType(extendsClause), state.getTypeFromString(THROWABLE_CLASS), state)) {
      return describeMatch(extendsClause);
    }

    return Description.NO_MATCH;
  }
}
