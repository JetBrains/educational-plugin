package com.jetbrains.edu.coursecreator.actions.stepik

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonSerializer
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.vfs.VfsUtil
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.ItemContainer
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.RemoteCourse
import com.jetbrains.edu.learning.courseFormat.StudyItem


class ExportStepikIds : DumbAwareAction("Export Stepik Ids", "Exports Stepik ids as json", null) {


  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val course = StudyTaskManager.getInstance(project).course ?: return

    val serializer = JsonSerializer<StudyItem> { item, type, context ->
      val jsonObject = JsonObject()
      jsonObject.addProperty("title", item.name)
      jsonObject.addProperty("id", item.id)


      val addChildren: (String, List<StudyItem>) -> Unit = { propertyName, children ->
        val jsonArray = JsonArray()
        for (childItem in children) {
          jsonArray.add(context.serialize(childItem, StudyItem::class.java))
        }
        jsonObject.add(propertyName, jsonArray)
      }

      if (item is ItemContainer) {
        addChildren("items", item.items)
      }
      if (item is Lesson) {
        addChildren("task_list", item.taskList)
      }
      jsonObject
    }

    val gson = GsonBuilder().registerTypeAdapter(StudyItem::class.java, serializer).setPrettyPrinting().create()
    val json = gson.toJson(course, StudyItem::class.java)
    runWriteAction {
      val stepikIdsFile = EduUtils.getCourseDir(project).findOrCreateChildData(this, EduNames.STEPIK_IDS_JSON)
      VfsUtil.saveText(stepikIdsFile, json)
      FileEditorManager.getInstance(project).openFile(stepikIdsFile, true)
    }
  }

  override fun update(e: AnActionEvent) {
    val presentation = e.presentation
    presentation.isEnabledAndVisible = false
    val project = e.project ?: return
    if (!CCUtils.isCourseCreator(project)) {
      return
    }
    val course = StudyTaskManager.getInstance(project).course ?: return
    if (course !is RemoteCourse) {
      return
    }
    presentation.isEnabledAndVisible = true
  }
}