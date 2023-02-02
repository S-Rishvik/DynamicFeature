package com.example.dynamicdelivery

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.dynamicdelivery.ui.theme.DynamicDeliveryTheme
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory

class MainActivity : ComponentActivity() {

    private val splitInstallManager: SplitInstallManager by lazy {
        SplitInstallManagerFactory.create(applicationContext)
    }

    private val dynamicDeliveryVM by viewModels<DynamicDeliveryVM> {
        InstallViewModelProviderFactory(splitInstallManager)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DynamicDeliveryTheme {
                // A surface container using the 'background' color from the theme
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Button(onClick = {
                        dynamicDeliveryVM.openActivityInOnDemandModule(
                            DynamicDeliveryVM.INSTALL_TIME_MODULE_NAME,
                            DynamicDeliveryVM.INSTALL_TIME_ACTIVITY_CLASS_PATH
                        )
                    }) {
                        Text(text = "On Install Delivery")
                    }
                    Button(onClick = {
                        dynamicDeliveryVM.openActivityInOnDemandModule(
                            DynamicDeliveryVM.ON_DEMAND_MODULE_NAME,
                            DynamicDeliveryVM.ON_DEMAND_ACTIVITY_CLASS_PATH
                        )
                    }) {
                        Text(text = "On Demand Delivery")
                    }
                    Button(onClick = {
                        dynamicDeliveryVM.removeModule(listOf(DynamicDeliveryVM.INSTALL_TIME_MODULE_NAME))
                    }) {
                        Text(text = "Remove Install Time Module")
                    }
                }
            }
        }
        addObservers()
    }

    private fun addObservers() {
        dynamicDeliveryVM.dynamicDeliveryLiveData.observe(this) {
//            if (it is ModuleStatus.Installing) showLoader(getString(R.string.launching_video_kyc) + " ${it.progress}%") else hideLoader()
            when (it) {
                ModuleStatus.Failed -> toast("Failed")
                is ModuleStatus.Installed -> openActivity(it.moduleName, it.activityName)
                ModuleStatus.Unavailable -> toast("Unavailable")
                is ModuleStatus.NeedsConfirmation -> {
                    splitInstallManager.startConfirmationDialogForResult(
                        it.state,
                        this,
                        USER_CONFIRMATION_REQUEST_CODE
                    )
                }
                else -> {}
            }
        }
    }

    private fun openActivity(moduleName: String, activityName: String) {
        val intent = Intent()
        intent.setClassName(
            BuildConfig.APPLICATION_ID,
            activityName
        )
        if (splitInstallManager.installedModules.contains(moduleName))
            startActivity(intent)
        else
            toast("Module unavailable")
    }

    private fun toast(text: String) {
        Toast.makeText(applicationContext, text, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val USER_CONFIRMATION_REQUEST_CODE = 17819
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    DynamicDeliveryTheme {
        Greeting("Android")
    }
}