package com.jetbrains.edu.cognifire.log

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

@State(name = "ProdeLoggerManager", storages = [Storage("storage.xml")])
@Service(Service.Level.PROJECT)
class ProdeLoggerManager : PersistentStateComponent<ProdeLoggerManager> {
  private val prodeData: MutableMap<String, Pair<PromptData, CodeData>> = mutableMapOf()

  fun addProdeData(prodeId: String, promptData: PromptData, codeData: CodeData) {
    prodeData[prodeId] = promptData to codeData
  }

  fun getProdeData(prodeId: String): Pair<PromptData, CodeData> {
    return prodeData[prodeId] ?: (PromptData("", "", "") to CodeData(""))
  }

  override fun getState() = this

  override fun loadState(state: ProdeLoggerManager) {
    prodeData.putAll(state.prodeData)
  }

  companion object {
    fun getInstance(project: Project): ProdeLoggerManager = project.service()
  }
}