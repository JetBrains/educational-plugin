package com.jetbrains.edu.coursecreator.actions.stepik;

import com.intellij.ide.IdeView;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task.Modal;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.coursecreator.stepik.CCStepikConnector;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.RemoteCourse;
import com.jetbrains.edu.learning.courseFormat.StepikChangeStatus;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.stepik.StepikConnector;
import com.jetbrains.edu.learning.stepik.StepikNames;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

import static com.jetbrains.edu.coursecreator.stepik.CCStepikConnector.*;

public class CCPushTask extends DumbAwareAction {
  public CCPushTask() {
    super("Update Task on Stepik", "Update Task on Stepik", null);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabledAndVisible(false);
    final IdeView view = e.getData(LangDataKeys.IDE_VIEW);
    final Project project = e.getData(CommonDataKeys.PROJECT);
    if (view == null || project == null) {
      return;
    }
    final Course course = StudyTaskManager.getInstance(project).getCourse();
    if (!(course instanceof RemoteCourse)) {
      return;
    }
    if (!course.getCourseMode().equals(CCUtils.COURSE_MODE)) return;
    final PsiDirectory[] directories = view.getDirectories();
    if (directories.length == 0 || directories.length > 1) {
      return;
    }
    final PsiDirectory taskDir = directories[0];
    if (taskDir == null) {
      return;
    }
    final PsiDirectory lessonDir = taskDir.getParentDirectory();
    if (lessonDir == null) {
      return;
    }
    Lesson lesson = CCUtils.lessonFromDir(course, lessonDir, project);
    if (lesson != null && lesson.getId() > 0 && course.getId() > 0) {
      e.getPresentation().setEnabledAndVisible(true);
      final Task task = lesson.getTask(taskDir.getName());
      if (task != null && task.getStepId() <= 0) {
        e.getPresentation().setText("Upload Task to Stepik");
      }
    }
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final IdeView view = e.getData(LangDataKeys.IDE_VIEW);
    final Project project = e.getData(CommonDataKeys.PROJECT);
    if (view == null || project == null) {
      return;
    }
    final Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course == null) {
      return;
    }
    final PsiDirectory[] directories = view.getDirectories();
    if (directories.length == 0 || directories.length > 1) {
      return;
    }

    final PsiDirectory taskDir = directories[0];

    if (taskDir == null) {
      return;
    }

    final PsiDirectory lessonDir = taskDir.getParentDirectory();
    if (lessonDir == null) return;

    Lesson lesson = CCUtils.lessonFromDir(course, lessonDir, project);
    if (lesson == null) return;

    final Task task = lesson.getTask(taskDir.getName());
    if (task == null) return;

    ProgressManager.getInstance().run(new Modal(project, "Uploading Task", true) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        indicator.setText("Uploading task to " + StepikNames.STEPIK_URL);
        boolean toPost = task.getStepId() <= 0;
        if (toPost) {
          postNewTask(project, task, lesson);
        }
        else {
          updateTask(task, lesson, project);
        }
      }
    });
  }

  private static void postNewTask(Project project, Task task, Lesson lesson) {
    int position = stepikPosition(task, lesson);
    boolean isPosted = postTask(project, task, lesson, position);
    if (!isPosted) {
      showErrorNotification(project, "Error uploading task", "Task " + task.getName() + "wasn't uploaded");
      return;
    }


    // if task was added in the middle we have to update positions of underlying pushed tasks
    boolean isLast = task.getIndex() == lesson.taskList.size();
    if (!isLast) {
      List<Task> postedTasks = getPostedTasks(lesson);
      List<Task> underlyingTasks = postedTasks.subList(position, postedTasks.size());
      updateTasksPositions(project, position + 1, underlyingTasks);
    }

    task.setStepikChangeStatus(StepikChangeStatus.UP_TO_DATE);
    showNotification(project, "Task " + task.getName() + " uploaded",
                     openOnStepikAction("/lesson/" + lesson.getId() + "/step/" + task.getIndex()));
  }


  private static List<Task> getPostedTasks(Lesson lesson) {
    return lesson.taskList.stream().filter(t -> t.getId() > 0).collect(Collectors.toList());
  }

  private static void updateTask(Task task, Lesson lesson, Project project) {
    int position = stepikPosition(task, lesson);
    int positionOnServer = StepikConnector.getTaskPosition(task.getId());

    boolean isPosted = updateTask(task, project, position);
    if (!isPosted) {
      showErrorNotification(project, "Error updating task", "Task " + task.getName() + "wasn't updated");
      return;
    }


    // if task position was changed we had to update affected tasks.
    // Position was changed for tasks that are between current and previous task position
    if (position != positionOnServer) {
      List<Task> postedTasks = getPostedTasks(lesson);
      boolean movedUp = position - positionOnServer < 0;
      if (movedUp) {
        List<Task> underlyingTasks = postedTasks.subList(position, positionOnServer);
        updateTasksPositions(project, position + 1, underlyingTasks);
      }
      else {
        List<Task> higherTasks = postedTasks.subList(positionOnServer - 1, position - 1);
        updateTasksPositions(project, position - 1, higherTasks);
      }
    }

    task.setStepikChangeStatus(StepikChangeStatus.UP_TO_DATE);
    showNotification(project, "Task " + task.getName() + " updated",
                     openOnStepikAction("/lesson/" + lesson.getId() + "/step/" + task.getIndex()));
  }

  private static boolean postTask(Project project, Task task, Lesson lesson, int position) {
    Task taskCopy = task.copy();
    taskCopy.setIndex(position);
    taskCopy.setLesson(lesson);
    boolean isPosted = CCStepikConnector.postTask(project, taskCopy, lesson.getId());
    if (isPosted) {
      task.setStepId(taskCopy.getStepId());
      return true;
    }

    return false;
  }

  private static boolean updateTask(Task task, Project project, int position) {
    Task taskCopy = task.copy();
    taskCopy.setIndex(position);
    taskCopy.setLesson(task.getLesson());
    return CCStepikConnector.updateTask(project, taskCopy);
  }

  private static void updateTasksPositions(@NotNull Project project, int initialPosition, List<Task> tasksToUpdate) {
    int position = initialPosition;
    for (Task task : tasksToUpdate) {
      if (task.getId() == 0) continue;
      updateTask(task, project, position++);
    }
  }

  /**
   * Calculates estimated task position on Stepik by counting pushed tasks only.
   */
  private static int stepikPosition(@NotNull Task taskToUpdate, @NotNull Lesson lesson) {
    int position = 1;
    for (Task task : lesson.taskList) {
      if (task.getName().equals(taskToUpdate.getName())) {
        break;
      }

      if (task.getId() > 0) {
        position++;
      }
    }

    return position;
  }
}