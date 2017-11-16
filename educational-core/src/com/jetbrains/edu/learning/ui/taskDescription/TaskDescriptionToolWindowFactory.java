package com.jetbrains.edu.learning.ui.taskDescription;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.jetbrains.edu.learning.EduSettings;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.courseFormat.Course;
import icons.EducationalCoreIcons;
import org.jetbrains.annotations.NotNull;

public class TaskDescriptionToolWindowFactory implements ToolWindowFactory, DumbAware {
  public static final String STUDY_TOOL_WINDOW = "Task Description";


  @Override
  public void createToolWindowContent(@NotNull final Project project, @NotNull final ToolWindow toolWindow) {
    toolWindow.setIcon(EducationalCoreIcons.TaskDescription);
    final Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course != null) {
      final TaskDescriptionToolWindow taskDescriptionToolWindow;
      if (EduUtils.hasJavaFx() && EduSettings.getInstance().shouldUseJavaFx()) {
        taskDescriptionToolWindow = new JavaFxToolWindow();
      }
      else {
        taskDescriptionToolWindow = new SwingToolWindow();
      }
      taskDescriptionToolWindow.init(project, true);
      final ContentManager contentManager = toolWindow.getContentManager();
      final Content content = contentManager.getFactory().createContent(taskDescriptionToolWindow, null, false);
      contentManager.addContent(content);
      Disposer.register(project, taskDescriptionToolWindow);
    }
  }
}
