package com.jetbrains.edu.learning.json.migration

import com.fasterxml.jackson.databind.node.ObjectNode

class To22VersionLocalCourseConverter : JsonLocalCourseConverterBase() {

  /**
   * Changes all `course.additional_files[ i ].is_visible` to `false`
   */
  override fun convert(localCourse: ObjectNode): ObjectNode {
    val additionalFiles = localCourse.get("additional_files")

    if (additionalFiles != null && additionalFiles.isArray) {
      additionalFiles.forEach { additionalFile ->
        if (additionalFile is ObjectNode) {
          additionalFile.put("is_visible", false)
        }
      }
    }

    return localCourse
  }
}