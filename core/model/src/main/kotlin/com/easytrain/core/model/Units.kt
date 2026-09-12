package com.easytrain.core.model

/**
 * Display preference only. Weights are stored in kilograms everywhere below the UI layer.
 */
enum class Units(
    val wireValue: String,
) {
    KG("kg"),
    LB("lb"),
    ;

    companion object {
        fun fromWireValue(value: String?): Units = entries.firstOrNull { it.wireValue == value } ?: KG
    }
}
