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

  override fun convertTaskElement(task: Element) {
    if (task.name != "ChoiceTask") {
      return
    }
    val oldOptionsElement = getChildList(task, "choiceVariants")
    println()
    val newOptionElements = oldOptionsElement.map {
      val newElement = Element("ChoiceOption")
      addChildWithName(newElement, "text", it.getAttributeValue(VALUE))
      newElement
    }
    addChildList(task, "choiceOptions", newOptionElements)
  }
}