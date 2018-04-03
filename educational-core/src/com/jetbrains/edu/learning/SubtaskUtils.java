package com.jetbrains.edu.learning;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.checker.CheckUtils;
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder;
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholderSubtaskInfo;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.navigation.NavigationUtils;
import com.jetbrains.edu.learning.ui.taskDescription.TaskDescriptionToolWindow;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SubtaskUtils {
  private static final Logger LOG = Logger.getInstance(SubtaskUtils.class);

  private SubtaskUtils() {
  }

  @Nullable
  public static String getTestFileName(@NotNull Project project, int subtaskIndex) {
    Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course == null) {
      return null;
    }
    EduConfigurator configurator = EduConfiguratorManager.forLanguage(course.getLanguageById());
    if (configurator == null) {
      return null;
    }
    String defaultTestFileName = configurator.getTestFileName();
    String nameWithoutExtension = FileUtil.getNameWithoutExtension(defaultTestFileName);
    String extension = FileUtilRt.getExtension(defaultTestFileName);
    return nameWithoutExtension + EduNames.SUBTASK_MARKER + subtaskIndex + "." + extension;
  }

  public static void updateUI(@NotNull Project project, @NotNull Task task, VirtualFile taskDir, boolean navigateToTask) {
    CheckUtils.drawAllPlaceholders(project, task);
    ProjectView.getInstance(project).refresh();
    TaskDescriptionToolWindow toolWindow = EduUtils.getStudyToolWindow(project);
    if (toolWindow != null) {
        String text = EduUtils.getTaskTextFromTask(taskDir, task);
        if (text == null) {
            toolWindow.setEmptyText(project);
            return;
        }
        toolWindow.setText(text);
    }
    if (navigateToTask) {
      NavigationUtils.navigateToTask(project, task);
    }
  }

  public static void refreshPlaceholder(@NotNull Editor editor, @NotNull AnswerPlaceholder placeholder) {
    int prevSubtaskIndex = placeholder.getActiveSubtaskIndex() - 1;
    AnswerPlaceholderSubtaskInfo info = placeholder.getSubtaskInfos().get(prevSubtaskIndex);
    String replacementText = info != null ? info.getAnswer() : placeholder.getTaskText();
    EduUtils.replaceAnswerPlaceholder(editor.getDocument(), placeholder, placeholder.getRealLength(), replacementText);
  }
}