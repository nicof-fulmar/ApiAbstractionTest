package com.example.apiabstractiontest

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.example.apiabstractiontest.ble_test.BLETestK
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        CoroutineScope(Dispatchers.IO).launch {
            App.bleUpgrade.connect(
                BLETestK.TANGO_BLE_NAME,
                10000,
                listOf(BLETestK.SERVICE_MAIN_UUID),
                516
            ).toString()
        }
    }
}