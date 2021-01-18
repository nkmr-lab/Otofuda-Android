package com.example.otofuda_android.Response

import com.google.firebase.database.IgnoreExtraProperties
import java.io.Serializable

@IgnoreExtraProperties
data class Music (
    var name: String? = "",
    var artist: String? = "",
    var musicOwner: Int? = 0,
    var storeURL: String? = "",
    var previewURL: String? = ""
): Serializable