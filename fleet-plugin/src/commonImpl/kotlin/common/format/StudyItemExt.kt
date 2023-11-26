package fleet.edu.common.format

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import fleet.api.FileAddress
import fleet.api.child
import fleet.api.exists
import fleet.common.fs.fsService

suspend fun StudyItem.getDir(courseDir: FileAddress): FileAddress? {
  val fsApi = requireNotNull(fsService(courseDir)) { "There must be fs service for file $courseDir" }
  val childDir = when (this) {
    is Course -> courseDir
    is Section -> courseDir.child(name)
    is Lesson -> if (parent is Section) courseDir.child((parent as Section).name).child(name) else courseDir.child(name)
    is Task -> parent.getDir(courseDir)?.child(name)
    else -> error("Can't find directory for the item $itemType")
  } ?: return null

  if (!fsApi.exists(childDir.path)) {
    error("Can't find directory for the item $itemType : $childDir")
  }
  return childDir
}
