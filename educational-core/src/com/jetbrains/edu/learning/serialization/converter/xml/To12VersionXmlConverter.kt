package com.jetbrains.edu.learning.serialization.converter.xml

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.serialization.SerializationUtils
import com.jetbrains.edu.learning.serialization.SerializationUtils.COURSE
import com.jetbrains.edu.learning.serialization.SerializationUtils.Xml.*
import org.jdom.Element

class To12VersionXmlConverter : BaseXmlConverter() {
  override fun convert(project: Project, element: Element): Element {
    val clone = element.clone()
    val taskManagerElement = clone.getChild(SerializationUtils.Xml.MAIN_ELEMENT)
    val courseElement = getRemoteCourseElement(taskManagerElement)
    if (courseElement != null) {
      val sectionIds = SerializationUtils.Xml.getChildList(courseElement, SECTION_IDS, true)
      if (sectionIds != null && sectionIds.isEmpty()) {
        courseElement.name = "StepikCourse"
      }
    }
    return clone
  }

  private fun getRemoteCourseElement(taskManagerElement: Element): Element? {
    val courseHolder = getChildWithName(taskManagerElement, COURSE)
    return courseHolder.getChild(REMOTE_COURSE)
  }
}
