package com.jetbrains.edu.learning.configuration.attributesEvaluator

import com.jetbrains.edu.learning.configuration.ArchiveInclusionPolicy
import com.jetbrains.edu.learning.configuration.CourseFileAttributes

internal class CourseFileAttributesMutable {
  fun toImmutable(): CourseFileAttributes =
    CourseFileAttributes(excludedFromArchive, inclusionPolicy)

  var excludedFromArchive: Boolean = false
  var inclusionPolicy: ArchiveInclusionPolicy = ArchiveInclusionPolicy.AUTHOR_DECISION
}