import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import co.touchlab.skie.configuration.DefaultArgumentInterop
import co.touchlab.skie.configuration.SealedInterop
import co.touchlab.skie.configuration.EnumInterop

// Helper to bridge GITHUB_TOKEN from env to GITHUB_PUBLISH_TOKEN if needed
if (System.getenv("GITHUB_TOKEN") != null && !project.hasProperty("GITHUB_PUBLISH_TOKEN")) {
    rootProject.extra["GITHUB_PUBLISH_TOKEN"] = System.getenv("GITHUB_TOKEN")
}
if (System.getenv("GITHUB_REPOSITORY") != null && !project.hasProperty("GITHUB_REPO")) {
    rootProject.extra["GITHUB_REPO"] = System.getenv("GITHUB_REPOSITORY")
}

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.skie)
    alias(libs.plugins.kmmbridge)
    id("jacoco")
}

group = "com.github.rfaturriza"
version = "1.0.0"

kotlin {
    jvm()
    android {
       namespace = "com.nonaconfig"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()

       compilerOptions {
           jvmTarget.set(JvmTarget.JVM_11)
       }
    }
    
    val xcfName = "NonaConfig"
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = xcfName
            isStatic = false // Best for distributable frameworks with Swift interfaces
            freeCompilerArgs += listOf(
                "-Xbinary=bundleId=com.nonaconfig.ios"
            )
        }
    }
    
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.no.arg)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.multiplatform.settings.test)
            implementation(libs.ktor.client.mock)
        }
    }
}

jacoco {
    toolVersion = "0.8.12"
}

tasks.register<JacocoReport>("jacocoTestReport") {
    group = "Reporting"
    description = "Generate Jacoco coverage report for JVM tests"

    dependsOn("jvmTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    val commonMainClasses = fileTree("${project.layout.buildDirectory.get().asFile}/classes/kotlin/jvm/main")

    classDirectories.setFrom(commonMainClasses)
    sourceDirectories.setFrom(files("src/commonMain/kotlin"))
    executionData.setFrom(fileTree(project.layout.buildDirectory.get().asFile).include("jacoco/jvmTest.exec"))
}

kmmbridge {
    frameworkName.set("NonaConfig")
    spm()
    gitHubReleaseArtifacts(repository = "rfaturriza/NonaConfigKMP")
}

skie {
    isEnabled.set(true)
    analytics {
        disableUpload.set(true)
        enabled.set(false)
    }
    build {
        produceDistributableFramework()
    }
    features {
        group {
            DefaultArgumentInterop.Enabled(true)
            SealedInterop.Enabled(true)
            EnumInterop.Enabled(true)
        }
    }
}

publishing {
    publications.withType<MavenPublication> {
        // Replace the default project name 'sharedLogic' with 'nona-config' in the artifact ID
        artifactId = artifactId.replace(project.name, "nona-config")

        pom {
            name.set("Nona Config SDK")
            description.set("A Kotlin Multiplatform SDK for Nona Config")
            url.set("https://github.com/rfaturriza/NonaConfigKMP")
            licenses {
                license {
                    name.set("The Apache License, Version 2.0")
                    url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }
            scm {
                connection.set("scm:git:github.com/rfaturriza/NonaConfigKMP.git")
                developerConnection.set("scm:git:ssh://github.com/rfaturriza/NonaConfigKMP.git")
                url.set("https://github.com/rfaturriza/NonaConfigKMP/tree/main")
            }
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/rfaturriza/NonaConfigKMP")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
