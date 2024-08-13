package com.jetbrains.edu.learning.yaml.format

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseraCourse
import com.jetbrains.edu.learning.courseFormat.codeforces.CodeforcesCourse

class CourseChangeApplier(project: Project) : ItemContainerChangeApplier<Course>(project) {
  override fun applyChanges(existingItem: Course, deserializedItem: Course) {
    super.applyChanges(existingItem, deserializedItem)
    existingItem.name = deserializedItem.name
    existingItem.description = deserializedItem.description
    existingItem.languageCode = deserializedItem.languageCode
    existingItem.translatedToLanguageCode = deserializedItem.translatedToLanguageCode
    existingItem.environment = deserializedItem.environment
    existingItem.solutionsHidden = deserializedItem.solutionsHidden
    existingItem.vendor = deserializedItem.vendor
    existingItem.feedbackLink = deserializedItem.feedbackLink
    existingItem.isMarketplacePrivate = deserializedItem.isMarketplacePrivate
    existingItem.languageId = deserializedItem.languageId
    existingItem.languageVersion = deserializedItem.languageVersion
    if (deserializedItem is CourseraCourse && existingItem is CourseraCourse) {
      existingItem.submitManually = deserializedItem.submitManually
    }
    if (deserializedItem is CodeforcesCourse && existingItem is CodeforcesCourse) {
      existingItem.endDateTime = deserializedItem.endDateTime
      existingItem.programTypeId = deserializedItem.programTypeId
    }
  }
}
