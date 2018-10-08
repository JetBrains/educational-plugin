package com.jetbrains.edu.learning.serialization

import com.intellij.util.xmlb.XmlSerializer
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOMission
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOStation
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.remote.LocalInfo
import com.jetbrains.edu.learning.courseFormat.remote.RemoteInfo
import com.jetbrains.edu.learning.courseFormat.tasks.*
import com.jetbrains.edu.learning.serialization.SerializationUtils.ITEMS
import com.jetbrains.edu.learning.serialization.SerializationUtils.Xml.*
import com.jetbrains.edu.learning.stepik.courseFormat.*
import org.fest.util.Lists
import org.jdom.Element
import java.util.*

private var courseElementTypes: List<Class<out Course>> = Lists.newArrayList(Course::class.java, StepikCourse::class.java,
                                                                             CheckiOCourse::class.java)

private var itemElementTypes: List<Class<out StudyItem>> = Lists.newArrayList(Section::class.java, Lesson::class.java,
                                                                              FrameworkLesson::class.java,
                                                                              CheckiOStation::class.java)

private var taskElementTypes: List<Class<out Task>> = Lists.newArrayList(CheckiOMission::class.java, EduTask::class.java,
                                                                         ChoiceTask::class.java,
                                                                         TheoryTask::class.java, CodeTask::class.java,
                                                                         OutputTask::class.java,
                                                                         IdeTask::class.java)

private var remoteInfoTypes: List<Class<out RemoteInfo>> = Lists.newArrayList(StepikCourseRemoteInfo::class.java,
                                                                              StepikLessonRemoteInfo::class.java,
                                                                              StepikSectionRemoteInfo::class.java,
                                                                              StepikTaskRemoteInfo::class.java,
                                                                              LocalInfo::class.java)

fun deserializeCourse(xmlCourse: Element): Course? {
  for (courseClass in courseElementTypes) {
    val courseElement = xmlCourse.getChild(courseClass.simpleName) ?: continue

    val courseBean = courseClass.newInstance()
    XmlSerializer.deserializeInto(courseBean, courseElement)
    deserializeItems(courseElement, courseBean)
    deserializeRemoteInfo(courseElement, courseBean)
    return courseBean
  }

  return null
}

fun deserializeCourseElement(courseElement: Element): Course? {
  for (courseClass in courseElementTypes) {
    if (courseElement.name != courseClass.simpleName) {
      continue
    }
    val courseBean = courseClass.newInstance()
    XmlSerializer.deserializeInto(courseBean, courseElement)
    deserializeItems(courseElement, courseBean)
    deserializeRemoteInfo(courseElement, courseBean)
    return courseBean
  }

  return null
}

private fun deserializeTasks(parentElement: Element, lesson: Lesson) {
  val tasks = ArrayList<Task>()
  val taskElements = getChildList(parentElement, TASK_LIST)
  for (taskElement in taskElements) {
    for (taskClass in taskElementTypes) {
      if (taskElement.name != taskClass.simpleName) {
        continue
      }
      val task = taskClass.newInstance()
      XmlSerializer.deserializeInto(task, taskElement)
      deserializeRemoteInfo(taskElement, task)
      tasks.add(task)
    }
  }
  lesson.taskList = tasks
}

private fun deserializeItems(courseElement: Element, itemsContainer: ItemContainer) {
  val items = ArrayList<StudyItem>()
  val itemElements = getChildList(courseElement, ITEMS)
  for (element in itemElements) {
    for (itemClass in itemElementTypes) {
      if (element.name != itemClass.simpleName) {
        continue
      }
      val studyItem = itemClass.newInstance()
      XmlSerializer.deserializeInto(studyItem, element)

      if (studyItem is Lesson) {
        deserializeTasks(element, studyItem)
      }
      else if (studyItem is Section) {
        deserializeItems(element, studyItem as ItemContainer)
      }
      deserializeRemoteInfo(element, studyItem)
      items.add(studyItem)
    }
  }
  itemsContainer.items = items
}

private fun deserializeRemoteInfo(parentElement: Element, remoteInfoHolder: StudyItem) {
  for (remoteInfoClass in remoteInfoTypes) {
    val remoteItemElement = parentElement.getChild(remoteInfoClass.simpleName) ?: continue
    val remoteInfo = remoteInfoClass.newInstance()
    XmlSerializer.deserializeInto(remoteInfo, remoteItemElement)
    remoteInfoHolder.remoteInfo = remoteInfo
  }
}

fun serializeCourse(course: Course): Element {
  val courseClass = course.javaClass
  val courseElement = Element(courseClass.simpleName)
  XmlSerializer.serializeInto(course, courseElement)

  addItemElements(course, courseElement)
  addRemoteInfo(courseElement, course.remoteInfo)
  return courseElement
}

private fun addItemElements(itemContainer: ItemContainer, itemContainerElement: Element) {
  val list = Element(LIST)
  for (item in itemContainer.items) {
    val itemClass = item.javaClass
    val itemElement = Element(itemClass.simpleName)
    XmlSerializer.serializeInto(item, itemElement)
    if (item is Lesson) {
      addTaskElements(item, itemElement)
    }
    else if (item is Section) {
      addItemElements(item, itemElement)
    }

    addRemoteInfo(itemElement, item.remoteInfo)
    list.addContent(itemElement)
  }
  addChildWithName(itemContainerElement, ITEMS, list)
}

private fun addTaskElements(item: Lesson, itemElement: Element) {
  val list = Element(LIST)
  for (task in item.getTaskList()) {
    val taskElement = serializeTask(task)
    list.addContent(taskElement)
  }
  addChildWithName(itemElement, TASK_LIST, list)
}

private fun serializeTask(task: Task): Element {
  val taskClass = task.javaClass
  val taskElement = Element(taskClass.simpleName)
  XmlSerializer.serializeInto(task, taskElement)

  addRemoteInfo(taskElement, task.remoteInfo)
  return taskElement
}

private fun addRemoteInfo(parentElement: Element, info: RemoteInfo) {
  val remoteInfoElement = serializeRemoteInfo(info)
  parentElement.addContent(remoteInfoElement)
}

private fun serializeRemoteInfo(remoteInfo: RemoteInfo): Element {
  val remoteInfoClass = remoteInfo.javaClass
  val remoteInfoElement = Element(remoteInfoClass.simpleName)
  XmlSerializer.serializeInto(remoteInfo, remoteInfoElement)
  return remoteInfoElement
}
