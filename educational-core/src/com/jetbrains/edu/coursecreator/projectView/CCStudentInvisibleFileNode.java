package com.jetbrains.edu.coursecreator.projectView;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.PsiFileNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.ui.SimpleTextAttributes;
import com.jetbrains.edu.coursecreator.AdditionalFilesUtils;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.VirtualFileExt;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.messages.EduCoreBundle;
import com.jetbrains.edu.learning.projectView.CourseViewUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

/**
 * Add to the file name postfix "course.creator.course.view.excluded" from EduCoreBundle.properties
 * if the file in {@link COURSE_IGNORE}
 */
public class CCStudentInvisibleFileNode extends PsiFileNode {
  private final String myName;

  public CCStudentInvisibleFileNode(Project project,
                                    PsiFile value,
                                    ViewSettings viewSettings) {
    this(project, value, viewSettings, value.getName());
  }

  public CCStudentInvisibleFileNode(Project project,
                                    PsiFile value,
                                    ViewSettings viewSettings,
                                    String name) {
    super(project, value, viewSettings);
    VirtualFile file = value.getVirtualFile();
    Course course = StudyTaskManager.getInstance(project).getCourse();
    boolean isExcluded = file != null && course != null &&
                         (VirtualFileExt.canBeAddedToTask(file, project) ||
                          AdditionalFilesUtils.isExcluded(file, null, course, project));
    myName = isExcluded ? excludedName(name) : name;
  }

  @Nls
  private static String excludedName(String name) {
    return EduCoreBundle.message("course.creator.course.view.excluded", name);
  }

  @Override
  protected void updateImpl(@NotNull PresentationData data) {
    super.updateImpl(data);
    data.clearText();
    data.addText(myName, SimpleTextAttributes.GRAY_ATTRIBUTES);
  }

  @SuppressWarnings("deprecation")
  @TestOnly
  @Override
  public String getTestPresentation() {
    return CourseViewUtils.testPresentation(this);
  }
}
