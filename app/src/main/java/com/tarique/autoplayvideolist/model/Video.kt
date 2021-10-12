package com.tarique.autoplayvideolist.model

import java.io.Serializable

data class Video(
    val id: Int,
    val thumbnailUrl: String, val url: String,
    val title: String,
    val subTitle: String
): Serializable
