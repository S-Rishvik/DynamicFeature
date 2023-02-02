package com.example.dynamicdelivery

import androidx.annotation.Keep
import androidx.lifecycle.*
import com.example.dynamicdelivery.ModuleStatus.*
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.google.android.play.core.splitinstall.SplitInstallSessionState
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus.*
import kotlinx.coroutines.launch

@Keep
class DynamicDeliveryVM(private val manager: SplitInstallManager) : ViewModel() {

    private var _dynamicDeliveryLiveData = MutableLiveData<ModuleStatus>()
    val dynamicDeliveryLiveData: LiveData<ModuleStatus> = _dynamicDeliveryLiveData

    fun openActivityInOnDemandModule(moduleName: String, activityName: String) =
        viewModelScope.launch {
            if (manager.installedModules.contains(moduleName)) {
                _dynamicDeliveryLiveData.value = Installed(moduleName, activityName)
            } else {
                requestModuleInstallation(moduleName, activityName)
            }
        }

    private fun requestModuleInstallation(moduleName: String, activityName: String) {
        val request = SplitInstallRequest.newBuilder().addModule(moduleName).build()
        manager.startInstall(request).addOnFailureListener {
            _dynamicDeliveryLiveData.value = Failed
        }
        getDynamicModuleStatus(moduleName, activityName)
    }

    private fun getDynamicModuleStatus(moduleName: String, activityName: String) {
        manager.registerListener {
            _dynamicDeliveryLiveData.value = when (it.status()) {
                CANCELED -> Available
                DOWNLOADING -> Installing(
                    ((it.bytesDownloaded().toDouble() / it.totalBytesToDownload()) * 100).toInt()
                )
                DOWNLOADED -> Installing(100)
                FAILED -> Failed
                INSTALLED -> Installed(moduleName, activityName)
                REQUIRES_USER_CONFIRMATION -> NeedsConfirmation(it)
                CANCELING, PENDING -> Installing(0)
                else -> Unavailable
            }
        }
    }

    fun removeModule(moduleName: List<String>){
        manager.deferredUninstall(moduleName)
    }

    companion object {
        const val ON_DEMAND_MODULE_NAME = "onDemand"
        const val ON_DEMAND_ACTIVITY_CLASS_PATH =
            "com.example.ondemand.OnDemandActivity"
        const val INSTALL_TIME_MODULE_NAME = "installTime"
        const val INSTALL_TIME_ACTIVITY_CLASS_PATH = "com.example.installtime.InstallTimeActivity"
    }
}

class InstallViewModelProviderFactory(
    private val manager: SplitInstallManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return modelClass.getConstructor(SplitInstallManager::class.java).newInstance(manager)
    }
}

sealed class ModuleStatus {
    object Available : ModuleStatus()
    data class Installing(val progress: Int) : ModuleStatus()
    object Unavailable : ModuleStatus()
    data class Installed(val moduleName: String, val activityName: String) : ModuleStatus()
    class NeedsConfirmation(val state: SplitInstallSessionState) : ModuleStatus()
    object Failed : ModuleStatus()
}