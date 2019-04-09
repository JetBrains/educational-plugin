package com.jetbrains.edu.learning.serialization.converter.xml

import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.serialization.SerializationUtils
import com.jetbrains.edu.learning.serialization.SerializationUtils.ID
import com.jetbrains.edu.learning.serialization.SerializationUtils.Xml.*
import org.jdom.Element

class To14VersionXmlConverter : BaseXmlConverter() {
  override fun convertCourseElement(course: Element) {
    val courseType = getChildWithName(course, COURSE_TYPE)
    val environment = if (courseType.getAttributeValue(VALUE) == EduNames.ANDROID) EduNames.ANDROID else EduNames.DEFAULT_ENVIRONMENT
    addChildWithName(course, SerializationUtils.ENVIRONMENT, environment)
    course.removeContent(courseType)
  }

  override fun convertTaskElement(task: Element) {
    val stepId = getChildWithName(task, STEP_ID)
    renameElement(stepId, ID)
  }

  override fun convertLessonElement(lesson: Element) {
    val taskList = getChildWithName(lesson, TASK_LIST)
    renameElement(taskList, SerializationUtils.ITEMS)
  }
}