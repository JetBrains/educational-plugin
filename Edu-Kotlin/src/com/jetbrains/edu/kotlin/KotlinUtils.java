package com.jetbrains.edu.kotlin;

import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.StudyStatus;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import org.jetbrains.annotations.NotNull;

public class KotlinUtils {
    static int calculateTaskNumber(@NotNull final Task solvedTask) {
        Lesson lesson = solvedTask.getLesson();
        Course course = lesson.getCourse();
        int solvedTaskNumber = 0;
        for (Lesson currentLesson: course.getLessons()) {
            for (Task task: currentLesson.getTaskList()) {
                if(task.getStatus() == StudyStatus.Solved) {
                    solvedTaskNumber++;
                }
            }
        }
        return solvedTaskNumber;
    }
}
