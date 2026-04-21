package com.swipeapply.app.notifications

import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters

class IntroFollowUpWorker(
    context: android.content.Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        AppNotificationService.ensureChannels(applicationContext)
        if (!AppNotificationService.canNotify(applicationContext)) return Result.success()

        val recipientName = inputData.getString(KEY_RECIPIENT_NAME).orEmpty().ifBlank { "your contact" }
        val companyName = inputData.getString(KEY_COMPANY_NAME).orEmpty().ifBlank { "the company" }

        AppNotificationService.post(
            context = applicationContext,
            channelId = AppNotificationService.CHANNEL_FOLLOW_UP,
            notificationId = ("intro_followup_${recipientName}_${companyName}").hashCode(),
            title = "Follow up on your intro",
            message = "Check in with $recipientName at $companyName if you have not heard back yet."
        )

        return Result.success()
    }

    companion object {
        private const val KEY_RECIPIENT_NAME = "recipient_name"
        private const val KEY_COMPANY_NAME = "company_name"

        fun inputData(recipientName: String, companyName: String): Data {
            return Data.Builder()
                .putString(KEY_RECIPIENT_NAME, recipientName)
                .putString(KEY_COMPANY_NAME, companyName)
                .build()
        }
    }
}
