import nl.javadude.gradle.plugins.license.LicenseExtension
import java.util.*
import nebula.plugin.info.InfoBrokerPlugin
import nebula.plugin.contacts.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        jcenter()
        gradlePluginPortal()
    }

    dependencies {
        classpath("io.spring.gradle:spring-release-plugin:0.20.1")

        constraints {
            classpath("org.jfrog.buildinfo:build-info-extractor-gradle:4.13.0") {
                because("Need recent version for Gradle 6+ compatibility")
            }
        }
    }
}

plugins {
    id("io.spring.release") version "0.20.1"
    id("org.jetbrains.kotlin.jvm") version "1.3.71" apply false
}

allprojects {
    apply(plugin = "license")
    group = "org.openrewrite"
    description = "Eliminate tech-debt. Automatically."
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "nebula.maven-resolved-dependencies")
    apply(plugin = "io.spring.publishing")

    repositories {
        mavenCentral()
    }

    dependencies {
        "compileOnly"("org.projectlombok:lombok:latest.release")
        "annotationProcessor"("org.projectlombok:lombok:latest.release")

        "testImplementation"("org.junit.jupiter:junit-jupiter-api:latest.release")
        "testImplementation"("org.junit.jupiter:junit-jupiter-params:latest.release")
        "testRuntimeOnly"("org.junit.jupiter:junit-jupiter-engine:latest.release")

        "testImplementation"("org.jetbrains.kotlin:kotlin-reflect")
        "testImplementation"("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

        "testImplementation"("org.assertj:assertj-core:latest.release")

        "testRuntimeOnly"("ch.qos.logback:logback-classic:1.0.13")
    }

    configure<ContactsExtension> {
        val j = Contact("jkschneider@gmail.com")
        j.moniker("Jonathan Schneider")

        people["jkschneider@gmail.com"] = j
    }

    configure<LicenseExtension> {
        ext.set("year", Calendar.getInstance().get(Calendar.YEAR))
        skipExistingHeaders = true
        header = project.rootProject.file("gradle/licenseHeader.txt")
        mapping(mapOf("kt" to "SLASHSTAR_STYLE", "java" to "SLASHSTAR_STYLE"))
        strictCheck = true
    }

    tasks.named<Test>("test") {
        useJUnitPlatform()
        jvmArgs = listOf("-XX:+UnlockDiagnosticVMOptions", "-XX:+ShowHiddenFrames")
    }

    tasks.withType(KotlinCompile::class.java).configureEach {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }

    configurations.all {
        resolutionStrategy.cacheDynamicVersionsFor(0, "seconds")
    }

    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    configure<PublishingExtension> {
        publications {
            named("nebula", MavenPublication::class.java) {
                suppressPomMetadataWarningsFor("runtimeElements")
            }
        }
    }

    tasks.withType<GenerateMavenPom> {
        doFirst {
            val runtimeClasspath = configurations.getByName("runtimeClasspath")

            val gav = { dep: ResolvedDependency ->
                "${dep.moduleGroup}:${dep.moduleName}:${dep.moduleVersion}"
            }

            val observedDependencies = TreeSet<ResolvedDependency> { d1, d2 ->
                gav(d1).compareTo(gav(d2))
            }

            fun reduceDependenciesAtIndent(indent: Int):
                    (List<String>, ResolvedDependency) -> List<String> =
                    { dependenciesAsList: List<String>, dep: ResolvedDependency ->
                        dependenciesAsList + listOf(" ".repeat(indent) + dep.module.id.toString()) + (
                                if (observedDependencies.add(dep)) {
                                    dep.children
                                            .sortedBy(gav)
                                            .fold(emptyList(), reduceDependenciesAtIndent(indent + 2))
                                } else {
                                    // this dependency subtree has already been printed, so skip it
                                    emptyList()
                                }
                                )
                    }

            project.plugins.withType<InfoBrokerPlugin> {
                add("Resolved-Dependencies", runtimeClasspath
                        .resolvedConfiguration
                        .lenientConfiguration
                        .firstLevelModuleDependencies
                        .sortedBy(gav)
                        .fold(emptyList(), reduceDependenciesAtIndent(6))
                        .joinToString("\n", "\n", "\n" + " ".repeat(4)))
            }
        }
    }
}

defaultTasks("build")
