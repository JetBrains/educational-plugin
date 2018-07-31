package com.jetbrains.edu.python.learning.checkio;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.checkio.connectors.CheckiOConnector;
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse;
import com.jetbrains.edu.learning.checkio.model.CheckiOMission;
import com.jetbrains.edu.learning.checkio.model.CheckiOMissionList;
import com.jetbrains.edu.learning.checkio.model.CheckiOStation;
import com.jetbrains.edu.learning.courseFormat.*;
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.python.learning.PyCourseBuilder;
import com.jetbrains.edu.python.learning.newproject.PyCourseProjectGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PyCheckiOCourseProjectGenerator extends PyCourseProjectGenerator {
  private static final Logger LOG = Logger.getInstance(PyCheckiOCourseProjectGenerator.class);

  public PyCheckiOCourseProjectGenerator(@NotNull PyCourseBuilder builder,
                                         @NotNull Course course) {
    super(builder, course);
  }

  @Override
  protected void createAdditionalFiles(@NotNull Project project, @NotNull VirtualFile baseDir) {

  }

  @Override
  protected boolean beforeProjectGenerated() {
    final CheckiOMissionList missions = CheckiOConnector.getMissionList();
    final CheckiOCourse newCourse = generateCourseFromMissions(missions);

    if (newCourse == null) {
      LOG.warn("Error occurred generating course");
      return false;
    }

    myCourse = newCourse;
    return true;
  }

  @Nullable
  private static CheckiOCourse generateCourseFromMissions(@Nullable CheckiOMissionList missions) {
    if (missions == null) {
      LOG.warn("Mission list is null");
      return null;
    }

    final List<CheckiOStation> stations = missions.groupByStation();
    final CheckiOCourse newCourse = new CheckiOCourse();

    for (CheckiOStation station : stations) {
      final Lesson newLesson = createLessonContent(station);
      newCourse.addLesson(newLesson);
    }
    return newCourse;
  }

  @NotNull
  private static Lesson createLessonContent(@NotNull CheckiOStation station) {
    final Lesson lesson = new Lesson();

    lesson.setName(station.getName());
    lesson.setId(station.getId());

    for (CheckiOMission mission : station.getMissions()) {
      Task task = createTaskContent(mission);
      lesson.addTask(task);
    }
    return lesson;
  }

  @NotNull
  private static Task createTaskContent(@NotNull CheckiOMission mission) {
    final Task task = new EduTask();

    task.setStepId(mission.getId());
    task.setName(mission.getTitle());
    task.setDescriptionFormat(DescriptionFormat.HTML);
    task.setDescriptionText(mission.getDescription());

    TaskFile taskFile = createTaskFileContent(mission);
    task.addTaskFile(taskFile);

    task.setStatus(mission.isSolved() ? CheckStatus.Solved : CheckStatus.Unchecked);
    return task;
  }

  @NotNull
  private static TaskFile createTaskFileContent(@NotNull CheckiOMission mission) {
    final TaskFile taskFile = new TaskFile();

    taskFile.name = mission.getTitle() + ".py";
    taskFile.text = mission.getCode().replaceAll("\r\n", "\n");
    taskFile.setHighlightErrors(true);

    return taskFile;
  }
}
