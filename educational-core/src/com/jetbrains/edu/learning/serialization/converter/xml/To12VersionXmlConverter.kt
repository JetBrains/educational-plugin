package com.jetbrains.edu.learning.serialization.converter.xml

import com.jetbrains.edu.learning.serialization.SerializationUtils
import com.jetbrains.edu.learning.serialization.SerializationUtils.Xml.SECTION_IDS
import org.jdom.Element

class To12VersionXmlConverter : BaseXmlConverter() {

  override fun convertCourseElement(course: Element) {
    val sectionIds = SerializationUtils.Xml.getChildList(course, SECTION_IDS, true)
    if (sectionIds != null && sectionIds.isEmpty()) { // TODO: write test for the migration
      course.name = "StepikCourse"
    }
  }

}
