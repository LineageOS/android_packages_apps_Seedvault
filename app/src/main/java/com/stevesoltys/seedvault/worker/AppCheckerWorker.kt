/*
 * SPDX-FileCopyrightText: 2024 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package com.stevesoltys.seedvault.worker

import android.content.Context
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.util.Log
import androidx.work.BackoffPolicy.EXPONENTIAL
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy.REPLACE
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.stevesoltys.seedvault.BackupStateManager
import com.stevesoltys.seedvault.repo.Checker
import com.stevesoltys.seedvault.ui.notification.BackupNotificationManager
import com.stevesoltys.seedvault.ui.notification.NOTIFICATION_ID_CHECKING
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Duration

class AppCheckerWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    companion object {
        private val TAG = AppCheckerWorker::class.simpleName
        private const val PERCENT = "percent"
        internal const val UNIQUE_WORK_NAME = "com.stevesoltys.seedvault.APP_BACKUP_CHECK"

        fun scheduleNow(context: Context, percent: Int) {
            check(percent in 0..100) { "Percent $percent out of bounds." }
            val data = Data.Builder().putInt(PERCENT, percent).build()
            val workRequest = OneTimeWorkRequestBuilder<AppCheckerWorker>()
                .setExpedited(RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setBackoffCriteria(EXPONENTIAL, Duration.ofSeconds(10))
                .setInputData(data)
                .build()
            val workManager = WorkManager.getInstance(context)
            Log.i(TAG, "Asking to check $percent% of app backups now...")
            workManager.enqueueUniqueWork(UNIQUE_WORK_NAME, REPLACE, workRequest)
        }
    }

    private val log = KotlinLogging.logger {}
    private val backupStateManager: BackupStateManager by inject()
    private val checker: Checker by inject()
    private val nm: BackupNotificationManager by inject()

    override suspend fun doWork(): Result {
        log.info { "Start worker $this ($id)" }
        if (backupStateManager.isBackupRunning.first()) {
            Log.i(TAG, "isBackupRunning was true, so retrying later...")
            return Result.retry()
        }
        try {
            setForeground(createForegroundInfo())
        } catch (e: Exception) {
            log.error(e) { "Error while running setForeground: " }
        }
        val percent = inputData.getInt(PERCENT, -1)
        check(percent in 0..100) { "Percent $percent out of bounds." }

        checker.check(percent)
        return Result.success()
    }

    private fun createForegroundInfo() = ForegroundInfo(
        NOTIFICATION_ID_CHECKING,
        nm.getCheckNotification().build(),
        FOREGROUND_SERVICE_TYPE_DATA_SYNC,
    )
}
