package com.jetbrains.edu.learning.stepik;

import com.intellij.lang.Language;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.jetbrains.edu.learning.*;
import com.jetbrains.edu.learning.checker.CheckResult;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOption;
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask;
import com.jetbrains.edu.learning.messages.EduCoreBundle;
import com.jetbrains.edu.learning.stepik.api.Attempt;
import com.jetbrains.edu.learning.stepik.api.Dataset;
import com.jetbrains.edu.learning.stepik.api.StepikConnector;
import com.jetbrains.edu.learning.submissions.Submission;
import com.jetbrains.edu.learning.submissions.SubmissionsManager;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.jetbrains.edu.learning.stepik.submissions.StepikBaseSubmissionFactory.createChoiceTaskSubmission;
import static com.jetbrains.edu.learning.stepik.submissions.StepikBaseSubmissionFactory.createCodeTaskSubmission;

public class StepikCheckerConnector {
  private static final Logger LOG = Logger.getInstance(StepikCheckerConnector.class);
  // Stepik uses some code complexity measure, but we agreed that it's not obvious measure and should be improved
  private static final String CODE_COMPLEXITY_NOTE = "code complexity score";

  @SuppressWarnings("unchecked")
  public static Result<Boolean, String> retryChoiceTask(@NotNull ChoiceTask task) {
    final Result<Attempt, String> resultAttempt = StepikConnector.getInstance().postAttempt(task);
    if (resultAttempt instanceof Err) {
      LOG.warn("New attempt is null for " + task.getId());
      return new Err<String>(EduCoreBundle.message("stepik.choice.task.failed.getting.attempt", task.getId()));
    }
    final Attempt attempt = ((Ok<Attempt>)resultAttempt).component1();

    final Dataset dataset = attempt.getDataset();
    if (dataset == null) {
      LOG.warn("Dataset is null for " + task.getId());
      return new Err<String>(EduCoreBundle.message("stepik.choice.task.failed.getting.attempt", task.getId()));
    }
    final List<String> options = dataset.getOptions();
    if (CollectionUtils.isEmpty(options)) {
      LOG.warn("Options is null or empty for " + task.getId());
      return new Err<String>(EduCoreBundle.message("stepik.choice.task.failed.getting.attempt", task.getId()));
    }

    final List<ChoiceOption> choiceOptions = options.stream()
      .map(ChoiceOption::new)
      .collect(Collectors.toList());

    task.setChoiceOptions(choiceOptions);
    task.setSelectedVariants(new ArrayList<>());
    return new Ok<Boolean>(true);
  }

  public static @NotNull CheckResult checkChoiceTask(Project project, @NotNull ChoiceTask task, @NotNull StepikUser user) {
    if (task.getSelectedVariants().isEmpty()) return new CheckResult(CheckStatus.Failed, "No variants selected");
    final StepikConnector stepikConnector = StepikConnector.getInstance();
    final Result<Attempt, String> resultAttempt = stepikConnector.getActiveAttemptOrPostNew(task, false);

    if (resultAttempt instanceof Err) {
      LOG.warn(((Err<String>)resultAttempt).getError());
      return CheckResult.getFailedToCheck();
    }
    final Attempt attempt = ((Ok<Attempt>)resultAttempt).component1();

    final Dataset dataset = attempt.getDataset();
    if (dataset == null) {
      return new CheckResult(CheckStatus.Failed, "Your solution is out of date. Please try again");
    }

    final List<String> options = dataset.getOptions();
    if (options == null) {
      return new CheckResult(CheckStatus.Failed, "Your solution is out of date. Please try again");
    }

    final boolean isActiveAttempt = task.getSelectedVariants().stream()
      .allMatch(index -> options.get(index).equals(task.getChoiceOptions().get(index).getText()));
    if (!isActiveAttempt) {
      return new CheckResult(CheckStatus.Failed, "Your solution is out of date. Please try again");
    }

    final int attemptId = attempt.getId();
    final Submission submission = createChoiceTaskSubmission(task, attempt);
    return doCheck(project, submission, attemptId, user.getId(), task);
  }

  public static CheckResult checkCodeTask(@NotNull Project project, @NotNull Task task, @NotNull StepikUser user) {
    final Result<Attempt, String> postedAttempt = StepikConnector.getInstance().postAttempt(task);
    if (postedAttempt instanceof Err) {
      LOG.warn(((Err<String>)postedAttempt).getError());
      return CheckResult.getFailedToCheck();
    }
    final Attempt attempt = ((Ok<Attempt>)postedAttempt).component1();
    int attemptId = attempt.getId();

    final Course course = task.getLesson().getCourse();
    final Language courseLanguage = course.getLanguageById();
    if (courseLanguage != null) {
      final Editor editor = OpenApiExtKt.getSelectedEditor(project);
      if (editor != null) {
        final String answer = editor.getDocument().getText();
        String defaultLanguage = StepikLanguage.langOfId(courseLanguage.getID(), course.getLanguageVersion()).getLangName();
        assert defaultLanguage != null : ("Default Stepik language not found for: " + courseLanguage.getDisplayName());

        final Submission submission = createCodeTaskSubmission(attempt, answer, defaultLanguage);
        return doCheck(project, submission, attemptId, user.getId(), task);
      }
    }
    return CheckResult.getFailedToCheck();
  }

  private static CheckResult doCheck(
    @NotNull Project project,
    @NotNull Submission submission,
    int attemptId,
    int userId,
    @NotNull Task task
  ) {
    Submission postedSubmission = postSubmission(submission, attemptId, userId);
    if (postedSubmission != null) {
      if (task instanceof CodeTask) {
        SubmissionsManager.getInstance(project).addToSubmissions(task.getId(), postedSubmission);
      }
      final String status = postedSubmission.getStatus();
      if (status == null) return CheckResult.getFailedToCheck();
      final String hint = postedSubmission.getHint();
      final boolean isSolved = !status.equals(EduNames.WRONG);
      String message = hint;
      if (message == null || message.isEmpty() || message.contains(CODE_COMPLEXITY_NOTE)) {
        message = StringUtil.capitalize(status) + " solution";
      }
      return new CheckResult(isSolved ? CheckStatus.Solved : CheckStatus.Failed, message);
    }
    else {
      LOG.warn("Can't perform check: submission is null");
      return new CheckResult(CheckStatus.Unchecked, "Can't get check results for Stepik");
    }
  }

  private static Submission postSubmission(@NotNull Submission submission, int attemptId, int userId) {
    final Result<Submission, String> sentSubmission = StepikConnector.getInstance().postSubmission(submission);
    if (sentSubmission instanceof Err) {
      return null;
    }
    Submission postedSubmission = ((Ok<Submission>)sentSubmission).component1();
    try {
      String status = postedSubmission.getStatus();
      while ("evaluation".equals(status)) {
        TimeUnit.MILLISECONDS.sleep(500);
        postedSubmission = StepikConnector.getInstance().getSubmission(attemptId, userId);
        if (postedSubmission == null) break;
        status = postedSubmission.getStatus();
      }
    }
    catch (InterruptedException e) {
      LOG.warn(e.getMessage());
    }
    return postedSubmission;
  }
}
