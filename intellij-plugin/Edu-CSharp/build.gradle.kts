plugins {
  id("intellij-plugin-module-conventions")
}

dependencies {
  intellijPlatform {
    intellijIde(riderVersion)
    intellijPlugins(csharpPlugins)

    bundledModule("intellij.rider")
    bundledModule("intellij.rider.debugger.shared")
    bundledModule("intellij.rider.rdclient.dotnet")
    bundledModule("intellij.rider.languages")
    bundledModule("intellij.rd.client")
    bundledModule("intellij.rider.model.generated")
    bundledModule("intellij.xml.psi.impl")
    testIntellijPlugins(sshPlugin)
  }

  implementation(project(":intellij-plugin:educational-core"))
  testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
  testImplementation(libs.testng)
}
