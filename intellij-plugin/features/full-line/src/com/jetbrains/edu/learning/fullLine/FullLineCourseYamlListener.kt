package com.jetbrains.edu.learning.fullLine

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.yaml.YamlListener

class FullLineCourseYamlListener(private val project: Project) : YamlListener {
  override fun itemDeserialized(item: StudyItem) {
    if (item is Course && item.courseMode != CourseMode.STUDENT) {
      updateAiCompletion(project, item)
    }
  }

  override fun beforeYamlLoad(configFile: VirtualFile) {}

  override fun yamlFailedToLoad(configFile: VirtualFile, exception: String) {}
}
