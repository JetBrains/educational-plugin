package com.jetbrains.edu.learning.stepik;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageCommenters;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.jetbrains.edu.learning.*;
import com.jetbrains.edu.learning.checker.CheckResult;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.RemoteCourse;
import com.jetbrains.edu.learning.courseFormat.tasks.ChoiceTask;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.navigation.NavigationUtils;
import com.jetbrains.edu.learning.ui.taskDescription.TaskDescriptionToolWindow;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class StepikAdaptiveConnector {
  public static final String EDU_TOOLS_COMMENT = " Posted from EduTools plugin\n";
  public static final int NEXT_RECOMMENDATION_REACTION = 2;
  public static final int TOO_HARD_RECOMMENDATION_REACTION = 0;
  public static final int TOO_BORING_RECOMMENDATION_REACTION = -1;
  public static final String LOADING_NEXT_RECOMMENDATION = "Loading Next Recommendation";
  private static final Logger LOG = Logger.getInstance(StepikAdaptiveConnector.class);
  private static final int CONNECTION_TIMEOUT = 60 * 1000;
  // Stepik uses some code complexity measure, but we agreed that it's not obvious measure and should be improved
  private static final String CODE_COMPLEXITY_NOTE = "code complexity score";

  @Nullable
  public static Task getNextRecommendation(@Nullable Project project, @NotNull RemoteCourse course) {
    try {
      final CloseableHttpClient client = StepikAuthorizedClient.getHttpClient();
      if (client == null) {
        LOG.warn("Http client is null");
        return null;
      }

      StepicUser user = EduSettings.getInstance().getUser();
      if (user == null) {
        LOG.warn("User is null");
        return null;
      }

      final URI uri = new URIBuilder(StepikNames.STEPIK_API_URL + StepikNames.RECOMMENDATIONS_URL)
        .addParameter(EduNames.COURSE, String.valueOf(course.getId()))
        .build();
      final HttpGet request = new HttpGet(uri);
      setTimeout(request);

      final CloseableHttpResponse response = client.execute(request);
      final HttpEntity responseEntity = response.getEntity();
      final String responseString = responseEntity != null ? EntityUtils.toString(responseEntity) : "";

      final int statusCode = response.getStatusLine().getStatusCode();
      EntityUtils.consume(responseEntity);
      if (statusCode == HttpStatus.SC_OK) {
        final Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
        final StepikWrappers.RecommendationWrapper recomWrapper = gson.fromJson(responseString, StepikWrappers.RecommendationWrapper.class);

        if (recomWrapper.recommendations.length != 0) {
          final StepikWrappers.Recommendation recommendation = recomWrapper.recommendations[0];
          final String lessonId = recommendation.lesson;
          final StepikWrappers.LessonContainer lessonContainer = StepikAuthorizedClient.getFromStepik(StepikNames.LESSONS + lessonId,
                                                                                                         StepikWrappers.LessonContainer.class);
          if (lessonContainer != null && lessonContainer.lessons.size() == 1) {
            final Lesson realLesson = lessonContainer.lessons.get(0);
            course.getLessons().get(0).setId(Integer.parseInt(lessonId));

            for (int stepId : realLesson.steps) {
              StepikWrappers.StepSource step = StepikConnector.getStep(stepId);
              String stepType = step.block.name;
              StepikTaskBuilder taskBuilder = new StepikTaskBuilder(course, realLesson.getName(), step, stepId, user.getId());
              if (taskBuilder.isSupported(stepType)) {
                final Task taskFromStep = taskBuilder.createTask(stepType);
                if (taskFromStep != null) return taskFromStep;
              }
              else {
                return skipRecommendation(project, course, user, lessonId);
              }
            }
          }
          else {
            LOG.warn("Got unexpected number of lessons: " + (lessonContainer == null ? null : lessonContainer.lessons.size()));
          }
        }
        else {
          LOG.warn("Got empty recommendation for the task: " + responseString);
        }
      }
      else {
        throw new IOException("Stepik returned non 200 status code: " + responseString);
      }
    }
    catch (IOException e) {
      LOG.warn(e.getMessage());
      if (project != null) {
        ApplicationManager.getApplication()
          .invokeLater(() -> EduUtils.showErrorPopupOnToolbar(project, "Connection problems, Please, try again"));
      }
    }
    catch (URISyntaxException e) {
      LOG.warn(e.getMessage());
    }
    return null;
  }

  private static Task skipRecommendation(@Nullable Project project, @NotNull RemoteCourse course, StepicUser user, String lessonId) {
    postRecommendationReaction(lessonId, String.valueOf(user.getId()), TOO_HARD_RECOMMENDATION_REACTION);
    return getNextRecommendation(project, course);
  }

  @Nullable
  public static StepikWrappers.AdaptiveAttemptWrapper.Attempt getAttemptForStep(int stepId, int userId) {
    try {
      final List<StepikWrappers.AdaptiveAttemptWrapper.Attempt> attempts = getAttempts(stepId, userId);
      if (attempts != null && attempts.size() > 0) {
        final StepikWrappers.AdaptiveAttemptWrapper.Attempt attempt = attempts.get(0);
        return attempt.isActive() ? attempt : createNewAttempt(stepId);
      }
      else {
        return createNewAttempt(stepId);
      }
    }
    catch (URISyntaxException | IOException e) {
      LOG.warn(e.getMessage());
    }
    return null;
  }

  private static StepikWrappers.AdaptiveAttemptWrapper.Attempt createNewAttempt(int id) throws IOException {
    final String response = StepikConnector.postAttempt(id);
    final StepikWrappers.AdaptiveAttemptContainer attempt = new Gson().fromJson(response, StepikWrappers.AdaptiveAttemptContainer.class);
    return attempt.attempts.get(0);
  }

  @Nullable
  private static List<StepikWrappers.AdaptiveAttemptWrapper.Attempt> getAttempts(int stepId, int userId)
    throws URISyntaxException, IOException {
    final URI attemptUrl = new URIBuilder(StepikNames.ATTEMPTS)
      .addParameter("step", String.valueOf(stepId))
      .addParameter("user", String.valueOf(userId))
      .build();
    final StepikWrappers.AdaptiveAttemptContainer attempt =
      StepikAuthorizedClient.getFromStepik(attemptUrl.toString(), StepikWrappers.AdaptiveAttemptContainer.class);
    return attempt == null ? null : attempt.attempts;
  }

  private static void setTimeout(HttpGet request) {
    final RequestConfig requestConfig = RequestConfig.custom()
      .setConnectionRequestTimeout(CONNECTION_TIMEOUT)
      .setConnectTimeout(CONNECTION_TIMEOUT)
      .setSocketTimeout(CONNECTION_TIMEOUT)
      .build();
    request.setConfig(requestConfig);
  }

  private static void setTimeout(HttpPost request) {
    final RequestConfig requestConfig = RequestConfig.custom()
      .setConnectionRequestTimeout(CONNECTION_TIMEOUT)
      .setConnectTimeout(CONNECTION_TIMEOUT)
      .setSocketTimeout(CONNECTION_TIMEOUT)
      .build();
    request.setConfig(requestConfig);
  }

  public static boolean postRecommendationReaction(@NotNull String lessonId, @NotNull String user, int reaction) {
    final HttpPost post = new HttpPost(StepikNames.STEPIK_API_URL + StepikNames.RECOMMENDATION_REACTIONS_URL);
    final String json = new Gson()
      .toJson(new StepikWrappers.RecommendationReactionWrapper(new StepikWrappers.RecommendationReaction(reaction, user, lessonId)));
    post.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
    final CloseableHttpClient client = StepikAuthorizedClient.getHttpClient();
    if (client == null) return false;
    setTimeout(post);
    try {
      final CloseableHttpResponse execute = client.execute(post);
      final int statusCode = execute.getStatusLine().getStatusCode();
      final HttpEntity entity = execute.getEntity();
      final String entityString = EntityUtils.toString(entity);
      EntityUtils.consume(entity);
      if (statusCode == HttpStatus.SC_CREATED) {
        return true;
      }
      else {
        LOG.warn("Stepik returned non-201 status code: " + statusCode + " " + entityString);
        return false;
      }
    }
    catch (IOException e) {
      LOG.warn(e.getMessage());
      return false;
    }
  }

  public static void addNextRecommendedTask(@NotNull Project project,
                                            @NotNull Lesson lesson,
                                            @NotNull ProgressIndicator indicator,
                                            int reactionToPost) {
    final Course course = StudyTaskManager.getInstance(project).getCourse();
    if (!(course instanceof RemoteCourse)) {
      LOG.warn("Course is in incorrect state");
      ApplicationManager.getApplication().invokeLater(() -> EduUtils.showErrorPopupOnToolbar(project,
                                                                                               "Can't get next recommendation: course is broken"));
      return;
    }

    indicator.checkCanceled();
    final StepicUser user = EduSettings.getInstance().getUser();
    if (user == null) {
      LOG.warn("Can't get next recommendation: user is null");
      ApplicationManager.getApplication().invokeLater(() -> EduUtils.showErrorPopupOnToolbar(project,
                                                                                               "Can't get next recommendation: you're not logged in"));
      return;
    }

    final boolean reactionPosted = postRecommendationReaction(String.valueOf(lesson.getId()), String.valueOf(user.getId()), reactionToPost);
    if (!reactionPosted) {
      LOG.warn("Recommendation reaction wasn't posted");
      ApplicationManager.getApplication().invokeLater(() -> EduUtils.showErrorPopupOnToolbar(project, "Couldn't post your reactionToPost"));
      return;
    }

    indicator.checkCanceled();
    String oldTaskName = lesson.getTaskList().get(lesson.getTaskList().size() - 1).getName();
    final Task task = getNextRecommendation(project, (RemoteCourse)course);
    if (task == null) {
      ApplicationManager.getApplication().invokeLater(() -> EduUtils.showErrorPopupOnToolbar(project,
                                                                                               "Couldn't load a new recommendation"));
      return;
    }

    task.initTask(lesson, false);
    boolean replaceCurrentTask = reactionToPost == TOO_HARD_RECOMMENDATION_REACTION || reactionToPost == TOO_BORING_RECOMMENDATION_REACTION;
    if (replaceCurrentTask) {
      replaceCurrentTask(project, task, oldTaskName, lesson);
    }
    else {
      addAsNextTask(project, task, lesson);
    }

    ApplicationManager.getApplication().invokeLater(() -> {
      VirtualFileManager.getInstance().refreshWithoutFileWatcher(false);
      ProjectView.getInstance(project).refresh();
      NavigationUtils.navigateToTask(project, task);
    });
  }

  private static void addAsNextTask(@NotNull Project project, @NotNull Task task, @NotNull Lesson lesson) {
    Course course = StudyTaskManager.getInstance(project).getCourse();
    assert course != null;

    lesson.addTask(task);
    task.setIndex(lesson.getTaskList().size());
    //lesson.initLesson(course, true);

    createFilesForNewTask(project, task, course.getLanguageById());
  }

  private static void createFilesForNewTask(@NotNull Project project,
                                            @NotNull Task task,
                                            @NotNull Language language) {
    final VirtualFile lessonDir = project.getBaseDir().findChild(task.getLesson().getName());
    if (lessonDir == null) {
      return;
    }

    ApplicationManager.getApplication().invokeLater(() -> ApplicationManager.getApplication().runWriteAction(() -> {
      EduConfigurator<?> configurator = EduConfiguratorManager.forLanguage(language);
      if (configurator != null) {
        configurator.getCourseBuilder().createTaskContent(project, task, lessonDir, task.getLesson().getCourse());
      }
    }));
  }

  public static void replaceCurrentTask(@NotNull Project project, @NotNull Task task, String oldTaskName, @NotNull Lesson lesson) {
    Course course = StudyTaskManager.getInstance(project).getCourse();
    assert course != null;

    int taskIndex = lesson.getTaskList().size();

    task.setIndex(taskIndex);
    lesson.getTaskList().set(taskIndex - 1, task);

    updateProjectFiles(project, task, oldTaskName, course.getLanguageById());
    setToolWindowText(project, task);
  }

  private static void updateProjectFiles(@NotNull Project project, @NotNull Task task, String oldTaskName, Language language) {
    final VirtualFile lessonDir = project.getBaseDir().findChild(task.getLesson().getName());
    if (lessonDir == null) {
      return;
    }

    ApplicationManager.getApplication().invokeLater(() -> ApplicationManager.getApplication().runWriteAction(() -> {
      try {
        removeOldProjectFiles(project, task, oldTaskName);
        EduConfigurator<?> configurator = EduConfiguratorManager.forLanguage(language);
        if (configurator != null) {
          configurator.getCourseBuilder().createTaskContent(project, task, lessonDir, task.getLesson().getCourse());
        }
      }
      catch (IOException e) {
        LOG.warn(e.getMessage());
      }
    }));
  }

  private static void removeOldProjectFiles(@NotNull Project project, @NotNull Task task, @NotNull String oldTaskName) throws IOException {
    VirtualFile lessonDir = project.getBaseDir().findChild(task.getLesson().getName());
    if (lessonDir == null) {
      LOG.warn("Failed to update files for a new recommendation: lesson directory is null");
      return;
    }
    final VirtualFile taskDir = lessonDir.findChild(oldTaskName);
    if (taskDir == null) {
      LOG.warn("Failed to update files for a new recommendation: task directory is null");
      return;
    }

    taskDir.delete(StepikAdaptiveConnector.class);
  }

  private static void setToolWindowText(@NotNull Project project, @NotNull Task task) {
    final TaskDescriptionToolWindow window = EduUtils.getStudyToolWindow(project);
    if (window != null) {
      window.setCurrentTask(project, task);
    }
  }

  public static CheckResult checkChoiceTask(@NotNull ChoiceTask task, @NotNull StepicUser user) {
    if (task.getSelectedVariants().isEmpty()) return new CheckResult(CheckStatus.Failed, "No variants selected");
    final StepikWrappers.AdaptiveAttemptWrapper.Attempt attempt = getAttemptForStep(task.getStepId(), user.getId());

    if (attempt != null) {
      final int attemptId = attempt.id;

      final boolean isActiveAttempt = task.getSelectedVariants().stream()
        .allMatch(index -> attempt.dataset.options.get(index).equals(task.getChoiceVariants().get(index)));
      if (!isActiveAttempt) return new CheckResult(CheckStatus.Failed, "Your solution is out of date. Please try again");
      final StepikWrappers.SubmissionToPostWrapper wrapper = new StepikWrappers.SubmissionToPostWrapper(String.valueOf(attemptId),
                                                                                                        createChoiceTaskAnswerArray(task));
      final CheckResult result = doAdaptiveCheck(wrapper, attemptId, user.getId());
      if (result.getStatus() == CheckStatus.Failed) {
        try {
          createNewAttempt(task.getStepId());
          StepikWrappers.StepSource step = StepikConnector.getStep(task.getStepId());
          StepikTaskBuilder taskBuilder = new StepikTaskBuilder((RemoteCourse)task.getLesson().getCourse(), task.getName(),
                                                                step, task.getStepId(), user.getId());
          final Task updatedTask = taskBuilder.createTask(step.block.name);
          if (updatedTask instanceof ChoiceTask) {
            final List<String> variants = ((ChoiceTask)updatedTask).getChoiceVariants();
            task.setChoiceVariants(variants);
            task.setSelectedVariants(new ArrayList<>());
          }
        }
        catch (IOException e) {
          LOG.warn(e.getMessage());
        }
      }
      return result;
    }

    return CheckResult.FAILED_TO_CHECK;
  }

  private static boolean[] createChoiceTaskAnswerArray(@NotNull ChoiceTask task) {
    final List<Integer> selectedVariants = task.getSelectedVariants();
    final boolean[] answer = new boolean[task.getChoiceVariants().size()];
    for (Integer index : selectedVariants) {
      answer[index] = true;
    }
    return answer;
  }

  public static CheckResult checkCodeTask(@NotNull Project project, @NotNull Task task, @NotNull StepicUser user) {
    int attemptId = -1;
    try {
      attemptId = getAttemptId(task);
    }
    catch (IOException e) {
      LOG.warn(e.getMessage());
    }
    if (attemptId != -1) {
      Course course = task.getLesson().getCourse();
      Language courseLanguage = course.getLanguageById();
      final Editor editor = EduUtils.getSelectedEditor(project);
      if (editor != null) {
        String commentPrefix = LanguageCommenters.INSTANCE.forLanguage(courseLanguage).getLineCommentPrefix();
        final String answer = commentPrefix + EDU_TOOLS_COMMENT + editor.getDocument().getText();
        String defaultLanguage = StepikLanguages.langOfId(courseLanguage.getID()).getLangName();
        assert defaultLanguage != null : ("Default Stepik language not found for: " + courseLanguage.getDisplayName());
        final StepikWrappers.SubmissionToPostWrapper submissionToPost =
          new StepikWrappers.SubmissionToPostWrapper(String.valueOf(attemptId), defaultLanguage, answer);
        return doAdaptiveCheck(submissionToPost, attemptId, user.getId());
      }
    }
    else {
      LOG.warn("Got an incorrect attempt id: " + attemptId);
    }
    return CheckResult.FAILED_TO_CHECK;
  }

  private static CheckResult doAdaptiveCheck(@NotNull StepikWrappers.SubmissionToPostWrapper submission,
                                             int attemptId, int userId) {
    final CloseableHttpClient client = StepikAuthorizedClient.getHttpClient();
    if (client != null) {
      StepikWrappers.ResultSubmissionWrapper wrapper = postResultsForCheck(client, submission);
      if (wrapper != null) {
        wrapper = getCheckResults(client, wrapper, attemptId, userId);
        if (wrapper.submissions.length > 0) {
          final String status = wrapper.submissions[0].status;
          final String hint = wrapper.submissions[0].hint;
          final boolean isSolved = !status.equals("wrong");
          String message = hint;
          if (message.isEmpty() || message.contains(CODE_COMPLEXITY_NOTE)) {
            message = StringUtil.capitalize(status) + " solution";
          }
          return new CheckResult(isSolved ? CheckStatus.Solved : CheckStatus.Failed, message);
        }
        else {
          LOG.warn("Got a submission wrapper with incorrect submissions number: " + wrapper.submissions.length);
        }
      }
      else {
        LOG.warn("Can't do adaptive check: " + "wrapper is null");
        return new CheckResult(CheckStatus.Unchecked, "Can't get check results for Stepik");
      }
    }
    return CheckResult.FAILED_TO_CHECK;
  }

  @Nullable
  private static StepikWrappers.ResultSubmissionWrapper postResultsForCheck(@NotNull final CloseableHttpClient client,
                                                                            @NotNull StepikWrappers.SubmissionToPostWrapper submissionToPostWrapper) {
    final CloseableHttpResponse response;
    try {
      final HttpPost httpPost = new HttpPost(StepikNames.STEPIK_API_URL + StepikNames.SUBMISSIONS);
      setTimeout(httpPost);
      try {
        httpPost.setEntity(new StringEntity(new Gson().toJson(submissionToPostWrapper)));
      }
      catch (UnsupportedEncodingException e) {
        LOG.warn(e.getMessage());
      }
      response = client.execute(httpPost);
      final HttpEntity entity = response.getEntity();
      final String entityString = EntityUtils.toString(entity);
      EntityUtils.consume(entity);
      return new Gson().fromJson(entityString, StepikWrappers.ResultSubmissionWrapper.class);
    }
    catch (IOException e) {
      LOG.warn(e.getMessage());
    }
    return null;
  }

  @NotNull
  private static StepikWrappers.ResultSubmissionWrapper getCheckResults(@NotNull CloseableHttpClient client,
                                                                        @NotNull StepikWrappers.ResultSubmissionWrapper wrapper,
                                                                        int attemptId,
                                                                        int userId) {
    try {
      while (wrapper.submissions.length == 1 && wrapper.submissions[0].status.equals("evaluation")) {
        TimeUnit.MILLISECONDS.sleep(500);
        final URI submissionURI = new URIBuilder(StepikNames.STEPIK_API_URL + StepikNames.SUBMISSIONS)
          .addParameter("attempt", String.valueOf(attemptId))
          .addParameter("order", "desc")
          .addParameter("user", String.valueOf(userId))
          .build();
        final HttpGet httpGet = new HttpGet(submissionURI);
        setTimeout(httpGet);
        final CloseableHttpResponse httpResponse = client.execute(httpGet);
        final HttpEntity entity = httpResponse.getEntity();
        final String entityString = EntityUtils.toString(entity);
        EntityUtils.consume(entity);
        wrapper = new Gson().fromJson(entityString, StepikWrappers.ResultSubmissionWrapper.class);
      }
    }
    catch (InterruptedException | URISyntaxException | IOException e) {
      LOG.warn(e.getMessage());
    }
    return wrapper;
  }

  private static int getAttemptId(@NotNull Task task) throws IOException {
    final StepikWrappers.AdaptiveAttemptWrapper attemptWrapper = new StepikWrappers.AdaptiveAttemptWrapper(task.getStepId());

    final HttpPost post = new HttpPost(StepikNames.STEPIK_API_URL + StepikNames.ATTEMPTS);
    post.setEntity(new StringEntity(new Gson().toJson(attemptWrapper)));

    final CloseableHttpClient client = StepikAuthorizedClient.getHttpClient();
    if (client == null) return -1;
    setTimeout(post);
    final CloseableHttpResponse httpResponse = client.execute(post);
    final int statusCode = httpResponse.getStatusLine().getStatusCode();
    final HttpEntity entity = httpResponse.getEntity();
    final String entityString = EntityUtils.toString(entity);
    EntityUtils.consume(entity);
    if (statusCode == HttpStatus.SC_CREATED) {
      final StepikWrappers.AttemptContainer container =
        new Gson().fromJson(entityString, StepikWrappers.AttemptContainer.class);
      return (container.attempts != null && !container.attempts.isEmpty()) ? container.attempts.get(0).id : -1;
    }
    return -1;
  }
}
