package com.jetbrains.edu.learning.serialization.converter.xml

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.serialization.SerializationUtils.ITEMS
import com.jetbrains.edu.learning.serialization.SerializationUtils.LESSONS
import com.jetbrains.edu.learning.serialization.SerializationUtils.Xml.*
import com.jetbrains.edu.learning.serialization.StudyUnrecognizedFormatException
import org.jdom.Element

/**
 * In ninth version lesson and task directory names were converted to their titles
 * instead of lesson<index> and task<index>
 */
class ToNinthVersionXmlConverter : XmlConverter {
  @Throws(StudyUnrecognizedFormatException::class)
  override fun convert(project: Project, state: Element): Element {
    val clone = state.clone()
    val taskManagerElement = clone.getChild(MAIN_ELEMENT)
    val courseElement = getCourseElement(taskManagerElement)
    for (lesson in getChildList(courseElement, LESSONS)) {
      val lessonDir = project.baseDir.findChild(EduNames.LESSON + getAsInt(lesson, INDEX)) ?: throw StudyUnrecognizedFormatException()
      for (task in getChildList(lesson, TASK_LIST)) {
        val taskDir = lessonDir.findChild(EduNames.TASK + getAsInt(task, INDEX)) ?: throw StudyUnrecognizedFormatException()
        runWriteAction {
          taskDir.rename(ToNinthVersionXmlConverter::class.java, GeneratorUtils.getUniqueValidName(lessonDir, getName(task)))
        }
      }
      runWriteAction {
        lessonDir.rename(ToNinthVersionXmlConverter::class.java, GeneratorUtils.getUniqueValidName(project.baseDir, getName(lesson)))
      }
    }
    val lessons = getChildWithName(courseElement, LESSONS)
    renameElement(lessons, ITEMS)
    return clone
  }

  private fun getName(element: Element) = getChildWithName(element, NAME).getAttributeValue(VALUE)
}
