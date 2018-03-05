package com.jetbrains.edu.learning.checker;

import com.intellij.openapi.project.Project;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;

public class CheckActionListener implements CheckListener {
    private static final Function2<Task, CheckResult, Unit> SHOULD_FAIL = new Function2<Task, CheckResult, Unit>() {
        @Override
        public Unit invoke(@NotNull Task task, @NotNull CheckResult result) {
            String taskName = getTaskName(task);
            Assert.assertFalse("Check Task Action skipped for " + taskName, result.getStatus() == CheckStatus.Unchecked);
            Assert.assertFalse("Check Task Action passed for " + taskName, result.getStatus() == CheckStatus.Solved);
            System.out.println("Checking status for " + taskName + " fails as expected");
            return Unit.INSTANCE;
        }
    };

    private static final Function2<Task, CheckResult, Unit> SHOULD_PASS = new Function2<Task, CheckResult, Unit>() {
        @Override
        public Unit invoke(@NotNull Task task, @NotNull CheckResult result) {
            String taskName = getTaskName(task);
            Assert.assertFalse("Check Task Action skipped for " + taskName, result.getStatus() == CheckStatus.Unchecked);
            Assert.assertFalse("Check Task Action failed for " + taskName, result.getStatus() == CheckStatus.Failed);
            System.out.println("Checking status for " + taskName + " passed");
            return Unit.INSTANCE;
        }
    };

    @NotNull
    private static String getTaskName(@NotNull Task task) {
        return task.getLesson().getName() + "/" + task.getName();
    }

    // Those fields can be modified if some special checks are needed (return true if should run standard checks)
    private static Function2<Task, CheckResult, Unit> checkStatus = SHOULD_PASS;
    private static Function1<Task, String> expectedMessageForTask = null;

    @Override
    public void afterCheck(@NotNull Project project, @NotNull Task task, @NotNull CheckResult result) {
        System.out.println("Check completed. Status: " + result.getStatus() + " , Message: " + result.getMessage());
        checkStatus.invoke(task, result);
        if (expectedMessageForTask != null) {
            String expectedMessage = expectedMessageForTask.invoke(task);
            if (expectedMessage != null) {
                Assert.assertEquals("Checking output for " + getTaskName(task) + " fails", expectedMessage, result.getMessage());
            } else {
                throw new IllegalStateException(String.format("Unexpected task `%s`", task.getName()));
            }

        }
    }

    public static void reset() {
        checkStatus = SHOULD_PASS;
        expectedMessageForTask = null;
    }

    public static void shouldFail() {
        checkStatus = SHOULD_FAIL;
    }

    public static void expectedMessage(@NotNull Function1<Task, String> f) {
        expectedMessageForTask = f;
    }
}
