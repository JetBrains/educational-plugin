package com.jetbrains.edu.learning.stepik;

import com.google.gson.*;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageCommenters;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.checker.CheckResult;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.tasks.ChoiceTask;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class StepikCheckerConnector {
  public static final String EDU_TOOLS_COMMENT = " Posted from EduTools plugin\n";
  private static final Logger LOG = Logger.getInstance(StepikCheckerConnector.class);
  private static final int CONNECTION_TIMEOUT = 60 * 1000;
  // Stepik uses some code complexity measure, but we agreed that it's not obvious measure and should be improved
  private static final String CODE_COMPLEXITY_NOTE = "code complexity score";

  @Nullable
  public static StepikWrappers.AttemptWrapper.Attempt getAttemptForStep(int stepId, int userId) {
    try {
      final List<StepikWrappers.AttemptWrapper.Attempt> attempts = getAttempts(stepId, userId);
      if (attempts != null && attempts.size() > 0) {
        final StepikWrappers.AttemptWrapper.Attempt attempt = attempts.get(0);
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

  private static StepikWrappers.AttemptWrapper.Attempt createNewAttempt(int id) throws IOException {
    final String response = StepikConnector.postAttempt(id);
    final StepikWrappers.AttemptContainer attempt = new Gson().fromJson(response, StepikWrappers.AttemptContainer.class);
    return attempt.attempts.get(0);
  }

  @Nullable
  private static List<StepikWrappers.AttemptWrapper.Attempt> getAttempts(int stepId, int userId)
    throws URISyntaxException, IOException {
    final URI attemptUrl = new URIBuilder(StepikNames.ATTEMPTS)
      .addParameter("step", String.valueOf(stepId))
      .addParameter("user", String.valueOf(userId))
      .build();
    final StepikWrappers.AttemptContainer attempt =
      StepikAuthorizedClient.getFromStepik(attemptUrl.toString(), StepikWrappers.AttemptContainer.class);
    return attempt == null ? null : attempt.attempts;
  }

  private static void setTimeout(HttpRequestBase request) {
    final RequestConfig requestConfig = RequestConfig.custom()
      .setConnectionRequestTimeout(CONNECTION_TIMEOUT)
      .setConnectTimeout(CONNECTION_TIMEOUT)
      .setSocketTimeout(CONNECTION_TIMEOUT)
      .build();
    request.setConfig(requestConfig);
  }

  public static CheckResult checkChoiceTask(@NotNull ChoiceTask task, @NotNull StepikUser user) {
    if (task.getSelectedVariants().isEmpty()) return new CheckResult(CheckStatus.Failed, "No variants selected");
    final StepikWrappers.AttemptWrapper.Attempt attempt = getAttemptForStep(task.getStepId(), user.getId());

    if (attempt != null) {
      final int attemptId = attempt.id;

      final boolean isActiveAttempt = task.getSelectedVariants().stream()
        .allMatch(index -> attempt.dataset.options.get(index).equals(task.getChoiceVariants().get(index)));
      if (!isActiveAttempt) return new CheckResult(CheckStatus.Failed, "Your solution is out of date. Please try again");
      final StepikWrappers.SubmissionToPostWrapper wrapper = new StepikWrappers.SubmissionToPostWrapper(String.valueOf(attemptId),
                                                                                                        createChoiceTaskAnswerArray(task));
      final CheckResult result = doCheck(wrapper, attemptId, user.getId());
      if (result.getStatus() == CheckStatus.Failed) {
        try {
          createNewAttempt(task.getStepId());
          StepikWrappers.StepSource step = StepikConnector.getStep(task.getStepId());
          Course course = task.getLesson().getCourse();
          StepikTaskBuilder taskBuilder = new StepikTaskBuilder(course.getLanguageById(), task.getName(),
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

  public static CheckResult checkCodeTask(@NotNull Project project, @NotNull Task task, @NotNull StepikUser user) {
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
        return doCheck(submissionToPost, attemptId, user.getId());
      }
    }
    else {
      LOG.warn("Got an incorrect attempt id: " + attemptId);
    }
    return CheckResult.FAILED_TO_CHECK;
  }

  private static CheckResult doCheck(@NotNull StepikWrappers.SubmissionToPostWrapper submission,
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
        LOG.warn("Can't perform check: wrapper is null");
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
    final StepikWrappers.AttemptWrapper attemptWrapper = new StepikWrappers.AttemptWrapper(task.getStepId());

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
        new GsonBuilder().registerTypeAdapter(StepikWrappers.AttemptWrapper.Dataset.class, new DatasetAdapter())
          .create().fromJson(entityString, StepikWrappers.AttemptContainer.class);
      return (container.attempts != null && !container.attempts.isEmpty()) ? container.attempts.get(0).id : -1;
    }
    return -1;
  }

  public static class DatasetAdapter implements JsonDeserializer<StepikWrappers.AttemptWrapper.Dataset> {
    @Override
    public StepikWrappers.AttemptWrapper.Dataset deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
      Gson gson = new Gson();
      if (json instanceof JsonPrimitive) {
        return null;
      }
      return gson.fromJson(json, StepikWrappers.AttemptWrapper.Dataset.class);
    }
  }
}
