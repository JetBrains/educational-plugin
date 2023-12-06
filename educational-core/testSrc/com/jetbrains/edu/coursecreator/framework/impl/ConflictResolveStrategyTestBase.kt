package com.jetbrains.edu.coursecreator.framework.impl

import com.jetbrains.edu.coursecreator.framework.diff.FLConflictResolveStrategy
import com.jetbrains.edu.learning.EduTestCase

abstract class ConflictResolveStrategyTestBase<T : FLConflictResolveStrategy> : EduTestCase() {
  abstract val conflictStrategy: T
}