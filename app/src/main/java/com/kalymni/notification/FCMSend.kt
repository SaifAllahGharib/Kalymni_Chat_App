package com.kalymni.notification

import android.content.Context
import android.os.StrictMode
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class FCMSend {
    companion object {
        val BASE_URL = "https://fcm.googleapis.com/fcm/send"
        val SERVER_KEY =
            "key=AAAA3uOAjwo:APA91bE1Qqsgp2V_5LLtZz-YB8MmyHRyi_ZZFABf7Bnf4dToaDoMzJIoW5HkFFytOum8D7Jw0WSCVrXW4hlFlxzVShbaRfeoy7nbqW7RT5WVt7c8ELEO_C16Rv136eiY_o7YgNoqHQW9"

        fun pushNotification(context: Context, token: String, title: String, msg: String) {
            val policy: StrictMode.ThreadPolicy =
                StrictMode.ThreadPolicy.Builder().permitAll().build()
            StrictMode.setThreadPolicy(policy)

            val queue = Volley.newRequestQueue(context)

            try {
                val json = JSONObject()
                json.put("to", token)

                val notification = JSONObject()
                notification.put("title", title)
                notification.put("body", msg)

                json.put("notification", notification)

                val jsonObjectRequest = object :
                    JsonObjectRequest(
                        com.android.volley.Request.Method.POST,
                        BASE_URL,
                        json,
                        Response.Listener { _ -> },
                        Response.ErrorListener {}) {
                    override fun getHeaders(): MutableMap<String, String> {
                        val params: MutableMap<String, String> = HashMap()
                        params["Content-Type"] = "application/json"
                        params["Authorization"] = SERVER_KEY
                        return params
                    }
                }

                queue.add(jsonObjectRequest)

            } catch (e: Exception) {
                println(e)
            }
        }
    }
}