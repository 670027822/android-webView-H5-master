package com.example.test;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class LocalService extends Service {
    private static final String TAG = "LocalService";
    
    private final IKeepAliveInterface.Stub binder = new IKeepAliveInterface.Stub() {
        @Override
        public void keepAlive() throws RemoteException {
            Log.d(TAG, "LocalService is alive");
        }
    };
    
    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            IKeepAliveInterface remoteService = IKeepAliveInterface.Stub.asInterface(service);
            try {
                remoteService.keepAlive();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "RemoteService died, restarting...");
            // 重启远程服务
            startAndBindRemoteService();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        startAndBindRemoteService();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private void startAndBindRemoteService() {
        Intent intent = new Intent(this, RemoteService.class);
        startService(intent);
        bindService(intent, connection, Context.BIND_IMPORTANT);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(connection);
    }
} 