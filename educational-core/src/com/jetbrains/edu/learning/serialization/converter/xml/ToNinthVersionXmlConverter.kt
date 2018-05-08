package com.jetbrains.edu.learning.serialization.converter.xml

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.serialization.SerializationUtils.*
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
      val lessonDir = EduUtils.getCourseDir(project).findChild(EduNames.LESSON + getAsInt(lesson, INDEX)) ?: throw StudyUnrecognizedFormatException()
      for (task in getChildList(lesson, TASK_LIST)) {
        val taskDir = lessonDir.findChild(EduNames.TASK + getAsInt(task, INDEX)) ?: throw StudyUnrecognizedFormatException()
        runWriteAction {
          val taskName = getName(task)
          val uniqueValidName = GeneratorUtils.getUniqueValidName(lessonDir, taskName)
          val nameElement = getChildWithName(task, NAME)
          if (nameElement != null && uniqueValidName != taskName) {
            changeValue(nameElement, uniqueValidName)
            addChildWithName(task, CUSTOM_NAME, taskName)
          }
          taskDir.rename(ToNinthVersionXmlConverter::class.java, uniqueValidName)
        }
        removeSubtaskInfos(task)
        migrateDescription(task)
      }
      runWriteAction {
        val lessonName = getName(lesson)
        val uniqueValidName = GeneratorUtils.getUniqueValidName(project.baseDir, lessonName)
        val nameElement = getChildWithName(lesson, NAME)
        if (nameElement != null && uniqueValidName != lessonName) {
          changeValue(nameElement, uniqueValidName)
          addChildWithName(lesson, CUSTOM_NAME, lessonName)
        }
        lessonDir.rename(ToNinthVersionXmlConverter::class.java, uniqueValidName)
      }
    }
    val lessons = getChildWithName(courseElement, LESSONS)
    renameElement(lessons, ITEMS)
    return clone
  }

  @Throws(StudyUnrecognizedFormatException::class)
  private fun removeSubtaskInfos(task: Element) {
    for ((_, taskFile) in getChildMap<String, Element>(task, TASK_FILES)) {
      for (placeholder in getChildList(taskFile, ANSWER_PLACEHOLDERS)) {
        val subtaskInfos = getChildMap<String, Element>(placeholder, SUBTASK_INFOS)
        val info = subtaskInfos.values.firstOrNull() ?: throw StudyUnrecognizedFormatException("Can't find any subtask info")
        addChildWithName(placeholder, PLACEHOLDER_TEXT, getAsString(info, PLACEHOLDER_TEXT))
        addChildWithName(placeholder, POSSIBLE_ANSWER, getAsString(info, POSSIBLE_ANSWER))
        addChildWithName(placeholder, STATUS, getAsString(info, PLACEHOLDER_TEXT))
        placeholder.addContent(getChildWithName(info, HINTS).clone())
        placeholder.removeContent(getChildWithName(placeholder, SUBTASK_INFOS))
      }
    }
  }

  private fun migrateDescription(task: Element) {
    val description = getChildMap<String, String>(task, TASK_TEXTS).values.firstOrNull()
                      ?: throw StudyUnrecognizedFormatException("`$TASK_TEXTS` map is empty")
    addChildWithName(task, DESCRIPTION_TEXT, description)
    addChildWithName(task, DESCRIPTION_FORMAT, DescriptionFormat.HTML)
    task.removeContent(getChildWithName(task, TASK_TEXTS))
  }

  private fun getName(element: Element) = getChildWithName(element, NAME).getAttributeValue(VALUE)
}
