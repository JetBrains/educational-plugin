package com.jetbrains.edu.coursecreator.handlers;

import com.intellij.ide.TitledHandler;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Section;
import org.jetbrains.annotations.NotNull;

public class CCSectionRenameHandler extends CCRenameHandler implements TitledHandler {
  @Override
  protected boolean isAvailable(@NotNull Project project, @NotNull VirtualFile dir) {
    Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course == null) {
      return false;
    }
    return course.getSection(dir.getName()) != null;
  }

  @Override
  protected void rename(@NotNull Project project, @NotNull Course course, @NotNull PsiDirectory directory) {
    Section section = course.getSection(directory.getName());
    if (section != null) {
      processRename(section, EduNames.SECTION, course, project, directory.getVirtualFile());
    }
  }

  @Override
  public String getActionTitle() {
    return "Rename section";
  }
}
