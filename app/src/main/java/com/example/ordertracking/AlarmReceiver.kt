package com.example.ordertracking

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Build
import android.preference.PreferenceManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import kotlin.coroutines.CoroutineContext

class AlarmReceiver : BroadcastReceiver() {
    private var CHANNEL_ID = "channel"

    interface OnTaskCompleted {
        fun onTaskCompleted(string: String)
    }

    class NetworkTask : AsyncTask<Void?, Void?, Void?> {
        lateinit var taskCompleted: OnTaskCompleted
        lateinit var context: Context

        constructor(context: Context) {
            this.context = context
        }

        override fun doInBackground(vararg params: Void?): Void? {
            val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
            val times =  sharedPref.getInt("1", 0)

            val doc: Document = Jsoup.connect("https://www.bgpost.bg/IPSWebTracking/IPSWeb_item_events.asp?itemid=  &Submit=Submit").get()
            var rows = doc.select("#200 tr")

            val newRows = rows.size - 2
            if (newRows > times)
            {
                taskCompleted.onTaskCompleted(rows.last().text())
            }

            with (sharedPref.edit()) {
                putInt("1", newRows)
                apply()
            }

            return null
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.println(Log.INFO, "Receiver", "received")

        NetworkTask(context as Context).let { o ->
            o.taskCompleted = object : OnTaskCompleted {
                override fun onTaskCompleted(string: String) {
                    createNotificationChannel(context as Context)
                    showNotification(context, string)
                }
            }

            o.execute()
        }
    }

    private fun createNotificationChannel(context: Context) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "channel" // getString(R.string.channel_name)
            val descriptionText = "description" //      getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            // Register the channel with the system
            val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private var notification = 0

    private fun showNotification(context: Context, string: String) {

        var builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle("New update")
                .setContentText(string)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(string)
                )
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(context)) {
            // notificationId is a unique int for each notification that you must define
            notify(notification++, builder.build())
        }
    }
}
