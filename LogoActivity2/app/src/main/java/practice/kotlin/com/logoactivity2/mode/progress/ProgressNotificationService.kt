package practice.kotlin.com.logoactivity2.mode.progress

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import practice.kotlin.com.logoactivity2.Constants
import practice.kotlin.com.logoactivity2.MainActivity
import practice.kotlin.com.logoactivity2.R
import practice.kotlin.com.logoactivity2.SharedData

class ProgressNotificationService : Service() {
    private var mCurrentFileCount: Int = 0
    private var mTotalFileCount: Int = 0
    private var mMode: Int = 0

    private lateinit var mPendingIntent: PendingIntent
    private lateinit var mBuilder: Notification.Builder

    private var mIsStop = false
    private val mNotificationId = 1000
    private val mTitle = "사진 동기화"
    private val mNotificationChannelId = "id"
    private val mNotificationChannelName = "사진 전송 결과"
    private lateinit var mNotificationManager: NotificationManager
    private lateinit var mNotificationChannel: NotificationChannel

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        mMode = intent.getIntExtra(Constants.MODE, 0)

        setNotification()
        setThread()

        return Service.START_NOT_STICKY // 서비스 재실행 하지 않음
    }

    private fun setNotification() {
        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mNotificationChannel = NotificationChannel(
                mNotificationChannelId,
                mNotificationChannelName,
                NotificationManager.IMPORTANCE_DEFAULT
            )

            mNotificationChannel.description = "사진 전송이 취소 또는 완료 되었을 때 발생하는 알림입니다."
            mNotificationChannel.enableLights(true)
            mNotificationChannel.lightColor = Color.YELLOW
            mNotificationChannel.setSound(null, null)
            mNotificationChannel.enableVibration(false)
            mNotificationChannel.vibrationPattern = longArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0)
            mNotificationChannel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            mNotificationManager.createNotificationChannel(mNotificationChannel)
            mBuilder = Notification.Builder(applicationContext, mNotificationChannelId)
        } else {
            mBuilder = Notification.Builder(applicationContext)
        }
    }

    private fun setThread() {
        val notificationThread = Thread(
            Runnable {
                while (!mIsStop) {
                    if (mMode == 0) {
                        mCurrentFileCount = SharedData.instance.allModeFileCount
                        mTotalFileCount = SharedData.instance.allModeTotalFileCount
                    } else {
                        mCurrentFileCount = SharedData.instance.selectedModeFileCount
                        mTotalFileCount = SharedData.instance.selectedModeTotalFileCount
                    }

                    if (mCurrentFileCount == mTotalFileCount && mTotalFileCount != 0) {
                        // 완료
                        mPendingIntent = PendingIntent.getActivity(
                            applicationContext, 0,
                            Intent(applicationContext, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT
                        )
                        mBuilder.setContentTitle(mTitle)
                            .setContentText("동기화 완료")
                            .setSmallIcon(R.mipmap.ic_launcher_round)
                            .setContentIntent(mPendingIntent)
                            .setOngoing(false)
                            .setAutoCancel(true)
                        mBuilder.build().flags = Notification.FLAG_AUTO_CANCEL
                        mNotificationManager.notify(mNotificationId + 1, mBuilder.build())
                        mIsStop = true
                        stopSelf()
                    }
                }
            }
        )
        notificationThread.start()
    }

    override fun onDestroy() {
        if (!mIsStop) {
            mIsStop = true
            mBuilder.setContentTitle(mTitle)
                .setContentText("전송 취소")
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setOngoing(false)
                .setAutoCancel(true)
            mNotificationManager.notify(mNotificationId + 1, mBuilder.build())
        }
        super.onDestroy()
    }
}