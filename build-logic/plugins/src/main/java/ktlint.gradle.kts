import org.gradle.accessors.dm.LibrariesForLibs

val libs = the<LibrariesForLibs>()

plugins {
  id("org.jlleitschuh.gradle.ktlint")
}

ktlint {
  version.set("0.49.1")

  filter {
    exclude { entry ->
      entry.file.toString().contains("build/generated/source/wire")
    }
  }
}

dependencies {
  ktlintRuleset(libs.ktlint.twitter.compose)
}
