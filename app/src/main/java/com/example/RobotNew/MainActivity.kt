package com.example.RobotNew

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log

import com.robotemi.sdk.Robot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var robot: Robot
    private var readApiAttempts = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        robot = Robot.getInstance()

        readApi()
    }

    private fun readApi() {
        GlobalScope.launch(Dispatchers.IO) {
            // This code runs on a background thread
            Log.d("MainActivity", "Start calling API")
            val result = URL("http://10.232.4.111:8080/project/DB/get/Temi_start.php?ID=2").readText()

            Log.d("MainActivity", "API result: $result")

            // parse the JSON response
            val jsonArray = JSONObject(result)
            if (!jsonArray.isNull("data")) {
                val data = jsonArray.getJSONArray("data")
                Log.d("MainActivity", "Data array length: ${data.length()}")

                // check if there are any records with status = "start"
                var found = false
                for (i in 0 until data.length()) {
                    val item = data.getJSONObject(i)
                    val status = item.getString("Status_start")
                    Log.d("MainActivity", "Item $i status: $status")

                    if (status == "start") {
                        // check if Temi is available

                        Log.d("MainActivity", "Temi is start")
                        // navigate to Activity2
                        startActivity(Intent(this@MainActivity, Activity2::class.java))
                        finish()
                        found = true
                        break

                    } else {
                        Log.e("MainActivity", "Temi is not available")
                        // wait for 10 seconds
                        delay(10000)
                        // increment the number of read attempts
                        readApiAttempts++
                        // check if we have reached the maximum number of attempts
                        if (readApiAttempts < 5760) {
                            // try reading the API again
                            readApi()
                        } else {
                            Log.e("MainActivity", "Reached maximum read attempts")
                        }
                        break
                    }
                }
            } else {
                Log.e("MainActivity", "JSON response does not contain data")
            }
        }
    }
}