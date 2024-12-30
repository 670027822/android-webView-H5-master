package com.example.test;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class JobSchedulerService extends JobService {
    private static final String TAG = "JobSchedulerService";

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d(TAG, "JobService onStartJob");
        
        // 启动前台服务和双进程保活服务
        startServices();
        
        // 返回 false 表示任务已完成，返回 true 表示任务会在后台继续运行
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d(TAG, "JobService onStopJob");
        // 返回 true 表示需要重新调度这个任务
        return true;
    }

    private void startServices() {
        // 启动前台服务
        Intent foregroundIntent = new Intent(this, ForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(foregroundIntent);
        } else {
            startService(foregroundIntent);
        }

        // 启动双进程保活服务
        startService(new Intent(this, LocalService.class));
        startService(new Intent(this, RemoteService.class));
    }
} 