package com.jetbrains.edu.learning.serialization.converter.xml

import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.serialization.SerializationUtils.Xml.*
import org.jdom.Element

class To16VersionXmlConverter : BaseXmlConverter() {
  private var convertPlaceholdersLength = false

  override fun convertTaskElement(task: Element) {
    val taskFiles = getChildMap<String, Element>(task, FILES)
    for (taskFile in taskFiles.values) {
      taskFile.removeContent(getChildWithName(taskFile, TRACK_LENGTH))

      val answerPlaceholders = getChildList(taskFile, ANSWER_PLACEHOLDERS)
      for (placeholder in answerPlaceholders) {
        placeholder.removeContent(getChildWithName(placeholder, USE_LENGTH))

        if (!convertPlaceholdersLength) continue
        val length = getChildWithName(placeholder, LENGTH)
        val possibleAnswer = getChildWithName(placeholder, POSSIBLE_ANSWER).getAttributeValue(VALUE)
        length.setAttribute(VALUE, possibleAnswer.length.toString())
      }
    }
  }

  override fun convertCourseElement(course: Element) {
    val courseMode = getChildWithName(course, COURSE_MODE)
    if (courseMode.getAttributeValue(VALUE) == CCUtils.COURSE_MODE) {
      convertPlaceholdersLength = true
    }
  }
}