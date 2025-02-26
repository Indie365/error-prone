/*
 * Copyright 2012 The Error Prone Authors.
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

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
@RunWith(JUnit4.class)
public class DeadExceptionTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(DeadException.class, getClass());

  @Test
  public void positiveCase() {
    compilationHelper
        .addSourceLines(
            "DeadExceptionPositiveCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            public class DeadExceptionPositiveCases {
              public void runtimeException() {
                // BUG: Diagnostic contains: throw new RuntimeException
                new RuntimeException("Not thrown, and reference lost");
              }

              public void error() {
                // BUG: Diagnostic contains: throw new AssertionError
                new AssertionError("Not thrown, and reference lost");
              }

              public void fixIsToDeleteTheFirstStatement() {
                // BUG: Diagnostic contains: remove this line
                new IllegalArgumentException("why is this here?");
                int i = 1;
                System.out.println("i = " + i);

                if (true) {
                  // BUG: Diagnostic contains: remove this line
                  new RuntimeException("oops");
                  System.out.println("another statement after exception");
                }

                switch (0) {
                  default:
                    // BUG: Diagnostic contains: remove this line
                    new RuntimeException("oops");
                    System.out.println("another statement after exception");
                }
              }

              public void firstStatementWithNoSurroundingBlock() {
                if (true)
                  // BUG: Diagnostic contains: throw new InterruptedException
                  new InterruptedException("this should be thrown");

                if (true) return;
                else
                  // BUG: Diagnostic contains: throw new ArithmeticException
                  new ArithmeticException("should also be thrown");

                switch (4) {
                  case 4:
                    System.out.println("4");
                    break;
                  default:
                    // BUG: Diagnostic contains: throw new IllegalArgumentException
                    new IllegalArgumentException("should be thrown");
                }
              }

              public void testLooksLikeAJunitTestMethod() {
                // BUG: Diagnostic contains: throw new RuntimeException
                new RuntimeException("Not thrown, and reference lost");
              }

              {
                // BUG: Diagnostic contains: throw new Exception
                new Exception();
              }
            }\
            """)
        .doTest();
  }

  @Test
  public void negativeCase() {
    compilationHelper
        .addSourceLines(
            "DeadExceptionNegativeCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            public class DeadExceptionNegativeCases {
              public void noError() {
                Exception e = new RuntimeException("stored");
                e = new UnsupportedOperationException("also stored");
                throw new IllegalArgumentException("thrown");
              }

              public Exception returnsException() {
                return new RuntimeException("returned");
              }
            }\
            """)
        .doTest();
  }

  /**
   * It's somewhat common to test the side-effects of Exception constructors by creating one, and
   * asserting that an exception is thrown in the constructor.
   */
  @Test
  public void negativeCaseWhenExceptionsUnthrownInTests() {
    compilationHelper
        .addSourceLines(
            "DeadExceptionTestingNegativeCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            import junit.framework.TestCase;
            import org.junit.Test;

            /**
             * @author alexeagle@google.com (Alex Eagle)
             */
            public class DeadExceptionTestingNegativeCases extends TestCase {

              public void testShouldAllowTestingOfExceptionConstructorSideEffects() {
                try {
                  new IllegalArgumentException((Throwable) null);
                  fail();
                } catch (NullPointerException e) {
                  // expected
                }
              }

              @Test
              public void shouldAllowTestingOfExceptionConstructorSideEffects() {
                try {
                  new IllegalArgumentException((Throwable) null);
                  fail();
                } catch (NullPointerException e) {
                  // expected
                }
              }
            }\
            """)
        .doTest();
  }
}
