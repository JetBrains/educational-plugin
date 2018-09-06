package com.jetbrains.edu.learning.serialization.converter.xml

import com.jetbrains.edu.learning.serialization.SerializationUtils
import com.jetbrains.edu.learning.serialization.SerializationUtils.Xml.*
import com.jetbrains.edu.learning.serialization.converter.LANGUAGE_TASK_ROOTS
import org.jdom.Element

class ToEleventhVersionXmlConverter : BaseXmlConverter() {

  private lateinit var language: String

  override fun convertCourseElement(course: Element) {
    language = SerializationUtils.Xml.getAsString(course, LANGUAGE)
  }

  override fun convertTaskElement(task: Element) {
    val (taskFilesRoot, testFilesRoot) = LANGUAGE_TASK_ROOTS[language] ?: return

    val taskFileMap = getChildMap<String, Element>(task, TASK_FILES)
      .map { (path, taskFileElement) ->
        val newPath = "$taskFilesRoot/$path"
        val newTaskFileElement = taskFileElement.clone()
        val nameElement = getChildWithName(newTaskFileElement, NAME)
        for (placeholderElement in getChildList(newTaskFileElement, ANSWER_PLACEHOLDERS)) {
          convertPlaceholder(placeholderElement, taskFilesRoot)
        }
        changeValue(nameElement, newPath)
        newPath to newTaskFileElement
      }.toMap()

    task.removeContent(getChildWithName(task, TASK_FILES))
    addChildMap(task, TASK_FILES, taskFileMap)

    val testFileMap = getChildMap<String, String>(task, TEST_FILES)
      .mapKeys { (path, _) -> "$testFilesRoot/$path" }
    task.removeContent(getChildWithName(task, TEST_FILES))
    addTextChildMap(task, TEST_FILES, testFileMap)
  }

  private fun convertPlaceholder(placeholderElement: Element, taskFilesDir: String) {
    val dependencyElement = getChildWithName(placeholderElement, PLACEHOLDER_DEPENDENCY, true)?.children?.getOrNull(0) ?: return
    val filePathElement = getChildWithName(dependencyElement, DEPENDENCY_FILE_NAME, true) ?: return
    val filePath = filePathElement.getAttributeValue(VALUE)
    changeValue(filePathElement, "$taskFilesDir/$filePath")
  }
}
