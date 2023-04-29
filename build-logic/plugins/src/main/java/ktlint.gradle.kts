import org.gradle.accessors.dm.LibrariesForLibs

val libs = the<LibrariesForLibs>()

plugins {
    id("org.jlleitschuh.gradle.ktlint")
}

ktlint {
    // Use a newer version to resolve https://github.com/JLLeitschuh/ktlint-gradle/issues/507
    version.set("0.47.1")

    filter {
        exclude { entry ->
            entry.file.toString().contains("build/generated/source/wire")
        }
    }
}

dependencies {
    ktlintRuleset(libs.ktlint.twitter.compose)
}
