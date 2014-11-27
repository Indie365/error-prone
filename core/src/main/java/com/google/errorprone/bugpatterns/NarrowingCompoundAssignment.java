/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.MaturityLevel.MATURE;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompoundAssignmentTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;

import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;

import java.util.EnumMap;

import javax.lang.model.type.TypeKind;

/**
 * @author cushon@google.com (Liam Miller-Cushon)
 */
@BugPattern(name = "NarrowingCompoundAssignment", altNames = "finally",
    summary = "Compound assignments to bytes, shorts, chars, and floats hide dangerous casts",
    explanation = "The compound assignment E1 op= E2 could be mistaken for being equivalent to "
        + " E1 = E1 op E2. However, this is not the case: compound "
        + " assignment operators automatically cast the result of the computation"
        + " to the type on the left hand side. So E1 op= E2 is actually equivalent to"
        + " E1 = (T) (E1 op E2), where T is the type of E1. If the type of the expression is"
        + " wider than the type of the variable (i.e. the variable is a byte, char, short, or"
        + " float), then the compound assignment will perform a narrowing"
        + " primitive conversion. Attempting to perform the equivalent simple assignment"
        + " would generate a compilation error.\n\n"
        + " For example, 'byte b = 0; b = b << 1;' does not compile, but 'byte b = 0; b <<= 1;'"
        + " does!\n\n"
        + " (See Puzzle #9 in 'Java Puzzlers: Traps, Pitfalls, and Corner Cases'.)",
    category = JDK, severity = ERROR, maturity = MATURE)
public class NarrowingCompoundAssignment extends BugChecker
    implements CompoundAssignmentTreeMatcher {

  // The set of types susceptible to the implicit narrowing bug, and their string names.
  private static final EnumMap<TypeKind, String> DEFICIENT_TYPES = new EnumMap<>(TypeKind.class);
  static {
    DEFICIENT_TYPES.put(TypeKind.BYTE, "byte");
    DEFICIENT_TYPES.put(TypeKind.SHORT, "short");
    DEFICIENT_TYPES.put(TypeKind.CHAR, "char");
    DEFICIENT_TYPES.put(TypeKind.FLOAT, "float");
  }

  static String op2string(Tree.Kind kind) {
    switch (kind) {
      case MULTIPLY_ASSIGNMENT:
        return "*";
      case DIVIDE_ASSIGNMENT:
        return "/";
      case REMAINDER_ASSIGNMENT:
        return "%";
      case PLUS_ASSIGNMENT:
        return "+";
      case MINUS_ASSIGNMENT:
        return "-";
      case LEFT_SHIFT_ASSIGNMENT:
        return "<<";
      case RIGHT_SHIFT_ASSIGNMENT:
        return ">>";
      case UNSIGNED_RIGHT_SHIFT_ASSIGNMENT:
        return ">>>";
    }
    throw new IllegalArgumentException();
  }

  @Override
  public Description matchCompoundAssignment(CompoundAssignmentTree tree, VisitorState state) {
    String deficient = DEFICIENT_TYPES.get(type(tree.getVariable()).getKind());
    if (deficient == null) {
      return Description.NO_MATCH;
    }
    if (state.getTypes().isConvertible(type(tree.getExpression()), type(tree.getVariable()))) {
      return Description.NO_MATCH;
    }

    CharSequence var = state.getSourceForNode((JCTree) tree.getVariable());
    CharSequence expr = state.getSourceForNode((JCTree) tree.getExpression());
    if (var == null || expr == null) {
      return Description.NO_MATCH;
    }
    String op = op2string(tree.getKind());
    // e.g. 's *= 42' -> 's = (short) (s * 42)'
    String replacement = String.format("%s = (%s) (%s %s %s)", var, deficient, var, op, expr);
    return describeMatch(tree, SuggestedFix.replace(tree, replacement));
  }

  static Type type(Tree tree) {
    return ((JCTree) tree).type;
  }
}
