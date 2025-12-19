@file:Repository("https://repo.maven.apache.org/maven2/")
@file:Import("shared-utils.main.kts")

import kotlin.system.exitProcess

val versionParam = args.firstOrNull() ?: run {
  println("Error: VERSION_PARAM is required. Pass the parameter that should be used to store the version as the first argument.\nUsage: kotlinc -script get-current-release-version.main.kts <VERSION_PARAM>")
  exitProcess(1)
}


val version = getPluginVersion()

if (version != null) {
  // Set TeamCity parameter using service message
  println("##teamcity[setParameter name='$versionParam' value='$version']")
  println("Plugin version: $version")
}
else {
  System.err.println("Could not determine project version")
  exitProcess(1)
}
