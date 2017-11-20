package com.jetbrains.edu.kotlin.check;

import com.intellij.openapi.project.Project;
import com.jetbrains.edu.learning.checker.CheckListener;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;

public class CheckActionListener implements CheckListener {
    @Override
    public void afterCheck(@NotNull Project project, @NotNull Task task) {
        String taskName = task.getLesson().getName() + "/" + task.getName();
        Assert.assertFalse("Check Task Action failed for " + taskName, task.getStatus() == CheckStatus.Failed);
        Assert.assertFalse("Check Task Action skipped for " + taskName, task.getStatus() == CheckStatus.Unchecked);
        System.out.println("Check for " + taskName + " passed");
    }
}
