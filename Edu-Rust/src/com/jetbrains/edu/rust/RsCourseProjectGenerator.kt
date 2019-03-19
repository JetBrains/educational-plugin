package com.jetbrains.edu.rust

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import org.rust.cargo.CargoConstants
import org.rust.cargo.project.model.cargoProjects
import org.rust.cargo.project.settings.rustSettings
import org.rust.openapiext.pathAsPath

class RsCourseProjectGenerator(builder: RsCourseBuilder, course: Course) :
    CourseProjectGenerator<RsProjectSettings>(builder, course) {

    override fun afterProjectGenerated(project: Project, projectSettings: RsProjectSettings) {
        super.afterProjectGenerated(project, projectSettings)
        project.rustSettings.modify {
            it.toolchain = projectSettings.toolchain
        }
        myCourse.visitLessons { lesson ->
            for (task in lesson.taskList) {
                val manifestFile = task.getTaskDir(project)?.findChild(CargoConstants.MANIFEST_FILE) ?: continue
                project.cargoProjects.attachCargoProject(manifestFile.pathAsPath)
            }
            true
        }
    }
}
