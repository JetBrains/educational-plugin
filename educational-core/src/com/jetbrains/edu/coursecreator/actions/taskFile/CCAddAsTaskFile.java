package com.jetbrains.edu.coursecreator.actions.taskFile;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;

public class CCAddAsTaskFile extends CCTaskFileActionBase {
  public static final String ACTION_NAME = "Make Visible to Student";

  public CCAddAsTaskFile() {
    super(ACTION_NAME);
  }


  protected void performAction(VirtualFile file, Task task, Course course, Project project) {
    EduUtils.runUndoableAction(project, ACTION_NAME, new AddTaskFile(file, null, project, task));
  }

  protected boolean isAvailable(Project project, VirtualFile file) {
    return EduUtils.getTaskFile(project, file) == null && !EduUtils.isTestsFile(project, file);
  }

  private static class AddTaskFile extends TaskUndoableAction {
    private TaskFile myTaskFile;
    private final Project myProject;

    public AddTaskFile(VirtualFile file, TaskFile taskFile, Project project, Task task) {
      super(task, file);
      myTaskFile = taskFile;
      myProject = project;
    }

    @Override
    public void performUndo() {
      if (myTaskFile == null) return;
      CCHideFromStudent.hideFromStudent(getFile(), myProject, getTask().getTaskFiles(), myTaskFile);
      ProjectView.getInstance(myProject).refresh();
    }

    @Override
    public void performRedo() {
      if (myTaskFile != null) {
        getTask().addTaskFile(myTaskFile);
      } else {
        final String taskRelativePath = EduUtils.pathRelativeToTask(myProject, getFile());
        getTask().addTaskFile(taskRelativePath, getTask().getTaskFiles().size());
        myTaskFile = getTask().getTaskFile(taskRelativePath);
      }
      ProjectView.getInstance(myProject).refresh();
    }
  }
}
