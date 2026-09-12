package com.easytrain.core.network

/**
 * The app only ever holds the publishable key. `core/network` cannot read the app module's
 * BuildConfig, so `app` binds these values into the graph.
 */
data class SupabaseConfig(
    val url: String,
    val publishableKey: String,
)
