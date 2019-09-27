package com.jetbrains.edu.learning.stepik;

import com.intellij.lang.Language;
import com.intellij.lang.LanguageCommenters;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.checker.CheckResult;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOption;
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask;
import com.jetbrains.edu.learning.stepik.api.*;
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView;
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
  public static Attempt getAttemptForStep(int stepId, int userId) {
    final List<Attempt> attempts = StepikConnector.getInstance().getAttempts(stepId, userId);
    if (attempts != null && attempts.size() > 0) {
      final Attempt attempt = attempts.get(0);
      return attempt.isActive() ? attempt : StepikConnector.getInstance().postAttempt(stepId);
    }
    else {
      return StepikConnector.getInstance().postAttempt(stepId);
    }
  }

  public static CheckResult checkChoiceTask(Project project, @NotNull ChoiceTask task, @NotNull StepikUser user) {
    if (task.getSelectedVariants().isEmpty()) return new CheckResult(CheckStatus.Failed, "No variants selected");
    final Attempt attempt = getAttemptForStep(task.getId(), user.getId());

    if (attempt != null) {
      final int attemptId = attempt.getId();
      final Dataset dataset = attempt.getDataset();
      if (dataset == null) return new CheckResult(CheckStatus.Failed, "Your solution is out of date. Please try again");
      final List<String> options = dataset.getOptions();
      if (options == null) return new CheckResult(CheckStatus.Failed, "Your solution is out of date. Please try again");
      final boolean isActiveAttempt = task.getSelectedVariants().stream()
        .allMatch(index -> options.get(index).equals(task.getChoiceOptions().get(index).getText()));
      if (!isActiveAttempt) return new CheckResult(CheckStatus.Failed, "Your solution is out of date. Please try again");
      final SubmissionData submissionData = createChoiceSubmissionData(task, attemptId);

      final CheckResult result = doCheck(submissionData, project, attemptId, user.getId(), task);
      if (result.getStatus() == CheckStatus.Failed) {
        StepikConnector.getInstance().postAttempt(task.getId());
        StepSource step = StepikConnector.getInstance().getStep(task.getId());
        if (step == null) {
          LOG.error("Failed to get step " + task.getId());
          return result;
        }
        Course course = task.getLesson().getCourse();
        StepikTaskBuilder taskBuilder = new StepikTaskBuilder(course, task.getLesson(), step, task.getId(), user.getId());
        final Step block = step.getBlock();
        if (block != null) {
          final Task updatedTask = taskBuilder.createTask(block.getName());
          if (updatedTask instanceof ChoiceTask) {
            final List<ChoiceOption> choiceOptions = ((ChoiceTask)updatedTask).getChoiceOptions();
            task.setChoiceOptions(choiceOptions);
            task.setSelectedVariants(new ArrayList<>());
          }
        }
      }
      return result;
    }

    return CheckResult.FAILED_TO_CHECK;
  }

  @NotNull
  private static SubmissionData createChoiceSubmissionData(@NotNull ChoiceTask task, int attemptId) {
    final SubmissionData submissionData = new SubmissionData();
    submissionData.submission = new Submission();
    submissionData.submission.setAttempt(attemptId);
    final Reply reply = new Reply();
    reply.setChoices(createChoiceTaskAnswerArray(task));
    submissionData.submission.setReply(reply);
    return submissionData;
  }

  @NotNull
  private static SubmissionData createCodeSubmissionData(int attemptId, String language, String answer) {
    final SubmissionData submissionData = new SubmissionData();
    submissionData.submission = createCodeSubmission(attemptId, language, answer);
    return submissionData;
  }

  public static Submission createCodeSubmission(int attemptId, String language, String answer) {
    Submission submission = new Submission();
    submission.setAttempt(attemptId);
    final Reply reply = new Reply();
    reply.setLanguage(language);
    reply.setCode(answer);
    submission.setReply(reply);
    return submission;
  }

  private static boolean[] createChoiceTaskAnswerArray(@NotNull ChoiceTask task) {
    final List<Integer> selectedVariants = task.getSelectedVariants();
    final boolean[] answer = new boolean[task.getChoiceOptions().size()];
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
        return doCheck(submissionData, project, attemptId, user.getId(), task);
      }
    }
    else {
      LOG.warn("Got an incorrect attempt id: " + attemptId);
    }
    return CheckResult.FAILED_TO_CHECK;
  }

  private static CheckResult doCheck(@NotNull SubmissionData submissionData,
                                     Project project, int attemptId, int userId, Task task) {
    Submission submission = postSubmission(submissionData, attemptId, userId);
    if (submission != null) {
      if (task instanceof CodeTask) {
        SubmissionsManager.addToSubmissions(task.getId(), submission);
        ApplicationManager.getApplication().invokeLater(() -> TaskDescriptionView.getInstance(project).updateAdditionalTaskTab());
      }
      final String status = submission.getStatus();
      if (status == null) return CheckResult.FAILED_TO_CHECK;
      final String hint = submission.getHint();
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

  private static Submission postSubmission(@NotNull SubmissionData submissionData, int attemptId, int userId) {
    Submission submission = StepikConnector.getInstance().postSubmission(submissionData);
    if (submission == null) {
      return null;
    }
    try {
      String status = submission.getStatus();
      while ("evaluation".equals(status)) {
        TimeUnit.MILLISECONDS.sleep(500);
        submission = StepikConnector.getInstance().getSubmission(attemptId, userId);
        if (submission == null) break;
        status = submission.getStatus();
      }
    }
    catch (InterruptedException e) {
      LOG.warn(e.getMessage());
    }
    return submission;
  }

  private static int getAttemptId(@NotNull Task task) {
    final Attempt attempt = StepikConnector.getInstance().postAttempt(task.getId());
    return attempt != null ? attempt.getId() : -1;
  }
}
