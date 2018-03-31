package com.jetbrains.edu.coursecreator.handlers;

import com.intellij.ide.TitledHandler;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import org.jetbrains.annotations.NotNull;

public class CCLessonRenameHandler extends CCRenameHandler implements TitledHandler {
  @Override
  protected boolean isAvailable(@NotNull Project project, @NotNull VirtualFile dir) {
    return EduUtils.isLessonDirectory(project, dir);
  }

  @Override
  protected void rename(@NotNull Project project, @NotNull Course course, @NotNull PsiDirectory directory) {
    Lesson lesson = EduUtils.getLesson(directory.getVirtualFile(), course);
    if (lesson != null) {
      processRename(lesson, EduNames.LESSON, project, directory.getVirtualFile());
    }
  }

  @Override
  public String getActionTitle() {
    return "Rename lesson";
  }
}
