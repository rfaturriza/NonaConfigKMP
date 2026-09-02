import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import co.touchlab.skie.configuration.DefaultArgumentInterop
import co.touchlab.skie.configuration.SealedInterop
import co.touchlab.skie.configuration.EnumInterop

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.kmmbridge)
    alias(libs.plugins.skie)
    id("maven-publish")
}

group = "com.nonaconfig"
version = "1.0.0"

kotlin {
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
            isStatic = true
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
        }
    }
}

kmmbridge {
    frameworkName.set("NonaConfig")
    spm(useCustomPackageFile = true)
    mavenPublishArtifacts()
}

skie {
    features {
        group {
            DefaultArgumentInterop.Enabled(true)
            SealedInterop.Enabled(true)
            EnumInterop.Enabled(true)
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            afterEvaluate {
                from(components["kotlin"])
            }
            
            artifactId = "nona-config"
            
            pom {
                name.set("Nona Config SDK")
                description.set("A Kotlin Multiplatform SDK for Nona Config")
                url.set("https://github.com/ryware/nona-config")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                scm {
                    connection.set("scm:git:github.com/ryware/nona-config.git")
                    developerConnection.set("scm:git:ssh://github.com/ryware/nona-config.git")
                    url.set("https://github.com/ryware/nona-config/tree/main")
                }
            }
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/ryware/nona-config")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
