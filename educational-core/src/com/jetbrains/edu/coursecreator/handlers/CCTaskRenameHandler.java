package com.jetbrains.edu.coursecreator.handlers;

import com.intellij.ide.TitledHandler;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFileSystemItem;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.ext.CourseExt;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import org.jetbrains.annotations.NotNull;

public class CCTaskRenameHandler extends CCRenameHandler implements TitledHandler {
  @Override
  protected boolean isAvailable(@NotNull Project project, @NotNull VirtualFile file) {
    return EduUtils.isTaskDirectory(project, file);
  }

  @Override
  protected void rename(@NotNull Project project, @NotNull Course course, @NotNull PsiFileSystemItem item) {
    if (!(item instanceof PsiDirectory)) return;
    PsiDirectory directory = (PsiDirectory)item;
    String sourceDir = CourseExt.getSourceDir(course);
    if (directory.getName().equals(sourceDir)) {
      directory = directory.getParent();
      if (directory == null) {
        return;
      }
    }
    PsiDirectory lessonDir = directory.getParent();
    if (lessonDir == null) {
      return;
    }
    Lesson lesson = EduUtils.getLesson(lessonDir.getVirtualFile(), course);
    if (lesson == null) {
      return;
    }
    String directoryName = directory.getName();
    Task task = lesson.getTask(directoryName);
    if (task != null) {
      processRename(task, EduNames.TASK, course, project, directory.getVirtualFile());
    }
  }

  @Override
  public String getActionTitle() {
    return "Rename task";
  }
}
