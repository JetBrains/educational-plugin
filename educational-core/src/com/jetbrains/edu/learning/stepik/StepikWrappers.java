package com.jetbrains.edu.learning.stepik;

import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.courseFormat.*;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask;
import com.jetbrains.edu.learning.stepik.serialization.StepikSubmissionTaskAdapter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

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
      if (task instanceof TheoryTask) {
        step.name = "text";
        step.text = task.getTaskDescription(false, null);
      }
      else {
        step.source = StepOptions.fromTask(project, task);
      }
      return step;
    }
  }

  public static class StepOptions {
    @SerializedName("task_type")
    @Expose public String taskType;
    @Expose public List<FileWrapper> test;
    @Expose public String title;
    @Expose public List<TaskFile> files;
    @Expose public List<FileWrapper> text;
    @Expose public List<List<String>> samples;
    @SerializedName("additional_files")
    @Expose public List<FileWrapper> additionalFiles;
    @Expose public Integer executionMemoryLimit;
    @Expose public Integer executionTimeLimit;
    @Expose public Map<String, String> codeTemplates;
    @SerializedName("format_version")
    @Expose public int formatVersion = StepikConnector.CURRENT_VERSION;

    public static StepOptions fromTask(@NotNull final Project project, @NotNull final Task task) {
      final StepOptions source = new StepOptions();
      source.title = task.getName();
      setTests(task, source, project);
      setTaskTexts(task, source);
      setTaskFiles(project, task, source);
      setAdditionalFiles(task, source);
      source.taskType = task.getTaskType();
      return source;
    }

    private static void setTaskTexts(@NotNull Task task, @NotNull StepOptions stepOptions) {
      stepOptions.text = new ArrayList<>();
      for (Map.Entry<String, String> entry : task.getTaskTexts().entrySet()) {
        stepOptions.text.add(new FileWrapper(entry.getKey(), entry.getValue()));
      }
    }

    private static void setTaskFiles(@NotNull Project project, @NotNull Task task, @NotNull StepOptions source) {
      source.files = new ArrayList<>();
      if (!task.getLesson().isAdditional()) {
        final VirtualFile taskDir = task.getTaskDir(project);
        assert taskDir != null;
        for (final Map.Entry<String, TaskFile> entry : task.getTaskFiles().entrySet()) {
          ApplicationManager.getApplication().runWriteAction(() -> {
            VirtualFile answerFile = EduUtils.findTaskFileInDir(entry.getValue(), taskDir);
            if (answerFile == null) return;
            TaskFile studentTaskFile = EduUtils.createStudentFile(project, answerFile, null);
            if (studentTaskFile == null) return;
            source.files.add(studentTaskFile);
          });
        }
      }
    }

    private static void setTests(@NotNull Task task, @NotNull StepOptions source, @NotNull Project project) {
      final Map<String, String> testsText = task.getTestsText();
      source.test = new ArrayList<>();
      if (testsText.isEmpty()) {
        List<VirtualFile> testFiles = EduUtils.getTestFiles(task, project);
        for (VirtualFile testFile : testFiles) {
          addFileWrapper(testFile, source.test);
        }
      }
      else {
        for (Map.Entry<String, String> entry : testsText.entrySet()) {
          source.test.add(new FileWrapper(entry.getKey(), entry.getValue()));
        }
      }
    }
  }

  private static void setAdditionalFiles(@NotNull Task task, @NotNull StepOptions stepOptions) {
    stepOptions.additionalFiles = new ArrayList<>();
    for (Map.Entry<String, String> entry : task.getAdditionalFiles().entrySet()) {
      stepOptions.additionalFiles.add(new FileWrapper(entry.getKey(), entry.getValue()));
    }
  }

  private static void addFileWrapper(@NotNull VirtualFile file, List<FileWrapper> wrappers) {
    try {
      wrappers.add(new FileWrapper(file.getName(), VfsUtilCore.loadText(file)));
    }
    catch (IOException e) {
      LOG.error(e);
    }
  }

  public static class CoursesContainer {
    public List<RemoteCourse> courses;
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
    RemoteCourse course;

    public CourseWrapper(@NotNull Course course) {
      this.course = new RemoteCourse();
      this.course.setName(course.getName());
      this.course.setLanguage(course.getLanguageID());
      this.course.setDescription(course.getDescription());
      this.course.setAuthors(course.getAuthors());
      if (course instanceof RemoteCourse) {
        this.course.setInstructors(((RemoteCourse)course).getInstructors());
        this.course.setPublic(((RemoteCourse)course).isPublic());
      }
    }
  }

  public static class LessonWrapper {
    Lesson lesson;

    public LessonWrapper(Lesson lesson) {
      this.lesson = new Lesson();
      this.lesson.setName(lesson.getName());
      this.lesson.setId(lesson.getId());
      this.lesson.steps = new ArrayList<>();
      this.lesson.setPublic(true);
    }
  }

  static class LessonContainer {
    List<Lesson> lessons;
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
    List<Section> sections;
    public List<Section> getSections() {
      return sections;
    }
  }

  public static class Unit {
    int id;
    int section;
    int lesson;
    int position;
    List<Integer> assignments;

    public void setSection(int section) {
      this.section = section;
    }

    public void setPosition(int position) {
      this.position = position;
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
  }

  static class UnitContainer {

    List<Unit> units;
  }

  public static class UnitWrapper {
    Unit unit;

    public void setUnit(Unit unit) {
      this.unit = unit;
    }
  }

  static class AttemptWrapper {
    static class Attempt {
      public Attempt(int step) {
        this.step = step;
      }

      int step;
      int id;
    }

    public AttemptWrapper(int step) {
      attempt = new Attempt(step);
    }

    Attempt attempt;
  }

  public static class AdaptiveAttemptWrapper {
    public static class Attempt {
      int step;
      public Dataset dataset;
      String dataset_url;
      String status;
      String time;
      String time_left;
      String user;
      String user_id;
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

    public AdaptiveAttemptWrapper(int step) {
      attempt = new Attempt(step);
    }

    Attempt attempt;
  }

  static class AdaptiveAttemptContainer {
    List<AdaptiveAttemptWrapper.Attempt> attempts;
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
    List<StepicUser> users;
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

    public static final int VERSION = 2;

    String score;
    List<SolutionFile> solution;
    String language;
    String code;
    String edu_task;
    public int version = VERSION;

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

  static class RecommendationReaction {
    int reaction;
    String user;
    String lesson;

    public RecommendationReaction(int reaction, String user, String lesson) {
      this.reaction = reaction;
      this.user = user;
      this.lesson = lesson;
    }
  }

  static class RecommendationReactionWrapper {
    RecommendationReaction recommendationReaction;

    public RecommendationReactionWrapper(RecommendationReaction recommendationReaction) {
      this.recommendationReaction = recommendationReaction;
    }
  }

  static class RecommendationWrapper {
    Recommendation[] recommendations;
  }

  static class Recommendation {
    String id;
    String lesson;
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

  static class TokenInfo {
    @Expose String accessToken;
    @Expose String refreshToken;
    @Expose String tokenType;
    @Expose String scope;
    @Expose int expiresIn;

    public TokenInfo() {
      accessToken = "";
      refreshToken = "";
    }

    public String getAccessToken() {
      return accessToken;
    }

    public String getRefreshToken() {
      return refreshToken;
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
