package com.example.test;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class RemoteService extends Service {
    private static final String TAG = "RemoteService";
    
    private final IKeepAliveInterface.Stub binder = new IKeepAliveInterface.Stub() {
        @Override
        public void keepAlive() throws RemoteException {
            Log.d(TAG, "RemoteService is alive");
        }
    };
    
    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            IKeepAliveInterface localService = IKeepAliveInterface.Stub.asInterface(service);
            try {
                localService.keepAlive();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "LocalService died, restarting...");
            // 重启本地服务
            startAndBindLocalService();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        startAndBindLocalService();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private void startAndBindLocalService() {
        Intent intent = new Intent(this, LocalService.class);
        startService(intent);
        bindService(intent, connection, Context.BIND_IMPORTANT);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(connection);
    }
} 