package com.jetbrains.edu.learning.stepik;

import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.EduVersions;
import com.jetbrains.edu.learning.courseFormat.*;
import com.jetbrains.edu.learning.courseFormat.remote.CourseRemoteInfo;
import com.jetbrains.edu.learning.stepik.courseFormat.StepikCourseRemoteInfo;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.serialization.SerializationUtils;
import com.jetbrains.edu.learning.stepik.courseFormat.StepikCourse;
import com.jetbrains.edu.learning.stepik.courseFormat.StepikLessonRemoteInfo;
import com.jetbrains.edu.learning.stepik.courseFormat.StepikSection;
import com.jetbrains.edu.learning.stepik.courseFormat.ext.StepikLessonExt;
import com.jetbrains.edu.learning.stepik.serialization.StepikSubmissionTaskAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class StepikWrappers {
  private static final Logger LOG = Logger.getInstance(StepOptions.class);

  static class StepContainer {
    List<StepSource> steps;
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
    @Expose public List<FileWrapper> test;
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
    @SerializedName("additional_files")
    @Expose public Map<String, AdditionalFile> additionalFiles;
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

      setTests(project, task, source);
      setTaskFiles(project, task, source);
      setAdditionalFiles(project, task, source);

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

    private static void setTests(@NotNull Project project, @NotNull Task task, @NotNull StepOptions source) {
      source.test = new ArrayList<>();
      if (task.getLesson().isAdditional()) {
        return;
      }

      final VirtualFile taskDir = task.getTaskDir(project);
      if (taskDir == null) {
        LOG.warn(String.format("Can't find task dir for `%s` task", task.getName()));
      } else {
        CCUtils.loadTestTextsToTask(task, taskDir);
      }

      for (Map.Entry<String, String> entry : task.getTestsText().entrySet()) {
        source.test.add(new FileWrapper(entry.getKey(), entry.getValue()));
      }
    }
  }

  private static void setAdditionalFiles(@NotNull Project project, @NotNull Task task, @NotNull StepOptions source) {
    final VirtualFile taskDir = task.getTaskDir(project);
    if (taskDir == null) {
      LOG.warn(String.format("Can't find task dir for `%s` task", task.getName()));
    } else {
      CCUtils.loadAdditionalFileTextsToTask(task, taskDir);
    }

    source.additionalFiles = new HashMap<>(task.getAdditionalFiles());
  }

  public static class CoursesContainer {
    public List<StepikCourse> courses;
    public Map meta;
  }

  public static class StepSourceWrapper {
    @Expose
    StepSource stepSource;

    public StepSourceWrapper(Project project, Task task, int lessonId) {
      stepSource = new StepSource(project, task, lessonId);
    }
  }

  public static class CourseWrapper {
    StepikCourse course;

    public CourseWrapper(@NotNull Course course) {
      this.course = new StepikCourse();
      this.course.setName(course.getName());
      this.course.setLanguage(course.getLanguageID());
      this.course.setDescription(course.getDescription());
      this.course.setAuthors(course.getAuthors());

      final StepikCourseRemoteInfo stepikRemoteInfo = new StepikCourseRemoteInfo();
      this.course.setRemoteInfo(stepikRemoteInfo);
      final CourseRemoteInfo remoteInfo = course.getRemoteInfo();
      if (remoteInfo instanceof StepikCourseRemoteInfo) {
        stepikRemoteInfo.setPublic(((StepikCourseRemoteInfo)remoteInfo).isPublic());
        stepikRemoteInfo.setInstructors((((StepikCourseRemoteInfo)remoteInfo).getInstructors()));
      }
    }
  }

  public static class LessonWrapper {
    Lesson lesson;

    public LessonWrapper(Lesson lesson) {
      this.lesson = new Lesson();
      this.lesson.setName(lesson.getName());
      final StepikLessonRemoteInfo info = new StepikLessonRemoteInfo();
      info.setId(StepikLessonExt.getId(lesson));
      info.setSteps(new ArrayList<>());
      info.setPublic(true);
      this.lesson.setRemoteInfo(info);
    }
  }

  public static class LessonContainer {
    public List<Lesson> lessons;
  }

  public static class StepSource {
    @Expose public Step block;
    @Expose public int position;
    @Expose public int lesson;
    @Expose public String progress;
    @Expose public int cost = 1;
    public Date update_date;

    public StepSource(Project project, Task task, int lesson) {
      this.lesson = lesson;
      position = task.getIndex();
      block = Step.fromTask(project, task);
      if (task.getLesson().isAdditional()) {
        cost = 0;
      }
    }
  }

  public static class FileWrapper {
    @Expose public final String name;
    @Expose public final String text;

    public FileWrapper(String name, String text) {
      this.name = name;
      this.text = text;
    }
  }

  public static class SectionWrapper {
    Section section;

    public void setSection(Section section) {
      this.section = section;
    }
  }

  public static class SectionContainer {
    List<StepikSection> sections;
    public List<StepikSection> getSections() {
      return sections;
    }
  }

  public static class Unit {
    int id;
    int section;
    int lesson;
    int position;
    @SerializedName("update_date") Date updateDate;
    List<Integer> assignments;

    public void setSection(int section) {
      this.section = section;
    }

    public void setPosition(int position) {
      this.position = position;
    }

    public int getPosition() {
      return position;
    }

    public void setLesson(int lesson) {
      this.lesson = lesson;
    }

    public int getSection() {
      return section;
    }

    public int getId() {
      return id;
    }

    public void setId(int id) {
      this.id = id;
    }

    public Date getUpdateDate() {
      return updateDate;
    }

    public void setUpdateDate(Date updateDate) {
      this.updateDate = updateDate;
    }
  }

  public static class UnitContainer {
    public List<Unit> units;
  }

  public static class UnitWrapper {
    Unit unit;

    public void setUnit(Unit unit) {
      this.unit = unit;
    }
  }

  public static class AttemptWrapper {
    public static class Attempt {
      int step;
      public Dataset dataset;
      String status;
      String user;
      int id;

      public Attempt(int step) {
        this.step = step;
      }

      public boolean isActive() {
        return status.equals("active");
      }
    }

    public static class Dataset {
      public boolean is_multiple_choice;
      public List<String> options;
    }

    public AttemptWrapper(int step) {
      attempt = new Attempt(step);
    }

    Attempt attempt;
  }

  static class AttemptContainer {
    List<AttemptWrapper.Attempt> attempts;
  }

  public static class SolutionFile {
    public String name;
    public String text;

    public SolutionFile(String name, String text) {
      this.name = name;
      this.text = text;
    }
  }

  static class AuthorWrapper {
    List<StepikUserInfo> users;
  }

  static class SubmissionsWrapper {
    Submission[] submissions;
  }

  static class SubmissionWrapper {
    Submission submission;

    public SubmissionWrapper(int attemptId, String score, ArrayList<SolutionFile> files, Task task) {
      String serializedTask = new GsonBuilder()
        .excludeFieldsWithoutExposeAnnotation()
        .registerTypeAdapter(Task.class, new StepikSubmissionTaskAdapter())
        .create()
        .toJson(new StepikWrappers.TaskWrapper(task));
      submission = new Submission(score, attemptId, files, serializedTask);
    }
  }

  public static class Reply {

    String score;
    List<SolutionFile> solution;
    String language;
    String code;
    String edu_task;
    public int version = EduVersions.JSON_FORMAT_VERSION;

    public Reply(List<SolutionFile> files, String score, String serializedTask) {
      this.score = score;
      solution = files;
      edu_task = serializedTask;
    }
  }

  public static class Submission {
    int attempt;
    public final Reply reply;

    public Submission(String score, int attemptId, ArrayList<SolutionFile> files, String serializedTask) {
      reply = new Reply(files, score, serializedTask);
      this.attempt = attemptId;
    }
  }

  static class SubmissionToPostWrapper {
    Submission submission;

    public SubmissionToPostWrapper(@NotNull String attemptId, @NotNull String language, @NotNull String code) {
      submission = new Submission(attemptId, new Submission.CodeReply(language, code));
    }

    public SubmissionToPostWrapper(@NotNull String attemptId, boolean[] choices) {
      submission = new Submission(attemptId, new Submission.ChoiceReply(choices));
    }

    static class Submission {
      String attempt;
      Reply reply;

      public Submission(String attempt, Reply reply) {
        this.attempt = attempt;
        this.reply = reply;
      }


      interface Reply {

      }

      static class CodeReply implements Reply {
        String language;
        String code;

        public CodeReply(String language, String code) {
          this.language = language;
          this.code = code;
        }
      }

      static class ChoiceReply implements Reply {
        boolean[] choices;

        public ChoiceReply(boolean[] choices) {
          this.choices = choices;
        }
      }
    }
  }

  static class ResultSubmissionWrapper {
    ResultSubmission[] submissions;

    static class ResultSubmission {
      int id;
      String status;
      String hint;
    }
  }

  static class AssignmentsWrapper {
    List<Assignment> assignments;
  }

  static class Assignment {
    int id;
    int step;
  }

  static class ViewsWrapper {
    View view;

    public ViewsWrapper(final int assignment, final int step) {
      this.view = new View(assignment, step);
    }
  }

  static class View {
    int assignment;
    int step;

    public View(int assignment, int step) {
      this.assignment = assignment;
      this.step = step;
    }
  }

  static class Enrollment {
    String course;

    public Enrollment(String courseId) {
      course = courseId;
    }
  }

  static class EnrollmentWrapper {
    Enrollment enrollment;

    public EnrollmentWrapper(@NotNull final String courseId) {
      enrollment = new Enrollment(courseId);
    }
  }

  static class ProgressContainer {
    static class Progress {
      String id;
      boolean isPassed;
    }

    List<Progress> progresses;

  }

  public static class TaskWrapper {
    @Expose Task task;

    public TaskWrapper(Task task) {
      this.task = task;
    }
  }
}
