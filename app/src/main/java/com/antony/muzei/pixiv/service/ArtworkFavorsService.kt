/*
 *     This file is part of PixivforMuzei3.
 *
 *     PixivforMuzei3 is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program  is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.antony.muzei.pixiv.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.JobIntentService
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.antony.muzei.pixiv.R

/**
 * [Service] for add artwork to favors(bookmark)
 */
class ArtworkFavorsService : JobIntentService() {

    override fun onHandleWork(intent: Intent) {
        createNotification(this).also {
            ContextCompat.getSystemService(this, NotificationManager::class.java)
                ?.notify(0, it)
        }
    }

    private fun createNotification(context: Context): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.getSystemService(context, NotificationManager::class.java)?.also { manager ->
                NotificationChannel(
                    SERVICE_ARTWORK_NOTIFICATION_CHANNEL_ID,
                    SERVICE_ARTWORK_NOTIFICATION_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    enableLights(false)
                    enableVibration(false)
                    setSound(null, null)
                    setShowBadge(false)
                }.also { channel ->
                    manager.createNotificationChannel(channel)
                }
            }
        }
        return NotificationCompat.Builder(context, SERVICE_ARTWORK_NOTIFICATION_CHANNEL_ID)
            .apply {
                setSmallIcon(R.drawable.muzei_launch_command)
                setContentTitle("My notification")
                setContentText("Hello World!")
            }
            .build()
    }

}
