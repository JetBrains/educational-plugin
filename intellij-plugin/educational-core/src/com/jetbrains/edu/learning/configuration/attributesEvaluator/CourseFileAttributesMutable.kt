package com.jetbrains.edu.learning.configuration.attributesEvaluator

import com.jetbrains.edu.learning.configuration.CourseFileAttributes
import com.jetbrains.edu.learning.configuration.InclusionPolicy

internal class CourseFileAttributesMutable {
  fun toImmutable(): CourseFileAttributes =
    CourseFileAttributes(excludedFromArchive, inclusionPolicy)

  var excludedFromArchive: Boolean = false
  var inclusionPolicy: InclusionPolicy = InclusionPolicy.OPTIONAL
}