package com.biewangle.dontforget.data.model

enum class RepeatType(val value: String, val label: String) {
    NONE("NONE", "不重复"),
    DAILY("DAILY", "每天"),
    WEEKLY("WEEKLY", "每周"),
    MONTHLY("MONTHLY", "每月");

    companion object {
        fun fromValue(value: String): RepeatType =
            entries.find { it.value == value } ?: NONE
    }
}
