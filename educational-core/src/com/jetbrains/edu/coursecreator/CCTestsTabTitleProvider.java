package com.jetbrains.edu.coursecreator;

import com.intellij.openapi.fileEditor.impl.EditorTabTitleProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.configuration.EduConfigurator;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.ext.CourseExt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CCTestsTabTitleProvider implements EditorTabTitleProvider {
  @Nullable
  @Override
  public String getEditorTabTitle(@NotNull Project project, @NotNull VirtualFile file) {
    if (!CCUtils.isCourseCreator(project)) {
      return null;
    }
    if (!EduUtils.isTestsFile(project, file)) {
      return null;
    }
    Course course = StudyTaskManager.getInstance(project).getCourse();
    assert course != null;
    EduConfigurator configurator = CourseExt.getConfigurator(course);
    if (configurator == null) {
      return null;
    }
    return configurator.getTestFileName();
  }
}