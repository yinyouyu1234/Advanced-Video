package io.agora.rtc.ss;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import io.agora.rtc.ss.aidl.IErrorNotification;
import io.agora.rtc.ss.aidl.IScreenShare;
import io.agora.rtc.ss.capture.ScreenSharingService;
import io.agora.rtc.video.VideoEncoderConfiguration;

public class ScreenShare {
    private static final String TAG = ScreenShare.class.getSimpleName();
    private static IScreenShare mScreenShareSvc;

    private static ServiceConnection mScreenShareConn = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mScreenShareSvc = IScreenShare.Stub.asInterface(service);

            try {
                mScreenShareSvc.registerCallback(mErrCallback);
                mScreenShareSvc.startShare();
            } catch (RemoteException e) {
                e.printStackTrace();
                Log.e(TAG, Log.getStackTraceString(e));
            }

        }
        public void onServiceDisconnected(ComponentName className) {
            mScreenShareSvc = null;
        }
    };

    private static IErrorNotification mErrCallback = new IErrorNotification.Stub() {
        /**
         * This is called by the remote service to tell us about error happened.
         * Note that IPC calls are dispatched through a thread
         * pool running in each process, so the code executing here will
         * NOT be running in our main thread like most other things -- so,
         * if to update the UI, we need to use a Handler to hop over there.
         */
        public void onError(int error) {
            Log.e(TAG, "screen share service error happened: " + error);
        }
    };

    public static void start(Context context, String appId, String channelName, int uid, VideoEncoderConfiguration vec) {
        if (mScreenShareSvc == null) {
            Intent intent = new Intent(context, ScreenSharingService.class);
            intent.putExtra(Constant.APP_ID, appId);
            intent.putExtra(Constant.CHANNEL_NAME, channelName);
            intent.putExtra(Constant.UID, uid);
            intent.putExtra(Constant.WIDTH, vec.dimensions.width);
            intent.putExtra(Constant.HEIGHT, vec.dimensions.height);
            intent.putExtra(Constant.FRAME_RATE, vec.frameRate);
            intent.putExtra(Constant.BIT_RATE, vec.bitrate);
            intent.putExtra(Constant.ORIENTATION_MODE, vec.orientationMode.getValue());
            context.bindService(intent, mScreenShareConn, Context.BIND_AUTO_CREATE);
        } else {
            try {
                mScreenShareSvc.startShare();
            } catch (RemoteException e) {
                e.printStackTrace();
                Log.e(TAG, Log.getStackTraceString(e));
            }
        }

    }

    public static void stop(Context context) {
        if (mScreenShareSvc != null) {
            try {
                mScreenShareSvc.stopShare();
                mScreenShareSvc.unregisterCallback(mErrCallback);
            } catch (RemoteException e) {
                e.printStackTrace();
                Log.e(TAG, Log.getStackTraceString(e));
            } finally {
                mScreenShareSvc = null;
            }
        }
        context.unbindService(mScreenShareConn);
    }
}
