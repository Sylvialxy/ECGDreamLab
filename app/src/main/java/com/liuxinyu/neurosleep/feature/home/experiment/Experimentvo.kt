package com.liuxinyu.neurosleep.feature.home.experiment

import com.google.gson.annotations.SerializedName

data class Experimentvo(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String
)