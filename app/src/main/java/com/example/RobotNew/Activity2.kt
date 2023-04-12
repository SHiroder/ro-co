package com.example.RobotNew

import android.annotation.SuppressLint
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
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.lang.Runnable
import java.net.URL

class Activity2 : AppCompatActivity(), OnGoToLocationStatusChangedListener {
    private lateinit var robot: Robot
    private var state: Int = -1
    private var pp: Int = 1
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_2)
        robot = Robot.getInstance()

        Thread(Runnable {
            var k = 0
            while (k < 2) {
                Thread.sleep(100)
                k += 1
            }
            try {
                robot.goTo("home base", backwards = false, noBypass = false)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }).start()
    }

    private fun Apilocation(callback: (ArrayList<String>) -> Unit) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val result =
                    URL("https://3414-110-49-17-15.ngrok-free.app/project/DB/get/room.php").readText()
                val jsonObject = JSONObject(result)
                val jsonArray = jsonObject.getJSONArray("data")
                val arrayList = ArrayList<String>()
                for (i in 0 until jsonArray.length()) {
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
        val url = "https://3414-110-49-17-15.ngrok-free.app/project/DB/get/temi_location.php"
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("ID", locationId.toString())
            .addFormDataPart("status_success", "success")
            .build()
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()
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
        val url = "https://3414-110-49-17-15.ngrok-free.app/project/DB/get/temi_location_idle.php"
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
                            robot!!.speak(
                                TtsRequest.create(
                                    arrayText[state],
                                    language = TtsRequest.Language.TH_TH
                                )
                            )
                            Thread.sleep(arrayDelay[state].toLong())

                            var success = false
                            while (!success) {
                                val url = "https://3414-110-49-17-15.ngrok-free.app/project/DB/get/healthbox_symptoms.php?ID=$pp"
                                val response = URL(url).readText()
                                val jsonObject = JSONObject(response)
                                success = jsonObject.getBoolean("success")
                                if (success) {

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
                                    }
                                    pp++
                                } else {
                                    Thread.sleep(1000)
                                    continue
                                }


                            }

                            Log.d(
                                "activity2",
                                "walk status: ${listlocation[state]} / listlocation.size : ${listlocation.size} / state : $state"
                            )
                            updateLocationToServer(state + 1)
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

    override fun onStart() {
        super.onStart()
        robot.addOnGoToLocationStatusChangedListener(this)
        Apilocation { locations ->
            listlocation = locations
        }
    }

}