
package com.example.rental_system;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class RentReminderScheduler {

    private static final String WORK_NAME =
            "monthly_rent_reminders";

    private static final String DEMO_WORK_NAME =
            "rent_reminder_demo_test";

    public static void schedule(Context context) {

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest reminderWork =
                new PeriodicWorkRequest.Builder(
                        RentReminderWorker.class,
                        24,
                        TimeUnit.HOURS)
                        .setConstraints(constraints)
                        .build();

        WorkManager.getInstance(
                context.getApplicationContext()
        ).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                reminderWork
        );
    }

    public static void runDemoTest(Context context) {

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        Data inputData = new Data.Builder()
                .putBoolean("demo_run", true)
                .build();

        OneTimeWorkRequest demoWork =
                new OneTimeWorkRequest.Builder(
                        RentReminderWorker.class)
                        .setConstraints(constraints)
                        .setInputData(inputData)
                        .build();

        WorkManager.getInstance(
                context.getApplicationContext()
        ).enqueueUniqueWork(
                DEMO_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                demoWork
        );
    }
}
