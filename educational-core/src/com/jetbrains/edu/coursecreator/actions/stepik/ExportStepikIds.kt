package com.jetbrains.edu.coursecreator.actions.stepik

import com.google.gson.*
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


  private fun <T>JsonObject.addChildren(propertyName: String, children: List<T>, serializeChild: (T) -> JsonElement){
    val jsonArray = JsonArray()
    for (childItem in children) {
      jsonArray.add(serializeChild(childItem))
    }
    add(propertyName, jsonArray)
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val course = StudyTaskManager.getInstance(project).course ?: return

    val serializer = JsonSerializer<StudyItem> { item, type, context ->
      val jsonObject = JsonObject()
      jsonObject.addProperty("title", item.name)
      jsonObject.addProperty("id", item.id)

      val serializeStudyItem: (StudyItem) -> JsonElement =  {itemToSerialize -> context.serialize(itemToSerialize, StudyItem::class.java)}

      if (item is ItemContainer) {
        jsonObject.addChildren("items", item.items, serializeStudyItem)
      }
      if (item is Lesson) {
        jsonObject.addChildren("task_list", item.taskList, serializeStudyItem)
        jsonObject.addProperty("unit_id", item.unitId)
      }
      if (item is RemoteCourse) {
        if (item.sectionIds.isNotEmpty()) {
          jsonObject.addChildren("sectionIds", item.sectionIds) { id ->
            JsonPrimitive(id)
          }
        }
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