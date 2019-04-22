package com.jetbrains.edu.coursecreator.yaml.format

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.StudyItem

/**
 * Placeholder to deserialize [StudyItem] with remote info. Remote info is applied to
 * existing StudyItem by [RemoteInfoChangeApplierBase]
 */
class RemoteStudyItem : StudyItem() {
  override fun getItemType(): String {
    throw NotImplementedError()
  }

  override fun getParent(): StudyItem {
    throw NotImplementedError()
  }

  override fun init(course: Course?, parentItem: StudyItem?, isRestarted: Boolean) {
    throw NotImplementedError()
  }

  override fun getName(): String {
    throw NotImplementedError()
  }

  override fun setName(name: String?) {
    throw NotImplementedError()
  }

  override fun getDir(project: Project): VirtualFile {
    throw NotImplementedError()
  }

  override fun getCourse(): Course {
    throw NotImplementedError()
  }
}