package com.healthdiary.app.util

fun Float.toDisplayString(): String =
    if (this % 1f == 0f) toInt().toString() else toString()

fun volumeDisplay(kg: Double): String =
    if (kg >= 1000) {
        String.format(java.util.Locale.US, "%.1ft", kg / 1000)
    } else {
        String.format(java.util.Locale.US, "%.0fkg", kg)
    }
