// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.jetbrains.edu.learning;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.impl.AbstractProjectViewPSIPane;
import com.intellij.ide.projectView.impl.AbstractProjectViewPane;
import com.intellij.openapi.fileTypes.PlainTextLanguage;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.ProjectViewTestUtil;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.editor.EduEditor;
import com.jetbrains.edu.learning.navigation.NavigationUtils;
import com.jetbrains.edu.learning.projectView.CourseTreeRenderer;
import com.jetbrains.edu.learning.projectView.CourseViewPane;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.IOException;

public class CourseViewTest extends EduTestCase {

  private Course myCourse;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    ProjectViewTestUtil.setupImpl(getProject(), true);
  }

  public void testCoursePane() {
    configureByTaskFile(1, 1, "taskFile1.txt");
    final AbstractProjectViewPSIPane pane = createPane();

    String structure = "-Project\n" +
                       " -CourseNode Edu test course\n" +
                       "  -LessonNode lesson1\n" +
                       "   +TaskNode task1\n" +
                       "   +TaskNode task2\n" +
                       "   +TaskNode task3\n" +
                       "   +TaskNode task4\n";
    PlatformTestUtil.assertTreeEqual(pane.getTree(), structure);
  }

  public void testProjectOpened() {
    EduUtils.openFirstTask(myCourse, getProject());
    ProjectView projectView = ProjectView.getInstance(getProject());
    projectView.changeView(CourseViewPane.Companion.getID());
    String structure = "-Project\n" +
                       " -CourseNode Edu test course\n" +
                       "  -LessonNode lesson1\n" +
                       "   -TaskNode task1\n" +
                       "    taskFile1.txt\n" +
                       "   +TaskNode task2\n" +
                       "   +TaskNode task3\n" +
                       "   +TaskNode task4\n";
    final AbstractProjectViewPane pane = projectView.getCurrentProjectViewPane();
    PlatformTestUtil.waitWhileBusy(pane.getTree());
    PlatformTestUtil.assertTreeEqual(pane.getTree(), structure);
  }

  public void testExpandAfterNavigation() {
    configureByTaskFile(1, 1, "taskFile1.txt");
    ProjectView projectView = ProjectView.getInstance(getProject());
    projectView.changeView(CourseViewPane.Companion.getID());
    navigateToNextTask();

    final AbstractProjectViewPane pane = projectView.getCurrentProjectViewPane();
    String structure = "-Project\n" +
                       " -CourseNode Edu test course\n" +
                       "  -LessonNode lesson1\n" +
                       "   +TaskNode task1\n" +
                       "   -TaskNode task2\n" +
                       "    taskFile2.txt\n" +
                       "   +TaskNode task3\n" +
                       "   +TaskNode task4\n";
    PlatformTestUtil.assertTreeEqual(pane.getTree(), structure);
  }

  private void navigateToNextTask() {
    EduEditor eduEditor = EduUtils.getSelectedStudyEditor(getProject());
    EduState eduState = new EduState(eduEditor);
    assertTrue(eduState.isValid());
    Task targetTask = NavigationUtils.nextTask(eduState.getTask());
    assertNotNull(targetTask);
    NavigationUtils.navigateToTask(getProject(), targetTask);
  }

  public void testCourseProgress() {
    configureByTaskFile(1, 1, "taskFile1.txt");
    ProjectView projectView = ProjectView.getInstance(getProject());
    projectView.changeView(CourseViewPane.Companion.getID());
    final AbstractProjectViewPane pane = projectView.getCurrentProjectViewPane();
    assertInstanceOf(pane, CourseViewPane.class);
    final JTree tree = pane.getTree();
    assertInstanceOf(tree.getCellRenderer(), CourseTreeRenderer.class);
  }

  public void testSwitchingPane() {
    ProjectView projectView = ProjectView.getInstance(getProject());
    projectView.changeView(CourseViewPane.Companion.getID());
    assertEquals(CourseViewPane.Companion.getID(), projectView.getCurrentViewId());
  }

  @Override
  protected void createCourse() throws IOException {
    myFixture.copyDirectoryToProject("lesson1", "lesson1");
    myCourse = new Course();
    myCourse.setLanguage(PlainTextLanguage.INSTANCE.getID());
    myCourse.setName("Edu test course");
    StudyTaskManager.getInstance(myFixture.getProject()).setCourse(myCourse);

    Lesson lesson1 = createLesson(1, 4);
    myCourse.addLesson(lesson1);
    myCourse.initCourse(false);
  }

  @NotNull
  private CourseViewPane createPane() {
    CourseViewPane pane = new CourseViewPane(getProject());
    pane.createComponent();
    Disposer.register(getProject(), pane);
    return pane;
  }

  @NotNull
  @Override
  protected String getTestDataPath() {
    return super.getTestDataPath() + "/projectView";
  }
}
