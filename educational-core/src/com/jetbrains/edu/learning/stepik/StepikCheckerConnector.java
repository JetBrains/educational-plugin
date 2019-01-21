package com.jetbrains.edu.learning.stepik;

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
import com.jetbrains.edu.learning.stepik.api.StepikConnector;
import com.jetbrains.edu.learning.stepik.api.SubmissionData;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class StepikCheckerConnector {
  public static final String EDU_TOOLS_COMMENT = " Posted from EduTools plugin\n";
  private static final Logger LOG = Logger.getInstance(StepikCheckerConnector.class);
  // Stepik uses some code complexity measure, but we agreed that it's not obvious measure and should be improved
  private static final String CODE_COMPLEXITY_NOTE = "code complexity score";

  @Nullable
  public static StepikWrappers.Attempt getAttemptForStep(int stepId, int userId) {
    final List<StepikWrappers.Attempt> attempts = StepikConnector.INSTANCE.getAttempts(stepId, userId);
    if (attempts != null && attempts.size() > 0) {
      final StepikWrappers.Attempt attempt = attempts.get(0);
      return attempt.isActive() ? attempt : StepikConnector.INSTANCE.postAttempt(stepId);
    }
    else {
      return StepikConnector.INSTANCE.postAttempt(stepId);
    }
  }

  public static CheckResult checkChoiceTask(@NotNull ChoiceTask task, @NotNull StepikUser user) {
    if (task.getSelectedVariants().isEmpty()) return new CheckResult(CheckStatus.Failed, "No variants selected");
    final StepikWrappers.Attempt attempt = getAttemptForStep(task.getStepId(), user.getId());

    if (attempt != null) {
      final int attemptId = attempt.id;

      final boolean isActiveAttempt = task.getSelectedVariants().stream()
        .allMatch(index -> attempt.dataset.options.get(index).equals(task.getChoiceVariants().get(index)));
      if (!isActiveAttempt) return new CheckResult(CheckStatus.Failed, "Your solution is out of date. Please try again");
      final SubmissionData submissionData = createChoiceSubmissionData(task, attemptId);

      final CheckResult result = doCheck(submissionData, attemptId, user.getId());
      if (result.getStatus() == CheckStatus.Failed) {
        StepikConnector.INSTANCE.postAttempt(task.getStepId());
        StepSource step = StepikConnector.INSTANCE.getStep(task.getStepId());
        if (step == null) {
          LOG.error("Failed to get step " + task.getStepId());
          return result;
        }
        Course course = task.getLesson().getCourse();
        StepikTaskBuilder taskBuilder = new StepikTaskBuilder(course.getLanguageById(), task.getLesson(), task.getName(),
                                                              step, task.getStepId(), user.getId());
        final Task updatedTask = taskBuilder.createTask(step.getBlock().getName());
        if (updatedTask instanceof ChoiceTask) {
          final List<String> variants = ((ChoiceTask)updatedTask).getChoiceVariants();
          task.setChoiceVariants(variants);
          task.setSelectedVariants(new ArrayList<>());
        }
      }
      return result;
    }

    return CheckResult.FAILED_TO_CHECK;
  }

  @NotNull
  private static SubmissionData createChoiceSubmissionData(@NotNull ChoiceTask task, int attemptId) {
    final SubmissionData submissionData = new SubmissionData();
    submissionData.submission = new StepikWrappers.Submission();
    submissionData.submission.attempt = attemptId;
    submissionData.submission.reply = new StepikWrappers.Reply();
    submissionData.submission.reply.choices = createChoiceTaskAnswerArray(task);
    return submissionData;
  }

  @NotNull
  private static SubmissionData createCodeSubmissionData(int attemptId, String language, String answer) {
    final SubmissionData submissionData = new SubmissionData();
    submissionData.submission = new StepikWrappers.Submission();
    submissionData.submission.attempt = attemptId;
    submissionData.submission.reply = new StepikWrappers.Reply();
    submissionData.submission.reply.language = language;
    submissionData.submission.reply.code = answer;
    return submissionData;
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
    int attemptId = getAttemptId(task);
    if (attemptId != -1) {
      Course course = task.getLesson().getCourse();
      Language courseLanguage = course.getLanguageById();
      final Editor editor = EduUtils.getSelectedEditor(project);
      if (editor != null) {
        String commentPrefix = LanguageCommenters.INSTANCE.forLanguage(courseLanguage).getLineCommentPrefix();
        final String answer = commentPrefix + EDU_TOOLS_COMMENT + editor.getDocument().getText();
        String defaultLanguage = StepikLanguages.langOfId(courseLanguage.getID()).getLangName();
        assert defaultLanguage != null : ("Default Stepik language not found for: " + courseLanguage.getDisplayName());

        final SubmissionData submissionData = createCodeSubmissionData(attemptId, defaultLanguage, answer);
        return doCheck(submissionData, attemptId, user.getId());
      }
    }
    else {
      LOG.warn("Got an incorrect attempt id: " + attemptId);
    }
    return CheckResult.FAILED_TO_CHECK;
  }

  private static CheckResult doCheck(@NotNull SubmissionData submission,
                                     int attemptId, int userId) {
    final CloseableHttpClient client = StepikAuthorizedClient.getHttpClient();
    if (client != null) {
      List<StepikWrappers.Submission> submissions = StepikConnector.INSTANCE.postSubmission(submission);
      if (submissions != null) {
        submissions = getCheckResults(submissions, attemptId, userId);
        if (submissions.size() > 0) {
          final String status = submissions.get(0).status;
          final String hint = submissions.get(0).hint;
          final boolean isSolved = !status.equals("wrong");
          String message = hint;
          if (message.isEmpty() || message.contains(CODE_COMPLEXITY_NOTE)) {
            message = StringUtil.capitalize(status) + " solution";
          }
          return new CheckResult(isSolved ? CheckStatus.Solved : CheckStatus.Failed, message);
        }
        else {
          LOG.warn("Got a submission wrapper with incorrect submissions number: " + submissions.size());
        }
      }
      else {
        LOG.warn("Can't perform check: wrapper is null");
        return new CheckResult(CheckStatus.Unchecked, "Can't get check results for Stepik");
      }
    }
    return CheckResult.FAILED_TO_CHECK;
  }

  private static List<StepikWrappers.Submission> getCheckResults(@NotNull List<StepikWrappers.Submission> submissions,
                                                                 int attemptId,
                                                                 int userId) {
    try {
      while (submissions != null && submissions.size() == 1 && submissions.get(0).status.equals("evaluation")) {
        TimeUnit.MILLISECONDS.sleep(500);
        submissions = StepikConnector.INSTANCE.getSubmissions(attemptId, userId);
      }
    }
    catch (InterruptedException e) {
      LOG.warn(e.getMessage());
    }
    return submissions;
  }

  private static int getAttemptId(@NotNull Task task) {
    final StepikWrappers.Attempt attempt = StepikConnector.INSTANCE.postAttempt(task.getStepId()); //TODO: DatasetAdapter
    return attempt != null ? attempt.id : -1;
  }
}
