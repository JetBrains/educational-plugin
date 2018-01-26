/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.edu.learning.newproject;

import com.intellij.facet.ui.ValidationResult;
import com.intellij.ide.util.projectWizard.AbstractNewProjectStep;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.DirectoryProjectGenerator;
import com.jetbrains.edu.learning.EduSettings;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.RemoteCourse;
import com.jetbrains.edu.learning.stepik.StepikConnector;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public abstract class CourseProjectGenerator<S> implements DirectoryProjectGenerator<S> {
  @NotNull protected Course myCourse;

  public CourseProjectGenerator(@NotNull final Course course) {
    myCourse = course;
  }

  protected boolean beforeProjectGenerated() {
    if (!(myCourse instanceof RemoteCourse)) return true;
    final RemoteCourse remoteCourse = (RemoteCourse) this.myCourse;
    if (remoteCourse.getId() > 0) {
      return ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
        ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);
        EduUtils.execCancelable(() -> StepikConnector.enrollToCourse(remoteCourse.getId(), EduSettings.getInstance().getUser()));
        RemoteCourse loadedCourse = EduUtils.execCancelable(() -> StepikConnector.getCourse(null, remoteCourse));
        if (loadedCourse != null) {
          myCourse = loadedCourse;
          return true;
        } else {
          return false;
        }
      }, "Creating Course", true, ProjectManager.getInstance().getDefaultProject());
    }
    return true;
  }

  protected void afterProjectGenerated(@NotNull Project project, @NotNull S projectSettings) {
  }

  /**
   * Generate new project and create course structure for created project
   *
   * @param location location of new course project
   * @param projectSettings new project settings
   */
  // 'projectSettings' must have S type but due to some reasons:
  //  * We don't know generic parameter of EduPluginConfigurator after it was gotten through extension point mechanism
  //  * Kotlin and Java do type erasure a little bit differently
  // we use Object instead of S and cast to S when it needed
  @SuppressWarnings("unchecked")
  @Nullable
  public final Project doCreateCourseProject(@NotNull String location, @NotNull Object projectSettings) {
    if (!beforeProjectGenerated()) {
      return null;
    }
    Project createdProject = createProject(location, projectSettings);
    if (createdProject == null) return null;
    afterProjectGenerated(createdProject, (S) projectSettings);
    return createdProject;
  }

  /**
   * Create new project in given location.
   * To create course structure: modules, folders, files, etc. use {@link CourseProjectGenerator#createCourseStructure(Project, VirtualFile, Object)}
   *
   * @param location location of new project
   * @param projectSettings new project settings
   * @return project of new course or null if new project can't be created
   */
  @Nullable
  protected Project createProject(@NotNull String location, @NotNull Object projectSettings) {
    return AbstractNewProjectStep.doGenerateProject(null, location, this, projectSettings);
  }

  /**
   * Callback method of {@link DirectoryProjectGenerator} to generate project structure. <b>Don't use</b> it explicitly.
   * If you want to generate course structure, just call {@link CourseProjectGenerator#createCourseStructure}
   */
  @Override
  public final void generateProject(@NotNull Project project, @NotNull VirtualFile baseDir,
                                    @NotNull S settings, @NotNull Module module) {
    createCourseStructure(project, baseDir, settings);
  }

  /**
   * Create course structure for already created project.
   *
   * @param project course project
   * @param baseDir base directory of project
   * @param settings project settings
   */
  abstract protected void createCourseStructure(@NotNull Project project, @NotNull VirtualFile baseDir, @NotNull S settings);

  @Nls
  @NotNull
  @Override
  public String getName() {
    return "";
  }

  @Nullable
  @Override
  public Icon getLogo() {
    return null;
  }

  @NotNull
  @Override
  public ValidationResult validate(@NotNull String baseDirPath) {
    return ValidationResult.OK;
  }
}
