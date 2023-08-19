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

        // Call the readApi function when the activity is created
        readApi()
    }

    private fun readApi() {
        GlobalScope.launch(Dispatchers.IO) {
            // This code runs on a background thread

            // Log a message indicating that the API call has started
            Log.d("MainActivity", "Start calling API")

            // Make an HTTP request to the API
            val result = URL("http://10.232.254.109:8080/project/DB/get/Temi_start.php?ID=2").readText()

            // Log the result of the API call
            Log.d("MainActivity", "API result: $result")

            // Parse the JSON response
            val jsonArray = JSONObject(result)
            if (!jsonArray.isNull("data")) {
                val data = jsonArray.getJSONArray("data")

                // Log the length of the data array
                Log.d("MainActivity", "Data array length: ${data.length()}")

                // Check if there are any records with status = "start"
                var found = false
                for (i in 0 until data.length()) {
                    val item = data.getJSONObject(i)
                    val status = item.getString("Status_start")

                    // Log the status of the current item
                    Log.d("MainActivity", "Item $i status: $status")

                    if (status == "start") {
                        // Log that Temi is in the "start" status
                        Log.d("MainActivity", "Temi is start")

                        // Navigate to Activity2
                        startActivity(Intent(this@MainActivity, Activity2::class.java))
                        finish()
                        found = true
                        break
                    } else {
                        // Log that Temi is not available
                        Log.e("MainActivity", "Temi is not available")

                        // Wait for 10 seconds
                        delay(10000)

                        // Increment the number of read attempts
                        readApiAttempts++

                        // Check if we have reached the maximum number of attempts
                        if (readApiAttempts < 5760) {
                            // Try reading the API again
                            readApi()
                        } else {
                            // Log that the maximum read attempts have been reached
                            Log.e("MainActivity", "Reached maximum read attempts")
                        }
                        break
                    }
                }
            } else {
                // Log an error message if the JSON response does not contain data
                Log.e("MainActivity", "JSON response does not contain data")
            }
        }
    }
}
