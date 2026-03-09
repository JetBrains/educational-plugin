package com.jetbrains.edu.learning.yaml

import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.intellij.openapi.vfs.VirtualFile

interface YamlListener {
  fun itemDeserialized(item: StudyItem)
  fun beforeYamlLoad(configFile: VirtualFile)
  fun yamlFailedToLoad(configFile: VirtualFile, exception: String)
}