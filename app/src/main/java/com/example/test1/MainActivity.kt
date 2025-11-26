package com.example.test1

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONArray
import android.content.SharedPreferences

class MainActivity : AppCompatActivity() {
    private lateinit var btn1: Button
    private lateinit var btn2: Button
    private lateinit var btn3: Button
    private var bleScan = BleScan() // BleScan 인스턴스 생성can()
    private var isScanning = false // 상태 변수 추가
    private var scanJob: Job? = null // 코루틴 Job 저장 변수


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        btn1 = findViewById(R.id.btn1)
        btn1.setOnClickListener {
            Step1()
            Log.d("MainActivity", "Button 1 clicked")
        }

        btn2 = findViewById(R.id.btn2)
        btn2.setOnClickListener {
            Step2()
            Log.d("MainActivity", "Button 2 clicked")
        }

        btn3 = findViewById(R.id.btn3)
        btn3.setOnClickListener {
            toggleScan()
            Log.d("MainActivity", "Button 3 clicked")
        }

        // 🔹 BleScan에서 데이터를 받을 콜백 설정
        bleScan.setDataReceiveListener { buff ->
            runOnUiThread {
                Log.e("TAG", "Lib_ComRecvAT buff: $buff") // 🔥 MainActivity에서 buff 로그 출력
                // 나중에 UI 업데이트 로직 추가 가능 (ex: TextView)
            }
        }

    }

    private fun Step1() {
        val ret = bleScan.enableMasterMode1(true)
        Log.d("MainActivity", "Step1: " + ret)
        val mac = bleScan.getDeviceMacAddress()
        Log.d("MainActivity", "mac: " + mac)

    }

    private fun Step2() {
        val ret = bleScan.startNewScan("", "mcandle", 0, "", "")
        Log.d("MainActivity", "Step2: " + ret)
    }

    private fun Step3() {
        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE) // ✅ sharedPreferences 가져오기

        val scanResultListener = object : BleScan.ScanResultListener { // ✅ ScanResultListener 객체 생성
            override fun onScanResult(scanData: JSONArray) {
                runOnUiThread {
                    Log.d("BLE_SCAN", "Received Scan Data: $scanData")
                    // UI 업데이트 코드 추가 가능
                }
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            bleScan.startScanAsync(sharedPreferences, scanResultListener)
        }
    }


    private fun toggleScan() {
        if (isScanning) {
            // Stop Scan
            isScanning = false
            btn3.text = "ComRev"
            btn1.isEnabled = true
            btn2.isEnabled = true
            scanJob?.cancel() // 코루틴 중지
            bleScan.stopScan() // BLE 스캔 중지
            Log.d("MainActivity", "Scanning stopped")
        } else {
            // Start Scan
            isScanning = true
            btn3.text = "Stop"
            btn1.isEnabled = false
            btn2.isEnabled = false

            // 코루틴 실행 (백그라운드에서 실행)
            scanJob = CoroutineScope(Dispatchers.IO).launch {
                Step3()
            }
            Log.d("MainActivity", "Scanning started")
        }
    }

    private fun startScan() {
        // 스캔 리스너 설정
        val scanListener = object : BleScan.ScanResultListener {
            override fun onScanResult(scanData: JSONArray) {
                // 백그라운드 스레드에서 메인 스레드로 전환
                Handler(Looper.getMainLooper()).post {
                    Log.e("BLE_SCAN", "onScanResult: " + scanData)
                    //               updateDeviceList(scanData)
                }
            }
        }
    }
}