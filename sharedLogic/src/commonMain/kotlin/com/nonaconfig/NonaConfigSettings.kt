package com.nonaconfig

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.hours

class NonaConfigSettings internal constructor(
    val minimumFetchInterval: Duration,
    val fetchTimeout: Duration
) {
    class Builder {
        private var minimumFetchInterval: Duration = 12.hours
        private var fetchTimeout: Duration = 1.minutes

        fun setMinimumFetchInterval(interval: Duration) = apply {
            this.minimumFetchInterval = interval
        }

        fun setFetchTimeout(timeout: Duration) = apply {
            this.fetchTimeout = timeout
        }

        fun build(): NonaConfigSettings = NonaConfigSettings(
            minimumFetchInterval = minimumFetchInterval,
            fetchTimeout = fetchTimeout
        )
    }
}
