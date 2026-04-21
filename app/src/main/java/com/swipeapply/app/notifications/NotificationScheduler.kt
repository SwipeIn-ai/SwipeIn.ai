package com.swipeapply.app.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object NotificationScheduler {

    private const val PERIODIC_NOTIFICATION_WORK = "periodic_notification_checks"

    fun schedulePeriodicChecks(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<NotificationSignalsWorker>(12, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_NOTIFICATION_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun scheduleIntroFollowUp(
        context: Context,
        recipientName: String,
        companyName: String
    ) {
        val safeRecipient = recipientName.ifBlank { "contact" }
        val safeCompany = companyName.ifBlank { "company" }
        val uniqueName = "intro_follow_up_${safeRecipient}_${safeCompany}".lowercase()
            .replace("\\s+".toRegex(), "_")

        val request = OneTimeWorkRequestBuilder<IntroFollowUpWorker>()
            .setInitialDelay(48, TimeUnit.HOURS)
            .addTag("intro_follow_up")
            .setInputData(
                IntroFollowUpWorker.inputData(
                    recipientName = safeRecipient,
                    companyName = safeCompany
                )
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueName,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
