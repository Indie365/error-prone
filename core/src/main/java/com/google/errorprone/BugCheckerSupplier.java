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

package com.google.errorprone;

import com.google.common.base.Supplier;
import com.google.errorprone.BugPattern.MaturityLevel;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.BugPattern.Suppressibility;
import com.google.errorprone.bugpatterns.BugChecker;

import java.util.Map;
import java.util.Objects;

/**
 * Supplies {@link BugChecker} instances.  Allows us to abstract over different ways of acquiring
 * {@link BugChecker}s, for example, by reflectively instantiating them from their {@link Class}es
 * vs. already having an instance.  Additionally provides information about the
 * checker, including canonical name, severity, maturity, and whether the checker may
 * be disabled.
 *
 * <p>Implementations of this class should be immutable.
 */
public abstract class BugCheckerSupplier implements Supplier<BugChecker> {

  /* Static factory methods */

  /**
   * Returns a {@link BugCheckerSupplier} given a {@link Class} that extends {@link BugChecker}.
   */
  public static BugCheckerSupplier fromClass(Class<? extends BugChecker> checkerClass) {
    return new FromClassBugCheckerSupplier(checkerClass);
  }

  /**
   * Returns a {@link BugCheckerSupplier} given a {@link BugChecker} instance.
   */
  public static BugCheckerSupplier fromInstance(BugChecker checker) {
    return new FromInstanceBugCheckerSupplier(checker);
  }

  /* Instance methods */

  /**
   * The canonical name of the {@link BugChecker}, e.g. "ArrayEquals".
   */
  public abstract String canonicalName();

  /**
   * The default {@link SeverityLevel} of the {@link BugChecker}, e.g. {@link SeverityLevel#ERROR}.
   */
  public abstract SeverityLevel defaultSeverity();
  
  /**
   * The {@link Suppressibility} of the {@link BugChecker}, e.g.
   * {@link Suppressibility#UNSUPPRESSIBLE}
   */
  public abstract Suppressibility suppressibility();

  /**
   * The per-compilation {@link SeverityLevel} of the {@link BugChecker}, e.g.
   * {@link SeverityLevel#ERROR}.
   */
  public abstract SeverityLevel severity(Map<String, SeverityLevel> severityMap);

  /**
   * The {@link MaturityLevel} of the {@link BugChecker}, e.g. {@link MaturityLevel#EXPERIMENTAL}.
   */
  public abstract MaturityLevel maturity();

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof BugCheckerSupplier) {
      BugCheckerSupplier that = (BugCheckerSupplier) obj;
      return this.canonicalName().equals(that.canonicalName())
          && this.defaultSeverity().equals(that.defaultSeverity())
          && this.maturity().equals(that.maturity())
          && this.suppressibility().equals(that.suppressibility());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(canonicalName(), defaultSeverity(), maturity(), suppressibility());
  }
}
