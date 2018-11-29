package com.jetbrains.edu.learning.serialization.converter.xml

import com.jetbrains.edu.learning.coursera.CourseraNames
import com.jetbrains.edu.learning.serialization.SerializationUtils
import com.jetbrains.edu.learning.serialization.SerializationUtils.Xml.REMOTE_COURSE
import com.jetbrains.edu.learning.serialization.SerializationUtils.Xml.SECTION_IDS
import org.jdom.Element

class To12VersionXmlConverter : BaseXmlConverter() {
  override fun convertCourseElement(course: Element) {
    val sectionIds = SerializationUtils.Xml.getChildList(course, SECTION_IDS, true)
    if (sectionIds != null && sectionIds.isEmpty() && course.name == REMOTE_COURSE) {
      course.name = "StepikCourse"
    }
    if (course.name == SerializationUtils.COURSE_TITLED || course.name == REMOTE_COURSE) {
      course.name = "EduCourse"
    }
    val courseType = SerializationUtils.Xml.getAsString(course, SerializationUtils.Xml.COURSE_TYPE)
    if (courseType == CourseraNames.COURSE_TYPE) {
      course.name = "CourseraCourse"
    }
  }
}
