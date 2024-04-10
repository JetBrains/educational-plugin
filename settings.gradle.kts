import java.io.IOException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.*

rootProject.name = "educational-plugin"
include(
  "edu-format",
  "intellij-plugin",
  "intellij-plugin:educational-core",
  "intellij-plugin:code-insight",
  "intellij-plugin:code-insight:html",
  "intellij-plugin:code-insight:markdown",
  "intellij-plugin:code-insight:yaml",
  "intellij-plugin:jvm-core",
  "intellij-plugin:remote-env",
  "intellij-plugin:Edu-Java",
  "intellij-plugin:Edu-Kotlin",
  "intellij-plugin:Edu-Scala",
  "intellij-plugin:Edu-Python",
  "intellij-plugin:Edu-Python:Idea", // python support for IDEA and Android Studio
  "intellij-plugin:Edu-Python:PyCharm", // python support for PyCharm and CLion
  "intellij-plugin:Edu-Android",
  "intellij-plugin:Edu-JavaScript",
  "intellij-plugin:Edu-Rust",
  "intellij-plugin:Edu-Cpp",
  "intellij-plugin:Edu-CSharp",
  "intellij-plugin:Edu-Go",
  "intellij-plugin:Edu-Php",
  "intellij-plugin:Edu-Shell",
  "intellij-plugin:sql",
  "intellij-plugin:sql:sql-jvm",
  "intellij-plugin:github"
)

if (settings.providers.gradleProperty("fleetIntegration").get().toBoolean()) {
  include("fleet-plugin")
}

apply(from = "common.gradle.kts")

val secretProperties: String by extra
val inJetBrainsNetwork: () -> Boolean by extra

val isTeamCity: Boolean get() = System.getenv("TEAMCITY_VERSION") != null

configureSecretProperties()

downloadHyperskillCss()

fun configureSecretProperties() {
  if (inJetBrainsNetwork() || isTeamCity) {
    download(URL("https://repo.labs.intellij.net/edu-tools/secret.properties"), secretProperties)
  }
  else {
    val secretProperties = file(secretProperties)
    if (!secretProperties.exists()) {
      secretProperties.createNewFile()
    }
  }

  val secretProperties = loadProperties(secretProperties)

  secretProperties.extractAndStore(
    "intellij-plugin/educational-core/resources/stepik/stepik.properties",
    "stepikClientId",
    "stepikClientSecret",
    "stepikNonProductionClientId",
    "stepikNonProductionClientSecret",
    "cogniterraClientId",
    "cogniterraClientSecret"
  )
  secretProperties.extractAndStore(
    "intellij-plugin/educational-core/resources/hyperskill/hyperskill-oauth.properties",
    "hyperskillClientId",
    "hyperskillClientSecret"
  )
  secretProperties.extractAndStore(
    "intellij-plugin/educational-core/resources/twitter/oauth_twitter.properties",
    "twitterConsumerKey",
    "twitterConsumerSecret"
  )
  secretProperties.extractAndStore(
    "intellij-plugin/Edu-Python/resources/checkio/py-checkio-oauth.properties",
    "pyCheckioClientId",
    "pyCheckioClientSecret"
  )
  secretProperties.extractAndStore(
    "intellij-plugin/Edu-JavaScript/resources/checkio/js-checkio-oauth.properties",
    "jsCheckioClientId",
    "jsCheckioClientSecret"
  )
  secretProperties.extractAndStore(
    "edu-format/resources/aes/aes.properties",
    "aesKey"
  )
  secretProperties.extractAndStore(
    "intellij-plugin/educational-core/resources/marketplace/marketplace-oauth.properties",
    "eduHubClientId",
    "eduHubClientSecret",
    "marketplaceHubClientId"
  )

  if (inJetBrainsNetwork() && !isTeamCity) {
    // This token should be available only with the plugin being built locally
    secretProperties.extractAndStore(
      "intellij-plugin/educational-core/resources/ai/ai.properties",
      "grazieTemporaryToken"
    )
  }
}

fun downloadHyperskillCss() {
  try {
    download(URL("https://hyperskill.org/static/shared.css"), "intellij-plugin/educational-core/resources/style/hyperskill_task.css")
  }
  catch (e: IOException) {
    System.err.println("Error downloading: ${e.message}. Using local copy")
    Files.copy(
      Paths.get("intellij-plugin/hyperskill_default.css"),
      Paths.get("intellij-plugin/educational-core/resources/style/hyperskill_task.css"),
      StandardCopyOption.REPLACE_EXISTING
    )
  }
}

fun download(url: URL, dstPath: String) {
  println("Download $url")

  url.openStream().use {
    val path = file(dstPath).toPath().toAbsolutePath()
    println("Copying file to $path")
    Files.copy(it, path, StandardCopyOption.REPLACE_EXISTING)
  }
}

fun loadProperties(path: String): Properties {
  val properties = Properties()
  file(path).bufferedReader().use { properties.load(it) }
  return properties
}

fun Properties.extractAndStore(path: String, vararg keys: String) {
  val properties = Properties()
  for (key in keys) {
    properties[key] = getProperty(key) ?: ""
  }
  val file = file(path)
  file.parentFile?.mkdirs()
  file.bufferedWriter().use { properties.store(it, "") }
}

buildCache {
  local {
    isEnabled = !isTeamCity
    // By default, build cache is stored in gradle home directory
    directory = File(rootDir, "build/build-cache")
    removeUnusedEntriesAfterDays = 30
  }
}

pluginManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    if (settings.providers.gradleProperty("fleetIntegration").get().toBoolean()) {
      maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
      maven("https://packages.jetbrains.team/maven/p/teamcity-rest-client/teamcity-rest-client")
      maven {
        url = uri("https://packages.jetbrains.team/maven/p/fleet/fleet-sdk")
        credentials {
          username = settings.providers.gradleProperty("spaceUsername").orNull
          password = settings.providers.gradleProperty("spacePassword").orNull
        }
      }
    }
  }
}
