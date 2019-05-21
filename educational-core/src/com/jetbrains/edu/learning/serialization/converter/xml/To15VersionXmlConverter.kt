package com.jetbrains.edu.learning.serialization.converter.xml

import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.serialization.SerializationUtils
import com.jetbrains.edu.learning.serialization.SerializationUtils.Xml.*
import org.jdom.Element

class To15VersionXmlConverter : BaseXmlConverter() {
  override fun convertCourseElement(course: Element) {
    val language = getChildWithName(course, LANGUAGE)
    if (language.getAttributeValue(VALUE) == EduNames.SCALA) {
      val environment = getChildWithName(course, SerializationUtils.ENVIRONMENT)
      if (environment.getAttributeValue(VALUE).isEmpty()) {
        environment.setAttribute(VALUE, "Gradle")
      }
    }
  }
}