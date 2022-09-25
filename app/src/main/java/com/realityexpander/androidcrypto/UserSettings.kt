package com.realityexpander.androidcrypto

import kotlinx.serialization.Serializable

@Serializable
data class UserSettings(
    val username: String? = "",
    val password: String? = ""
)
