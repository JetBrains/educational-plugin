package com.jetbrains.edu.coursecreator.handlers;

import com.intellij.ide.TitledHandler;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFileSystemItem;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import org.jetbrains.annotations.NotNull;

public class CCLessonRenameHandler extends CCRenameHandler implements TitledHandler {
  @Override
  protected boolean isAvailable(@NotNull Project project, @NotNull VirtualFile file) {
    return EduUtils.isLessonDirectory(project, file);
  }

  @Override
  protected void rename(@NotNull Project project, @NotNull Course course, @NotNull PsiFileSystemItem item) {
    Lesson lesson = EduUtils.getLesson(item.getVirtualFile(), course);
    if (lesson != null) {
      processRename(lesson, EduNames.LESSON, course, project, item.getVirtualFile());
    }
  }

  @Override
  public String getActionTitle() {
    return "Rename lesson";
  }
}
