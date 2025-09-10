package com.cocido.ramf.models

import com.google.gson.annotations.SerializedName

data class ChartsResponse(
    @SerializedName("charts")
    val charts: Map<String, Any>
)