package com.jetbrains.edu.learning.json.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import com.jetbrains.edu.learning.courseFormat.EduFormatNames
import com.jetbrains.edu.learning.json.migration.MigrationNames.ANDROID
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.COURSE_TYPE
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.ENVIRONMENT

class To11VersionLocalCourseConverter : JsonLocalCourseConverterBase() {

  override fun convert(localCourse: ObjectNode): ObjectNode {
    val courseType = localCourse.get(COURSE_TYPE)?.asText() ?: EduFormatNames.PYCHARM
    if (courseType == ANDROID) {
      localCourse.put(ENVIRONMENT, ANDROID)
      localCourse.put(COURSE_TYPE, EduFormatNames.PYCHARM)
    }
    return localCourse
  }
}
