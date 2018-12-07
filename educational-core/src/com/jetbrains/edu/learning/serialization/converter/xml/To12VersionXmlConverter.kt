package com.jetbrains.edu.learning.serialization.converter.xml

import com.jetbrains.edu.learning.coursera.CourseraNames
import com.jetbrains.edu.learning.serialization.SerializationUtils
import com.jetbrains.edu.learning.serialization.SerializationUtils.Xml.REMOTE_COURSE
import org.jdom.Element

class To12VersionXmlConverter : BaseXmlConverter() {
  override fun convertCourseElement(course: Element) {
    if (course.name == SerializationUtils.COURSE_TITLED || course.name == REMOTE_COURSE) {
      course.name = "EduCourse"
    }
    val courseType = SerializationUtils.Xml.getAsString(course, SerializationUtils.Xml.COURSE_TYPE)
    if (courseType == CourseraNames.COURSE_TYPE) {
      course.name = "CourseraCourse"
    }
  }
}
