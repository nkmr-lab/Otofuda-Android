package com.example.otofuda_android.Response

import com.google.firebase.database.IgnoreExtraProperties
import java.io.Serializable

@IgnoreExtraProperties
data class Room(
    var name: String? = "",
    var date: String? = "",
    var member: List<String>? = null,
    var status: String? = "",
    var mode: Map<String, String>? = null,
    var cardLocations: List<Int>? = null,
    var selectedPlayers: List<Int>? = null,
    var musicCounts: List<Int>? = null,
    var playMusics: List<Map<String, Any>>? = null,
    var tapped: List<Map<String, Any>>? = null,
    var answearUser: List<Map<String, Any>>? = null // これでいいんか？
): Serializable