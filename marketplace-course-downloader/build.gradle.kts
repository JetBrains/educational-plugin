plugins {
  id("common-conventions")
  application
}

dependencies {
  implementation(project(":edu-format"))
  implementation(libs.okhttp)
  implementation(libs.jackson.module.kotlin)
}

application {
  mainClass.set("com.jetbrains.edu.tools.marketplace.MarketplaceCourseDownloaderKt")
}
