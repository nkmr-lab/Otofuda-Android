package com.example.otofuda_android.Response

data class PresetListResponse(
    val list: List<PresetList>,
    val result: String
)

data class PresetList(
    val id: Int,
    val presets: List<Preset>,
    val type_name: String
)

data class Preset(
    val count: Int,
    val id: Int,
    val name: String
)