package com.jetbrains.edu.learning.serialization.converter.xml

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.JDOMUtil
import com.jetbrains.edu.learning.serialization.SerializationUtils
import com.jetbrains.edu.learning.serialization.SerializationUtils.Xml.*
import com.jetbrains.edu.learning.serialization.StudyUnrecognizedFormatException
import org.jdom.Element

abstract class BaseXmlConverter : XmlConverter {

  @Throws(StudyUnrecognizedFormatException::class)
  override fun convert(project: Project, element: Element): Element {
    val clone = element.clone()
    val taskManagerElement = clone.getChild(MAIN_ELEMENT)
    val courseElement = getCourseElement(taskManagerElement)

    convertCourseElement(courseElement)
    for (item in getChildList(courseElement, SerializationUtils.ITEMS)) {
      when (item.name) {
        SerializationUtils.Xml.SECTION -> {
          convertSectionElement(item)
          for (lesson in getChildList(item, SerializationUtils.ITEMS)) {
            convertLesson(lesson)
          }
        }
        SerializationUtils.Xml.LESSON -> convertLesson(item)
        else -> throw StudyUnrecognizedFormatException("Unknown item name `${item.name}` in\n${JDOMUtil.write(courseElement, "\n")}")
      }
    }
    return clone
  }

  private fun convertLesson(lesson: Element) {
    convertLessonElement(lesson)
    for (task in getChildList(lesson, TASK_LIST)) {
      convertTaskElement(task)
    }
  }

  protected open fun convertCourseElement(course: Element) {}
  protected open fun convertSectionElement(section: Element) {}
  protected open fun convertLessonElement(lesson: Element) {}
  protected open fun convertTaskElement(task: Element) {}
}
