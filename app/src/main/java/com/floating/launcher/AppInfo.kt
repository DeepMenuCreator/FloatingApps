package com.floating.launcher

data class AppInfo(
    val packageName: String,
    val appName: String,
    var width: Int = 800,
    var height: Int = 600,
    var x: Int = 100,
    var y: Int = 100
)
