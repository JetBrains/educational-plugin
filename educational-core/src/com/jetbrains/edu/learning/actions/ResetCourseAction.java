package com.jetbrains.edu.learning.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.courseFormat.*;
import com.jetbrains.edu.learning.courseFormat.tasks.ChoiceTask;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;

import java.util.ArrayList;
import java.util.Map;

public class ResetCourseAction extends DumbAwareAction {


  public ResetCourseAction() {
    super("Reset Course", "Reset Course", null);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getProject();
    assert project != null;

    StudyTaskManager studyTaskManager = StudyTaskManager.getInstance(project);
    Course course = studyTaskManager.getCourse();
    assert course != null;
    ((RemoteCourse)course).setLoadSolutions(false);

    ApplicationManager.getApplication().runWriteAction(() -> {
      for (Lesson lesson : course.getLessons()) {
        for (Task task : lesson.getTaskList()) {
          VirtualFile taskDir = task.getTaskDir(project);
          if (taskDir == null) continue;
          for (Map.Entry<String, TaskFile> entry : task.getTaskFiles().entrySet()) {
            String relativePath = entry.getKey();
            TaskFile taskFile = entry.getValue();
            VirtualFile taskFileVF = taskDir.findFileByRelativePath(relativePath);
            if (taskFileVF != null) {
              Document document = EduUtils.getDocument(project.getBasePath(), lesson.getIndex(), task.getIndex(), relativePath);
              if (document != null) {
                RefreshTaskFileAction.resetDocument(document, taskFile);
                task.setStatus(CheckStatus.Unchecked);
                if (task instanceof ChoiceTask) {
                  ((ChoiceTask)task).setSelectedVariants(new ArrayList<>());
                }
                RefreshTaskFileAction.resetAnswerPlaceholders(taskFile, project);
              }
            }
          }
        }
      }

      RefreshTaskFileAction.refresh(project);
    });
  }

  @Override
  public void update(AnActionEvent e) {
    Project project = e.getProject();
    if (project != null) {
      Course course = StudyTaskManager.getInstance(project).getCourse();
      if (course instanceof RemoteCourse) {
        e.getPresentation().setEnabledAndVisible(true);
        return;
      }
    }
    e.getPresentation().setEnabledAndVisible(false);
  }
}
