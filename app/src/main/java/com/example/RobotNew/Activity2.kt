package com.example.RobotNew

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.robotemi.sdk.Robot
import com.robotemi.sdk.TtsRequest
import com.robotemi.sdk.listeners.OnGoToLocationStatusChangedListener
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.lang.Runnable
import java.net.URL

class Activity2 : AppCompatActivity(), OnGoToLocationStatusChangedListener {
    private lateinit var robot: Robot
    private var state: Int = -1
    private var pp: Int = 1
    private var ii: Int = 9
    private var temiId: Int = 2
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_2)
        robot = Robot.getInstance()

        Thread(Runnable {
            var k = 0
            while (k < 2) {
                Thread.sleep(1000)
                k += 1
            }
            try {
                val result = URL("http://10.232.153.49:8080/project/DB/get/room1.php?ID=1").readText()
                val jsonArray = JSONObject(result)
                if (!jsonArray.isNull("data")) {
                    val data = jsonArray.getJSONArray("data")
                    if (data.length() > 0) {
                        val room = data.getJSONObject(0).getString("Room")
                        robot.goTo(room, backwards = false, noBypass = false)
                        updateLocationToServer(state+1)
                        StatusOnOff(ii)
                        insertDataToServer(temiId)
                    }
                } else {
                    Log.e("MainActivity", "JSON response does not contain data")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }).start()
    }

    private fun Apilocation(callback: (ArrayList<String>) -> Unit) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val result =
                    URL("http://10.232.153.49:8080/project/DB/get/room.php").readText()
                val jsonObject = JSONObject(result)
                val jsonArray = jsonObject.getJSONArray("data")
                val arrayList = ArrayList<String>()
                for (i in 1 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    val room = jsonObject.getString("Room")
                    arrayList.add(room)
                }
                Log.d("activity2", "Item location: $arrayList")
                callback(arrayList)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateLocationToServer(locationId: Int) {
        val client = OkHttpClient()
        val url = "http://10.232.153.49:8080/project/DB/get/temi_location.php"
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("ID", locationId.toString())
            .addFormDataPart("status_success", "success")
            .build()
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()
        // Add the following code to update the database

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("Activity2", "Error updating location: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    Log.d("Activity2", "Location updated successfully")
                } else {
                    Log.e("Activity2", "Error updating location: ${response.code}")
                }
            }
        })
    }

    private fun updateStatusToIdle() {
        val client = OkHttpClient()
        val url = "http://10.232.153.49:8080/project/DB/get/temi_location_idle.php"
        val requestBody = JSONObject().apply {
            put("status_success", "IDLE")
        }.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url(url)
            .patch(requestBody)
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("Activity2", "Error updating status: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    Log.d("Activity2", "Status updated to IDLE successfully")
                    val temiStartUrl = "http://10.232.153.49:8080/project/update/temi_start.php?ID=2"
                    val temiStartRequestBody = JSONObject().apply {
                        put("Status_start", "IDLE")
                    }.toString().toRequestBody("application/json".toMediaTypeOrNull())
                    val temiStartRequest = Request.Builder()
                        .url(temiStartUrl)
                        .put(temiStartRequestBody)
                        .build()
                    client.newCall(temiStartRequest).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            Log.e("Activity2", "Error updating temi_start: ${e.message}")
                        }

                        override fun onResponse(call: Call, response: Response) {
                            if (response.isSuccessful) {
                                Log.d("Activity2", "temi_start updated to IDLE successfully")
                                val intent = Intent(this@Activity2, MainActivity::class.java)
                                startActivity(intent)

                            } else {
                                Log.e("Activity2", "Error updating temi_start: ${response.code}")
                            }
                        }
                    })
                } else {
                    Log.e("Activity2", "Error updating status: ${response.code}")
                }
            }
        })
    }


    private var listlocation = ArrayList<String>()
    override fun onGoToLocationStatusChanged(
        location: String,
        status: String,
        descriptionId: Int,
        description: String
    ) {
        try {
            if (status == "complete") {
                if (listlocation.size > state) {
                    Thread(Runnable {
                        try {
                            val arrayText = ArrayList<String>()
                            arrayText.add("s1")
                            arrayText.add("s2")
                            arrayText.add("s3")
                            arrayText.add("s4")
                            arrayText.add("home base")
                            val arrayDelay = ArrayList<Int>()
                            arrayDelay.add(1000)
                            arrayDelay.add(1000)
                            arrayDelay.add(1000)
                            arrayDelay.add(1000)
                            arrayDelay.add(1000)

                            var success = false
                            while (!success) {
                                val url = "http://10.232.153.49:8080/project/DB/get/healthbox_symptoms.php?ID=$pp"
                                val response = URL(url).readText()
                                val jsonObject = JSONObject(response)
                                success = jsonObject.getBoolean("success")
                                updateLocationToServer(state + 1)
                                insertDataToServer(temiId)
                                StatusOnOff(ii+1)
                                if (success) {

                                    Log.d(
                                        "activity2",
                                        "updateLocationToServer: $state / insertDataToServer : $temiId / StatusOnOff : $ii "
                                    )
                                    val dataArray = jsonObject.getJSONArray("data")
                                    val dataObject = dataArray.getJSONObject(0)
                                    val ss = dataObject.getString("Status_symptoms")
                                    Log.d(
                                        "activity2",
                                        "url status: $url / dataObject : $ss "
                                    )

                                    if (ss == "success") {

                                        robot.goTo(
                                            listlocation[state],
                                            backwards = false,
                                            noBypass = false
                                        )
                                        Thread.sleep(arrayDelay[state].toLong())
                                        robot!!.speak(
                                            TtsRequest.create(
                                                arrayText[state],
                                                language = TtsRequest.Language.TH_TH
                                            )
                                        )
                                        pp++
                                    } else {
                                        state -1
                                        ii-1
                                        success = false
                                        Thread.sleep(5500)
                                    }

                                } else {
                                    Thread.sleep(1000)
                                }
                            }

                            Log.d(
                                "activity2",
                                "walk status: ${listlocation[state]} / listlocation.size : ${listlocation.size} / state : $state"
                            )

                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }).start()
                    state += 1
                    if (state >= 4) {
                        updateStatusToIdle()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun StatusOnOff(ii: Int) {
        try {
            val url = "http://10.232.153.49:8080/project/update/healthbox.php?ID=$ii&Status_Onoff=On"
            val response = URL(url).readText()
            Log.d("activity2", "StatusOnOff: $response")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    fun insertDataToServer(temiId: Int) {
        var temiId = 2
        val postData = "temi_id=$temiId"
        val url = "http://10.232.153.49:8080/project/update/temi_history.php"
        val response = URL(url).readText(Charsets.UTF_8) // make a POST request to insert data into the server
        Log.d("insertDataToServer", "response from server: $response")

    }


    override fun onStart() {
        super.onStart()
        robot.addOnGoToLocationStatusChangedListener(this)
        Apilocation { locations ->
            listlocation = locations
        }
    }
}