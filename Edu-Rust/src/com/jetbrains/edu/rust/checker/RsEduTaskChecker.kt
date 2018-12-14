package com.jetbrains.edu.rust.checker

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.checker.TaskChecker
import com.jetbrains.edu.learning.checker.TestsOutputParser
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import org.rust.cargo.project.model.cargoProjects
import org.rust.cargo.project.settings.rustSettings
import org.rust.cargo.toolchain.CargoCommandLine
import org.rust.openapiext.execute

class RsEduTaskChecker(project: Project, task: EduTask) : TaskChecker<EduTask>(task, project) {

    private val parser: JsonParser = JsonParser()

    override fun check(indicator: ProgressIndicator): CheckResult {
        val taskDir = task.getTaskDir(project) ?: return CheckResult.FAILED_TO_CHECK
        val cargoProject = project.cargoProjects.findProjectForFile(taskDir) ?: return CheckResult.FAILED_TO_CHECK
        val cargo = project.rustSettings.toolchain?.rawCargo() ?: return CheckResult.FAILED_TO_CHECK
        val cmd = CargoCommandLine.forProject(cargoProject, "test", listOf(
            "--", "-Z", "unstable-options", "--format=json"
        ))
        val processOutput = cargo.toGeneralCommandLine(cmd).execute(project)
        for (line in processOutput.stdoutLines) {
            if (line.trimStart().startsWith("error: Could not compile")) {
                return CheckResult(CheckStatus.Failed, CheckUtils.COMPILATION_FAILED_MESSAGE, processOutput.stdout)
            }
            val jsonObject = try {
                parser.parse(line) as? JsonObject ?: continue
            } catch (e: JsonSyntaxException) {
                continue
            }
            val testMessage = LibtestTestMessage.fromJson(jsonObject) ?: continue
            if (testMessage.event == "failed") {
                return CheckResult(CheckStatus.Failed, testMessage.stdout ?: "")
            }
        }

        return CheckResult(CheckStatus.Solved, TestsOutputParser.CONGRATULATIONS)
    }
}

private data class LibtestTestMessage(
  val type: String,
  val event: String,
  val name: String,
  val stdout: String?
) {
    companion object {
        fun fromJson(json: JsonObject): LibtestTestMessage? {
            if (json.getAsJsonPrimitive("type")?.asString != "test") return null
            return Gson().fromJson(json, LibtestTestMessage::class.java)
        }
    }
}

