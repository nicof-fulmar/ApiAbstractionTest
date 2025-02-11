package com.supermegazinc.permission

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.supermegazinc.logger.Logger
import com.supermegazinc.permission.model.PermissionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class PermissionManagerAndroidImpl(
    private val onObtainContext: () -> Context,
    private val onObtainActivity: () -> Activity,
    private val logger: Logger
) : PermissionManager {

    private companion object {
        const val LOG_KEY = "PERMISSION"
    }

    private val _permissions = MutableStateFlow<List<PermissionState>>(emptyList())
    override val permissions: StateFlow<List<PermissionState>>
        get() = _permissions.asStateFlow()

    override fun setPermissions(permissions: List<String>) {
        _permissions.value = permissions.map {
            it.toPermissionState()
        }.also {
            logger.d(LOG_KEY, "setPermissions: $it")
        }
    }

    override fun refreshPermissionsState() {
        _permissions.update { old->
            old.map {
                it.permission.toPermissionState()
            }.also { new->
                logger.d(LOG_KEY, "refreshPermissionsState: $new")
            }
        }
    }

    private fun String.toPermissionState(): PermissionState {
        return PermissionState(
            permission = this,
            isGranted = checkPermission(this),
            shouldShowRationale = shouldShowRequestPermissionRationale(this)
        )
    }

    private fun shouldShowRequestPermissionRationale(permission: String) : Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            onObtainActivity().shouldShowRequestPermissionRationale(permission)
        } else {
            ActivityCompat.shouldShowRequestPermissionRationale(onObtainActivity(), permission)
        }
    }

    private fun checkPermission(permission: String): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            onObtainActivity().checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(onObtainContext(), permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun checkPermissions(permissions: List<String>): Boolean = permissions.all{checkPermission(it)}

    fun requestPermissions(launcher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>) {
        _permissions.value.filter { it.shouldShowRationale }.map { it.permission }.toTypedArray().let {
            logger.d(LOG_KEY, "requestPermissions: $it")
            launcher.launch(it)
        }
    }

    fun onRequestPermissionsResult(result: Map<String, Boolean>) {
        _permissions.update { old->
            old.map {
                it.copy(
                    isGranted = result[it.permission] ?: false,
                    shouldShowRationale = shouldShowRequestPermissionRationale(it.permission)
                )
            }.also {
                logger.d(LOG_KEY, "onRequestPermissionsResult: $it")
            }
        }
    }
}