/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin

import com.datadog.gradle.plugin.internal.ApiKey
import com.datadog.gradle.plugin.internal.ApiKeySource
import com.datadog.gradle.plugin.internal.DdAppIdentifier
import com.datadog.gradle.plugin.internal.OkHttpUploader
import com.datadog.gradle.plugin.internal.Uploader
import com.datadog.gradle.plugin.internal.variant.AppVariant
import org.gradle.api.DefaultTask
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import javax.inject.Inject

/**
 * A Gradle task to upload symbolication files to Datadog servers (NDK symbol files,
 * Proguard/R8 files, etc.)..
 */
abstract class FileUploadTask @Inject constructor(
    providerFactory: ProviderFactory,
    @get:Internal internal val repositoryDetector: RepositoryDetector
) : DefaultTask() {

    @get:Internal
    internal var uploader: Uploader = OkHttpUploader()

    /**
     * The API key to use for uploading.
     */
    @get:Input
    var apiKey: String = ""

    private val disableGzipOption: Provider<String> =
        providerFactory.gradleProperty(DISABLE_GZIP_GRADLE_PROPERTY)

    // needed for functional tests, because we don't have real API key
    private val emulateNetworkCall: Provider<String> =
        providerFactory.gradleProperty(EMULATE_UPLOAD_NETWORK_CALL)

    /**
     * Source of the API key set: environment, gradle property, etc.
     */
    @get:Input
    var apiKeySource: ApiKeySource = ApiKeySource.NONE

    /**
     * The variant name of the application.
     */
    @get:Input
    var variantName: String = ""

    /**
     * The version name of the application.
     */
    @get:Input
    abstract val versionName: Property<String>

    /**
     * The version code of the application. Need to be a provider, because resolution during
     * configuration phase may cause incompatibility with other plugins if legacy Variant API is used.
     */
    @get:Input
    abstract val versionCode: Property<Int>

    /**
     * The service name of the application (by default, it is your app's package name).
     */
    @get:Input
    abstract val serviceName: Property<String>

    /**
     * The Flashcat site to upload to (one of "CN", "STAGING").
     */
    @get:Input
    var site: String = ""

    /**
     * The url of the remote repository where the source code was deployed.
     */
    @get:Input
    var remoteRepositoryUrl: String = ""

    /**
     * Build ID which will be used for mapping file matching.
     */
    @get:Input
    abstract val buildId: Property<String>

    /**
     * flashcat-ci.json file, if found or applicable for the particular task.
     */
    @Optional
    @get:InputFile
    var flashcatCiFile: File? = null

    /**
     * The sourceSet root folders.
     */
    @get:InputFiles
    abstract val sourceSetRoots: ListProperty<File>

    /**
     * The file containing the repository description.
     */
    @get:OutputFile
    var repositoryFile: File = File("")

    init {
        group = DdAndroidGradlePlugin.DATADOG_TASK_GROUP
        // it is never up-to-date, because request may fail
        outputs.upToDateWhen { false }
    }

    /**
     * Uploads the files retrieved from `getFilesList` to Datadog.
     */
    @TaskAction
    @Suppress("TooGenericExceptionCaught", "LongMethod")
    fun applyTask() {
        flashcatCiFile?.let {
            applyFlashcatCiConfig(it)
        }
        applySiteFromEnvironment()
        validateConfiguration()

        check(!(apiKey.contains("\"") || apiKey.contains("'"))) {
            INVALID_API_KEY_FORMAT_ERROR
        }

        check(buildId.isPresent && buildId.get().isNotEmpty()) {
            MISSING_BUILD_ID_ERROR
        }

        val mappingFiles = getFilesList()
        if (mappingFiles.isEmpty()) {
            LOGGER.warn("No mapping files to upload.")
            return
        }

        // it can be an overlap between java and kotlin directories and since File doesn't override
        // equals for set comparison, we will remove duplicates manually
        val uniqueSourceSetRoots = sourceSetRoots.get()
            .map { it.absolutePath }
            .distinct()
            .map { File(it) }

        val repositories = repositoryDetector.detectRepositories(
            uniqueSourceSetRoots,
            remoteRepositoryUrl
        )

        if (repositories.isNotEmpty()) {
            generateRepositoryFile(repositories)
        }

        val site = FlashcatSite.valueOf(site)
        val caughtErrors = mutableListOf<Exception>()

        for (mappingFile in mappingFiles) {
            LOGGER.info("Uploading ${mappingFile.fileType} file: ${mappingFile.file.absolutePath}")
            try {
                uploader.upload(
                    site,
                    mappingFile,
                    if (repositories.isEmpty()) null else repositoryFile,
                    apiKey,
                    DdAppIdentifier(
                        serviceName = serviceName.get(),
                        version = versionName.get(),
                        versionCode = versionCode.get(),
                        variant = variantName,
                        buildId = buildId.get()
                    ),
                    repositories.firstOrNull(),
                    !disableGzipOption.isPresent,
                    emulateNetworkCall.isPresent
                )
            } catch (e: Exception) {
                caughtErrors.add(e)
            }
        }
        // If any errors occurred, throw them as a single exception
        if (caughtErrors.isNotEmpty()) {
            if (caughtErrors.count() == 1) {
                throw caughtErrors.first()
            } else {
                val consolidatedError = RuntimeException("Multiple errors occurred during upload")
                caughtErrors.forEach {
                    consolidatedError.addSuppressed(it)
                }
                throw consolidatedError
            }
        }
    }

    // region Internal

    @Internal
    internal abstract fun getFilesList(): List<Uploader.UploadFileInfo>

    internal fun configureWith(
        apiKey: ApiKey,
        extensionConfiguration: DdExtensionConfiguration,
        variant: AppVariant
    ) {
        this.apiKey = apiKey.value
        apiKeySource = apiKey.source
        site = extensionConfiguration.site ?: ""

        versionName.set(variant.versionName)
        versionCode.set(variant.versionCode)

        if (extensionConfiguration.serviceName != null) {
            serviceName.set(extensionConfiguration.serviceName)
        } else {
            serviceName.set(variant.applicationId)
        }

        variantName = variant.flavorName
        remoteRepositoryUrl = extensionConfiguration.remoteRepositoryUrl ?: ""
    }

    // endregion

    // region Private

    private fun applySiteFromEnvironment() {
        val environmentSite = System.getenv(FLASHCAT_SITE)
        if (!environmentSite.isNullOrEmpty()) {
            if (this.site.isNotEmpty()) {
                DdAndroidGradlePlugin.LOGGER.info(
                    "Site property found as FLASHCAT_SITE env variable, but it will be ignored," +
                        " because also an explicit one was provided in extension."
                )
                return
            }
            val site = FlashcatSite.fromHostName(environmentSite)
            if (site == null) {
                DdAndroidGradlePlugin.LOGGER.warn("Unknown Flashcat hostname provided: $environmentSite, ignoring it.")
            } else {
                DdAndroidGradlePlugin.LOGGER.info("Site property found in Flashcat CI config file, using it.")
                this.site = site.name
            }
        }
    }

    private fun applyFlashcatCiConfig(flashcatCiFile: File) {
        try {
            val config = JSONObject(flashcatCiFile.readText())
            applyApiKeyFromFlashcatCiConfig(config)
            applySiteFromFlashcatCiConfig(config)
        } catch (e: JSONException) {
            DdAndroidGradlePlugin.LOGGER.error("Failed to parse Flashcat CI config file.", e)
        }
    }

    private fun applyApiKeyFromFlashcatCiConfig(config: JSONObject) {
        val apiKey = config.optString(FLASHCAT_CI_API_KEY_PROPERTY, null)
        if (!apiKey.isNullOrEmpty()) {
            if (this.apiKeySource == ApiKeySource.GRADLE_PROPERTY) {
                DdAndroidGradlePlugin.LOGGER.info(
                    "API key found in Flashcat CI config file, but it will be ignored," +
                        " because also an explicit one was provided as a gradle property."
                )
            } else {
                DdAndroidGradlePlugin.LOGGER.info("API key found in Flashcat CI config file, using it.")
                this.apiKey = apiKey
                this.apiKeySource = ApiKeySource.FLASHCAT_CI_CONFIG_FILE
            }
        }
    }

    private fun applySiteFromFlashcatCiConfig(config: JSONObject) {
        val siteAsHostName = config.optString(FLASHCAT_CI_SITE_PROPERTY, null)
        if (!siteAsHostName.isNullOrEmpty()) {
            if (this.site.isNotEmpty()) {
                DdAndroidGradlePlugin.LOGGER.info(
                    "Site property found in Flashcat CI config file, but it will be ignored," +
                        " because also an explicit one was provided in extension."
                )
            } else {
                val site = FlashcatSite.fromHostName(siteAsHostName)
                if (site == null) {
                    DdAndroidGradlePlugin.LOGGER.warn("Unknown Flashcat hostname provided: $siteAsHostName, ignoring it.")
                } else {
                    DdAndroidGradlePlugin.LOGGER.info("Site property found in Flashcat CI config file, using it.")
                    this.site = site.name
                }
            }
        }
    }

    @Suppress("CheckInternal")
    private fun validateConfiguration() {
        check(apiKey.isNotBlank()) { API_KEY_MISSING_ERROR }

        if (site.isBlank()) {
            site = FlashcatSite.CN.name
        } else {
            val validSiteIds = FlashcatSite.validIds
            check(site in validSiteIds) {
                "You need to provide a valid site (one of ${validSiteIds.joinToString()})"
            }
        }
    }

    private fun generateRepositoryFile(repositories: List<RepositoryInfo>) {
        val data = JSONArray()
        repositories.forEach {
            data.put(it.toJson())
            DdAndroidGradlePlugin.LOGGER.info(
                "Detected repository:\n${it.toJson().toString(
                    INDENT
                )}"
            )
        }

        val jsonObject = JSONObject()
        jsonObject.put("version", REPOSITORY_FILE_VERSION)
        jsonObject.put("data", data)

        repositoryFile.parentFile.mkdirs()
        repositoryFile.writeText(jsonObject.toString(0))
    }

    // endregion

    internal companion object {
        private const val REPOSITORY_FILE_VERSION = 1
        private const val INDENT = 4

        private const val FLASHCAT_CI_API_KEY_PROPERTY = "apiKey"
        private const val FLASHCAT_CI_SITE_PROPERTY = "flashcatSite"
        const val FLASHCAT_SITE = "FLASHCAT_SITE"

        internal val LOGGER = Logging.getLogger("DdFileUploadTask")

        const val DISABLE_GZIP_GRADLE_PROPERTY = "dd-disable-gzip"
        const val EMULATE_UPLOAD_NETWORK_CALL = "dd-emulate-upload-call"

        const val API_KEY_MISSING_ERROR = "Make sure you define an API KEY to upload your mapping files to Flashcat. " +
            "Create a FC_API_KEY or FLASHCAT_API_KEY environment variable, gradle" +
            " property or define it in flashcat-ci.json file."
        const val INVALID_API_KEY_FORMAT_ERROR =
            "FC_API_KEY provided shouldn't contain quotes or apostrophes."
        const val MISSING_BUILD_ID_ERROR =
            "Build ID is missing, you need to run upload task only after APK/AAB file is generated."
    }
}
