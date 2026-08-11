package com.healthdiary.app.util

fun Float.toDisplayString(): String =
    if (this % 1f == 0f) toInt().toString() else toString()
