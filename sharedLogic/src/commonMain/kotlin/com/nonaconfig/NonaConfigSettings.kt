package com.nonaconfig

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.hours

class NonaConfigSettings internal constructor(
    val minimumFetchInterval: Duration,
    val fetchTimeout: Duration,
    val releaseVersion: String? = null
) {
    class Builder {
        private var minimumFetchInterval: Duration = 12.hours
        private var fetchTimeout: Duration = 1.minutes
        private var releaseVersion: String? = null

        fun setMinimumFetchInterval(interval: Duration) = apply {
            this.minimumFetchInterval = interval
        }

        fun setFetchTimeout(timeout: Duration) = apply {
            this.fetchTimeout = timeout
        }

        fun setReleaseVersion(version: String?) = apply {
            this.releaseVersion = version
        }

        fun build(): NonaConfigSettings = NonaConfigSettings(
            minimumFetchInterval = minimumFetchInterval,
            fetchTimeout = fetchTimeout,
            releaseVersion = releaseVersion
        )
    }
}
