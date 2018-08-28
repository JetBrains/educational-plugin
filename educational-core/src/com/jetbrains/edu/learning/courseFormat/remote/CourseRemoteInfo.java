package com.jetbrains.edu.learning.courseFormat.remote;

import com.intellij.openapi.project.Project;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Tag;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import org.fest.util.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

public interface CourseRemoteInfo {
  default @NotNull
  List<Tag> getTags() { return Lists.emptyList(); }
  default boolean isCourseValid(@NotNull Course course) { return true; }
  default @NotNull String wrapTaskText(@NotNull final String taskText, @NotNull final Task task) { return taskText; }
  default @Nullable
  JPanel getAdditionalDescriptionPanel(Project project) { return null; }

}
