/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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
import com.google.errorprone.BugPattern.Category;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.ChildMultiMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;

import static com.google.errorprone.BugPattern.MaturityLevel.MATURE;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.*;

@BugPattern(name = "JMockTestWithoutRunWithOrRuleAnnotation",
        summary = "JMock tests must have @RunWith class annotation or the mockery field declared as a JUnit rule",
        explanation = "If this is not done then all of your JMock tests will run and pass. However none of your assertions " +
                "will actually be evaluated so your tests will be producing false positive results.",
        category = Category.JMOCK, severity = ERROR, maturity = MATURE)
public class JMockTestWithoutRunWithOrRuleAnnotation extends BugChecker implements BugChecker.VariableTreeMatcher {

    private static final Matcher<VariableTree> JMOCK_MOCKERY_MATCHER = allOf(isSameType("org.jmock.Mockery"), isField());
    private static final DoesNotHaveARunnerOrDeclaredARuleMatcher DOES_NOT_HAVE_A_RUNNER_OR_DECLARED_A_RULE_MATCHER = new DoesNotHaveARunnerOrDeclaredARuleMatcher();
    private static final Matcher<VariableTree> BIG_DADDY_MATCHER = allOf(JMOCK_MOCKERY_MATCHER, DOES_NOT_HAVE_A_RUNNER_OR_DECLARED_A_RULE_MATCHER);

    @Override
    public Description matchVariable(VariableTree tree, VisitorState state) {
        if (BIG_DADDY_MATCHER.matches(tree, state)) {
            return describeMatch(tree);
        }
        return Description.NO_MATCH;
    }

    private static class DoesNotHaveARunnerOrDeclaredARuleMatcher implements Matcher<VariableTree> {

        private static final Matcher<VariableTree> variableWithRule = hasAnnotation("org.junit.Rule");
        private static final Matcher<Tree> classWithRunWith = enclosingClass(Matchers.<ClassTree>annotations(ChildMultiMatcher.MatchType.ALL, isType("org.junit.runner.RunWith")));
        private static final Matcher<Tree> classRunnerIsJMock = enclosingClass(Matchers.<ClassTree>annotations(ChildMultiMatcher.MatchType.ALL,
                hasArgumentWithValue("value", classLiteral(isSameType("org.jmock.integration.junit4.JMock")))));

        @Override
        public boolean matches(VariableTree variableTree, VisitorState state) {
            return (classWithRunWith.matches(variableTree, state) && !classRunnerIsJMock.matches(variableTree, state))
                    || !variableWithRule.matches(variableTree, state);
        }
    }
}
