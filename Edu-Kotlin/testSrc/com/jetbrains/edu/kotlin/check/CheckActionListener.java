package com.jetbrains.edu.kotlin.check;

import com.intellij.openapi.project.Project;
import com.jetbrains.edu.learning.checker.CheckListener;
import com.jetbrains.edu.learning.checker.CheckResult;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;

public class CheckActionListener implements CheckListener {
    public static final Function1<Task, Unit> SHOULD_FAIL = new Function1<Task, Unit>() {
        @Override
        public Unit invoke(Task task) {
            String taskName = task.getLesson().getName() + "/" + task.getName();
            Assert.assertFalse("Check Task Action skipped for " + taskName, task.getStatus() == CheckStatus.Unchecked);
            Assert.assertFalse("Check Task Action passed for " + taskName, task.getStatus() == CheckStatus.Solved);
            System.out.println("Check for " + taskName + " fails as expected");
            return Unit.INSTANCE;
        }
    };

    public static final Function1<Task, Unit> SHOULD_PASS = new Function1<Task, Unit>() {
        @Override
        public Unit invoke(Task task) {
            String taskName = task.getLesson().getName() + "/" + task.getName();
            Assert.assertFalse("Check Task Action skipped for " + taskName, task.getStatus() == CheckStatus.Unchecked);
            Assert.assertFalse("Check Task Action failed for " + taskName, task.getStatus() == CheckStatus.Failed);
            System.out.println("Check for " + taskName + " passed");
            return Unit.INSTANCE;
        }
    };

    // This field can be modified if some special checks are needed (return true if should run standard checks)
    @SuppressWarnings("StaticNonFinalField")
    public static Function1<Task, Unit> afterCheck = SHOULD_PASS;

    @Override
    public void afterCheck(@NotNull Project project, @NotNull Task task, @NotNull CheckResult result) {
        afterCheck.invoke(task);
    }

    public static void reset() {
        afterCheck = SHOULD_PASS;
    }
}
