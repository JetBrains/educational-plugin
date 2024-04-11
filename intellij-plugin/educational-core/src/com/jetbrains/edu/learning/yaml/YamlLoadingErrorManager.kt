package com.jetbrains.edu.learning.yaml

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotifications
import com.jetbrains.edu.learning.LightTestAware
import org.jetbrains.annotations.TestOnly
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.PROJECT)
class YamlLoadingErrorManager : LightTestAware {

  private val errors: ConcurrentHashMap<String, String> = ConcurrentHashMap()

  fun getLoadingErrorForFile(file: VirtualFile): String? = errors[file.url]

  @TestOnly
  override fun cleanUpState() {
    errors.clear()
  }

  companion object {
    fun getInstance(project: Project): YamlLoadingErrorManager = project.service()
  }

  class Listener(private val project: Project) : YamlListener {
    override fun beforeYamlLoad(configFile: VirtualFile) {
      getInstance(project).errors.remove(configFile.url)
      EditorNotifications.getInstance(project).updateNotifications(configFile)
    }

    override fun yamlFailedToLoad(configFile: VirtualFile, exception: String) {
      getInstance(project).errors[configFile.url] = exception
      EditorNotifications.getInstance(project).updateNotifications(configFile)
    }
  }
}
