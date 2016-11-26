/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.isSubtypeOf;
import static com.google.errorprone.matchers.Matchers.methodHasArity;
import static com.google.errorprone.matchers.Matchers.methodIsNamed;
import static com.google.errorprone.matchers.Matchers.methodReturns;
import static com.google.errorprone.suppliers.Suppliers.INT_TYPE;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Type;
import java.io.InputStream;
import javax.lang.model.element.ElementKind;

/** Checks that InputStreams should override int read(byte[], int, int); */
@BugPattern(
  name = "InputStreamSlowMultibyteRead",
  summary =
      "Please also override int read(byte[], int, int), otherwise multi-byte reads from this "
          + "input stream are likely to be slow.",
  category = JDK,
  severity = WARNING
)
public class InputStreamSlowMultibyteRead extends BugChecker implements ClassTreeMatcher {

  private static final Matcher<ClassTree> IS_INPUT_STREAM = isSubtypeOf(InputStream.class);

  private static final Matcher<MethodTree> READ_INT_METHOD =
      allOf(methodIsNamed("read"), methodReturns(INT_TYPE), methodHasArity(0));

  @Override
  public Description matchClass(ClassTree classTree, VisitorState state) {
    if (!IS_INPUT_STREAM.matches(classTree, state)) {
      return Description.NO_MATCH;
    }

    TypeSymbol thisClassSymbol = ASTHelpers.getSymbol(classTree);
    if (thisClassSymbol.getKind() != ElementKind.CLASS) {
      return Description.NO_MATCH;
    }

    // Find the method that overrides the single-byte read. It should also override the multibyte
    // read.
    MethodTree readByteMethod =
        classTree
            .getMembers()
            .stream()
            .filter(MethodTree.class::isInstance)
            .map(MethodTree.class::cast)
            .filter(m -> READ_INT_METHOD.matches(m, state))
            .findFirst()
            .orElse(null);

    if (readByteMethod == null) {
      return Description.NO_MATCH;
    }

    Type byteArrayType = state.getType(state.getSymtab().byteType, true, ImmutableList.of());
    Type intType = state.getSymtab().intType;
    MethodSymbol multiByteReadMethod =
        ASTHelpers.resolveExistingMethod(
            state,
            thisClassSymbol,
            state.getName("read"),
            ImmutableList.of(byteArrayType, intType, intType),
            ImmutableList.of());

    return multiByteReadMethod.owner.equals(thisClassSymbol)
        ? Description.NO_MATCH
        : describeMatch(readByteMethod);
  }
}
