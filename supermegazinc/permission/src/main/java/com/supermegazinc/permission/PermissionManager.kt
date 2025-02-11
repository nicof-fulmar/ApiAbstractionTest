package com.supermegazinc.permission

import com.supermegazinc.permission.model.PermissionState
import kotlinx.coroutines.flow.StateFlow

interface PermissionManager {
    fun setPermissions(permissions: List<String>)
    val permissions: StateFlow<List<PermissionState>>
    fun refreshPermissionsState()
}