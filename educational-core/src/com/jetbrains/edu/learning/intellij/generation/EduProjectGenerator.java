package com.jetbrains.edu.learning.intellij.generation;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseGeneration.ProjectGenerator;
import org.jetbrains.annotations.NotNull;

public class EduProjectGenerator extends ProjectGenerator {
  @Override
  public void generateProject(@NotNull Project project, @NotNull VirtualFile baseDir,
                              @NotNull final Course courseInfo) {
    final Course course = initCourse(courseInfo, project);
    updateCourseFormat(course);
    StudyTaskManager.getInstance(project).setCourse(course);
  }
}
