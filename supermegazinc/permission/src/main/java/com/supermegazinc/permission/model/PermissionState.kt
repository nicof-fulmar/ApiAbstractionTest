package com.supermegazinc.permission.model

data class PermissionState(
    val permission: String,
    val isGranted: Boolean,
    val shouldShowRationale: Boolean,
)