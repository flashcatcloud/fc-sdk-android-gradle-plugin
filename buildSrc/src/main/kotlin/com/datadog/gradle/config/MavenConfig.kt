/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.config

import com.datadog.gradle.utils.Version
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.findByType
import org.gradle.plugins.signing.SigningExtension

object MavenConfig {

    val VERSION = determineVersion()
    const val GROUP_ID = "cloud.flashcat"
    const val PUBLICATION = "pluginMaven"

    /**
     * Determine version based on Git ref type and branch
     * - Tag (v*) → Release version (e.g., 1.0.0)
     * - publish branch → Snapshot version (e.g., 1.1.0-SNAPSHOT)
     * - Other → Local development version
     */
    private fun determineVersion(): Version {
        val refType = System.getenv("GITHUB_REF_TYPE")
        val refName = System.getenv("GITHUB_REF_NAME")
        
        return when {
            // Tag release: v1.0.0 → 1.0.0
            refType == "tag" && refName?.startsWith("v") == true -> {
                parseVersionFromTag(refName)
            }
            // publish branch → Snapshot
            refName == "publish" -> {
                Version(1, 1, 0, Version.Type.Snapshot)
            }
            // Local development or other branches
            else -> {
                Version(1, 0, 0, Version.Type.Release)
            }
        }
    }

    private fun parseVersionFromTag(tag: String): Version {
        val versionString = tag.removePrefix("v")
        val parts = versionString.split(".")
        
        return Version(
            major = parts.getOrNull(0)?.toIntOrNull() ?: 1,
            minor = parts.getOrNull(1)?.toIntOrNull() ?: 0,
            hotfix = parts.getOrNull(2)?.toIntOrNull() ?: 0,
            type = Version.Type.Release
        )
    }
}

fun Project.publishingConfig(projectDescription: String) {
    val projectName = name
    val signingExtension = extensions.findByType(SigningExtension::class)

    if (signingExtension == null) {
        logger.error("Missing signing extension for $projectName")
        return
    }
    signingExtension.apply {
        val privateKey = System.getenv("GPG_PRIVATE_KEY")
        val password = System.getenv("GPG_PASSWORD")
        isRequired = System.getenv("CI").toBoolean() && !hasProperty("dd-skip-signing")
        useInMemoryPgpKeys(privateKey, password)
        // com.gradle.plugin-publish plugin will automatically add signing task "signPluginMavenPublication"
        // sign(publishingExtension.publications.getByName(MavenConfig.PUBLICATION))
    }

    afterEvaluate {
        tasks.named("javadocJar", Jar::class.java).configure {
            group = "publishing"
            dependsOn("dokkaGenerate")
            archiveClassifier.convention("javadoc")
            from("${layout.buildDirectory.dir("/reports/javadoc")}")
        }

        val publishingExtension = extensions.findByType<PublishingExtension>()
        if (publishingExtension == null) {
            logger.error("Missing publishing extension for $projectName")
            return@afterEvaluate
        }

        publishingExtension.apply {
            publications.getByName(MavenConfig.PUBLICATION) {
                check(this is MavenPublication)

                groupId = MavenConfig.GROUP_ID
                artifactId = projectName
                version = MavenConfig.VERSION.name

                pom {
                    name.set(projectName)
                    description.set(projectDescription)
                    url.set("https://github.com/flashcatcloud/fc-sdk-android-gradle-plugin/")

                    licenses {
                        license {
                            name.set("Apache-2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0")
                        }
                    }
                    organization {
                        name.set("Flashcat")
                        url.set("https://flashcat.cloud/")
                    }
                    developers {
                        developer {
                            name.set("Flashcat")
                            email.set("support@flashcat.cloud")
                            organization.set("Flashcat")
                            organizationUrl.set("https://flashcat.cloud/")
                        }
                    }

                    scm {
                        url.set("https://github.com/flashcatcloud/fc-sdk-android-gradle-plugin/")
                        connection.set(
                            "scm:git:git@github.com:flashcatcloud/fc-sdk-android-gradle-plugin.git"
                        )
                        developerConnection.set(
                            "scm:git:git@github.com:flashcatcloud/fc-sdk-android-gradle-plugin.git"
                        )
                    }
                }
            }
        }

        val mavenPublishing = extensions.findByType<MavenPublishBaseExtension>()
        if (mavenPublishing == null) {
            logger.error("Missing Maven publishing extension for $projectName")
            return@afterEvaluate
        }

        mavenPublishing.apply {
            publishToMavenCentral(automaticRelease = false)
        }
    }
}
