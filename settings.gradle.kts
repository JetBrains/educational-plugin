import java.io.IOException
import java.net.URL
import java.net.UnknownHostException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.*

rootProject.name = "educational-plugin"
include(
  "edu-format",
  "intellij-plugin",
  "intellij-plugin:educational-core",
  "intellij-plugin:jvm-core",
  "intellij-plugin:AI",
  "intellij-plugin:Edu-Java",
  "intellij-plugin:Edu-Kotlin",
  "intellij-plugin:Edu-Scala",
  "intellij-plugin:Edu-Python",
  "intellij-plugin:Edu-JavaScript",
  "intellij-plugin:Edu-Rust",
  "intellij-plugin:Edu-Cpp",
  "intellij-plugin:Edu-Cpp:CLion-Classic", // specific support for CLion classic
  "intellij-plugin:Edu-Cpp:CLion-Nova",    // specific support for CLion Nova
  "intellij-plugin:Edu-Go",
  "intellij-plugin:Edu-Php",
  "intellij-plugin:Edu-Shell",
  "intellij-plugin:Edu-CSharp",
  "intellij-plugin:sql",
  "intellij-plugin:sql:sql-jvm",
  "intellij-plugin:localization",
  "intellij-plugin:features:code-insight-core",
  "intellij-plugin:features:code-insight-html",
  "intellij-plugin:features:code-insight-markdown",
  "intellij-plugin:features:code-insight-yaml",
  "intellij-plugin:features:command-line",
  "intellij-plugin:features:github",
  "intellij-plugin:features:ai-error-explanation",
  "intellij-plugin:features:ai-hints-core",
  "intellij-plugin:features:ai-hints-kotlin",
  "intellij-plugin:features:ai-hints-python",
  "intellij-plugin:features:ai-test-generation",
  "intellij-plugin:features:ide-onboarding",
  "intellij-plugin:features:social-media",
)

// BACKCOMPAT: Temporarily exclude for 2025.2 as it doesn't compile
if (settings.providers.gradleProperty("environmentName").get().toInt() < 252) {
  include("intellij-plugin:features:remote-env")
}

if (settings.providers.gradleProperty("fleetIntegration").get().toBoolean()) {
  include("fleet-plugin")
}

val secretProperties: String = "secret.properties"

val isTeamCity: Boolean get() = System.getenv("TEAMCITY_VERSION") != null

configureSecretProperties()

downloadHyperskillCss()

fun configureSecretProperties() {
  try {
    download(URL("https://repo.labs.intellij.net/edu-tools/secret.properties"), secretProperties)
  }
  catch (_: UnknownHostException) {
    println("repo.labs.intellij.net is not reachable")
    val secretProperties = file(secretProperties)
    if (!secretProperties.exists()) {
      secretProperties.createNewFile()
    }
  }

  val secretProperties = loadProperties(secretProperties)

  secretProperties.extractAndStore(
    "intellij-plugin/educational-core/resources/stepik/stepik.properties",
    "stepikClientId",
    "cogniterraClientId",
  )
  secretProperties.extractAndStore(
    "intellij-plugin/educational-core/resources/hyperskill/hyperskill-oauth.properties",
    "hyperskillClientId",
  )
  secretProperties.extractAndStore(
    "intellij-plugin/educational-core/resources/twitter/oauth_twitter.properties",
    "xClientId"
  )
  secretProperties.extractAndStore(
    "intellij-plugin/educational-core/resources/linkedin/linkedin-oauth.properties",
    "linkedInClientId",
    "linkedInClientSecret"
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
  secretProperties.extractAndStore(
    "intellij-plugin/educational-core/resources/lti/lti-auth.properties",
    "ltiServiceToken"
  )
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
  }
}

pluginManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://central.sonatype.com/repository/maven-snapshots/")
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
