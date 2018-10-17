package com.jetbrains.edu.learning.serialization.converter.xml

import com.jetbrains.edu.learning.serialization.SerializationUtils.ID
import com.jetbrains.edu.learning.serialization.SerializationUtils.Xml.*
import org.jdom.Element

class To12VersionXmlConverter : BaseXmlConverter() {
  override fun convertCourseElement(course: Element) {
    val name = course.name
    if (name == REMOTE_COURSE) {
      course.name = STEPIK_COURSE
    }
    val id = getChildWithName(course, ID, true)
    if (id == null || id.getAttributeValue(VALUE).toInt() == 0) {
      val remoteInfo = Element(LOCAL_INFO)
      course.addContent(remoteInfo)
    }
    else {
      val updateDate = getChildWithName(course, UPDATE_DATE)
      val public = getChildWithName(course, PUBLIC)
      val additionalDate = getChildWithName(course, ADDITIONAL_UPDATE_DATE)
      val compatible = getChildWithName(course, COMPATIBLE)
      val type = getChildWithName(course, COURSE_TYPE)
      val loadSolutions = getChildWithName(course, LOAD_SOLUTIONS)

      val instructors = getChildWithName(course, INSTRUCTORS)
      val sectionIds = getChildWithName(course, SECTION_IDS)

      val courseRemoteInfo = Element(COURSE_REMOTE_INFO)
      addChildWithName(courseRemoteInfo, ADDITIONAL_UPDATE_DATE, additionalDate.getAttributeValue(VALUE))
      addChildWithName(courseRemoteInfo, COURSE_FORMAT, type.getAttributeValue(VALUE))
      addChildWithName(courseRemoteInfo, ID, id.getAttributeValue(VALUE))
      addChildWithName(courseRemoteInfo, IDEA_COMPATIBLE, compatible.getAttributeValue(VALUE))
      val instructorsList = instructors.getChild(LIST)?.clone() ?: Element(LIST)
      addChildWithName(courseRemoteInfo, INSTRUCTORS, instructorsList)
      addChildWithName(courseRemoteInfo, LOAD_SOLUTIONS, loadSolutions.getAttributeValue(VALUE))
      addChildWithName(courseRemoteInfo, PUBLIC, public.getAttributeValue(VALUE))
      val sectionList = sectionIds.getChild(LIST)?.clone() ?: Element(LIST)
      addChildWithName(courseRemoteInfo, SECTION_IDS, sectionList)
      addChildWithName(courseRemoteInfo, UPDATE_DATE, updateDate.getAttributeValue(VALUE))

      course.addContent(courseRemoteInfo)

      course.removeContent(id)
      course.removeContent(updateDate)
      course.removeContent(public)
      course.removeContent(additionalDate)
      course.removeContent(compatible)
      course.removeContent(type)
      course.removeContent(loadSolutions)
      course.removeContent(instructors)
      course.removeContent(sectionIds)
    }
  }

  override fun convertSectionElement(section: Element) {
    val id = getChildWithName(section, ID, true)
    val updateDate = getChildWithName(section, UPDATE_DATE)
    val courseId = getChildWithName(section, COURSE_ID)
    val units = getChildWithName(section, UNITS)
    val position = getChildWithName(section, POSITION)

    if (id.getAttributeValue(VALUE).toInt() == 0) {
      val sectionRemoteInfo = Element(LOCAL_INFO)
      section.addContent(sectionRemoteInfo)
    }
    else {
      val sectionRemoteInfo = Element(SECTION_REMOTE_INFO)
      addChildWithName(sectionRemoteInfo, COURSE_ID, courseId.getAttributeValue(VALUE))
      addChildWithName(sectionRemoteInfo, ID, id.getAttributeValue(VALUE))
      addChildWithName(sectionRemoteInfo, POSITION, position.getAttributeValue(VALUE))
      val unitsList = units.getChild(LIST)?.clone() ?: Element(LIST)
      addChildWithName(sectionRemoteInfo, UNITS, unitsList)
      addChildWithName(sectionRemoteInfo, UPDATE_DATE, updateDate.getAttributeValue(VALUE))

      section.addContent(sectionRemoteInfo)

    }
    section.removeContent(id)
    section.removeContent(updateDate)
    section.removeContent(courseId)
    section.removeContent(units)
    section.removeContent(position)
  }

  override fun convertLessonElement(lesson: Element) {
    val id = getChildWithName(lesson, ID, true)
    val unitId = getChildWithName(lesson, UNIT_ID)
    val updateDate = getChildWithName(lesson, UPDATE_DATE)

    if (id.getAttributeValue(VALUE).toInt() == 0) {
      val remoteInfo = Element(LOCAL_INFO)
      lesson.addContent(remoteInfo)
    }
    else {
      val lessonRemoteInfo = Element(LESSON_REMOTE_INFO)
      addChildWithName(lessonRemoteInfo, ID, id.getAttributeValue(VALUE))
      addChildWithName(lessonRemoteInfo, PUBLIC, "true")
      addChildWithName(lessonRemoteInfo, UNIT_ID, unitId.getAttributeValue(VALUE))
      addChildWithName(lessonRemoteInfo, UPDATE_DATE, updateDate.getAttributeValue(VALUE))

      lesson.addContent(lessonRemoteInfo)

    }
    lesson.removeContent(id)
    lesson.removeContent(updateDate)
    lesson.removeContent(unitId)
  }

  override fun convertTaskElement(task: Element) {
    val id = getChildWithName(task, STEP_ID)
    val updateDate = getChildWithName(task, UPDATE_DATE)

    if (id.getAttributeValue(VALUE).toInt() == 0) {
      val remoteInfo = Element(LOCAL_INFO)
      task.addContent(remoteInfo)
    }
    else {
      val taskRemoteInfo = Element(TASK_REMOTE_INFO)
      addChildWithName(taskRemoteInfo, ID, id.getAttributeValue(VALUE))
      addChildWithName(taskRemoteInfo, UPDATE_DATE, updateDate.getAttributeValue(VALUE))
      task.addContent(taskRemoteInfo)
    }

    task.removeContent(id)
    task.removeContent(updateDate)
  }
}
