package com.jetbrains.edu.learning.stepik;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.EduVersions;
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat;
import com.jetbrains.edu.learning.courseFormat.FeedbackLink;
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.serialization.SerializationUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class StepikSteps {

  public static class StepsList {
    public List<StepSource> steps;
  }

  public static class Step {
    @Expose public StepOptions options;
    @Expose public String text;
    @Expose public String name = "pycharm";
    @Expose public StepOptions source;

    public static Step fromTask(Project project, @NotNull final Task task) {
      final Step step = new Step();
      step.text = task.getDescriptionText();
      step.source = StepOptions.fromTask(project, task);

      return step;
    }
  }

  public static class StepOptions {
    @SerializedName("task_type")
    @Expose public String taskType;
    @SerializedName("lesson_type")
    @Expose public String lessonType;
    @Expose public String title;
    @SerializedName(SerializationUtils.Json.DESCRIPTION_TEXT)
    @Expose public String descriptionText;
    @SerializedName(SerializationUtils.Json.DESCRIPTION_FORMAT)
    @Expose public DescriptionFormat descriptionFormat;
    @Expose
    @SerializedName("feedback_link")
    @NotNull
    public FeedbackLink myFeedbackLink = new FeedbackLink();
    @Expose public List<TaskFile> files;
    @Expose public List<List<String>> samples;
    @Expose public Integer executionMemoryLimit;
    @Expose public Integer executionTimeLimit;
    @Expose public Map<String, String> codeTemplates;
    @SerializedName("format_version")
    @Expose public int formatVersion = EduVersions.JSON_FORMAT_VERSION;

    public static StepOptions fromTask(@NotNull final Project project, @NotNull final Task task) {
      final StepOptions source = new StepOptions();
      source.title = task.getName();
      source.descriptionText = task.getDescriptionText();
      source.descriptionFormat = task.getDescriptionFormat();

      setTaskFiles(project, task, source);

      source.taskType = task.getTaskType();
      source.lessonType = task.getLesson() instanceof FrameworkLesson ? "framework" : null;
      source.myFeedbackLink = task.getFeedbackLink();
      return source;
    }

    private static void setTaskFiles(@NotNull Project project, @NotNull Task task, @NotNull StepOptions source) {
      source.files = new ArrayList<>();
      if (!task.getLesson().isAdditional()) {
        final VirtualFile taskDir = task.getTaskDir(project);
        assert taskDir != null;
        for (final Map.Entry<String, TaskFile> entry : task.getTaskFiles().entrySet()) {
          ApplicationManager.getApplication().invokeAndWait(() -> ApplicationManager.getApplication().runWriteAction(() -> {
            VirtualFile answerFile = EduUtils.findTaskFileInDir(entry.getValue(), taskDir);
            if (answerFile == null) return;
            TaskFile studentTaskFile = EduUtils.createStudentFile(project, answerFile, task);
            if (studentTaskFile == null) return;
            source.files.add(studentTaskFile);
          }));
        }
      } else {
        for (Map.Entry<String, TaskFile> entry : task.getTaskFiles().entrySet()) {
          source.files.add(entry.getValue());
        }
      }
    }
  }

  public static class StepSource {
    @Expose public int id;
    @Expose public Step block;
    @Expose public int position;
    @Expose public int lesson;
    @Expose public String progress;
    @Expose public int cost = 1;
    public Date update_date;

    @SuppressWarnings("unused")
    public StepSource() { }

    public StepSource(Project project, Task task, int lesson) {
      this.lesson = lesson;
      position = task.getIndex();
      block = Step.fromTask(project, task);
      if (task.getLesson().isAdditional()) {
        cost = 0;
      }
    }
  }
}
