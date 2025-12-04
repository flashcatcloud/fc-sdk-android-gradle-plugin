/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin

/**
 * Defines the Flashcat sites you can send tracked data to.
 */
enum class FlashcatSite(internal val id: String, internal val intakeHostName: String) {
    /**
     * The CN site: [browser.flashcat.cloud](https://browser.flashcat.cloud).
     */
    CN("cn", "browser.flashcat.cloud"),

    /**
     * The STAGING site (internal usage only): [jira.flashcat.cloud](https://jira.flashcat.cloud).
     */
    STAGING("staging", "jira.flashcat.cloud");

    /** The intake endpoint url. */
    val intakeEndpoint: String = "https://$intakeHostName"

    /**
     * Returns the endpoint to use to upload sourcemap to this site.
     */
    internal fun uploadEndpoint(): String {
        return "https://sourcemap-intake.$intakeHostName/api/v2/srcmap"
    }

    /**
     * Returns the endpoint to use to verify API key for this site.
     */
    internal fun apiKeyVerificationEndpoint(): String {
        return "https://api.$intakeHostName/api/v1/validate"
    }

    companion object {
        internal val validIds = FlashcatSite.values().map { it.name }

        /**
         * Returns the FlashcatSite matching the given hostname, or null if not found.
         * Only predefined intakeHostName values in the enum will be recognized.
         */
        internal fun fromHostName(hostName: String): FlashcatSite? {
            return FlashcatSite.values().firstOrNull { it.intakeHostName == hostName }
        }
    }
}

