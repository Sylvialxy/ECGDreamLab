package com.liuxinyu.neurosleep.core.auth

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("token") val token: String
)


