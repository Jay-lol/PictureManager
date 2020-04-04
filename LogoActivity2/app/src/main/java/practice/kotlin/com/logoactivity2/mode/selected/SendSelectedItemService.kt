package practice.kotlin.com.logoactivity2.mode.selected

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import practice.kotlin.com.logoactivity2.Constants
import practice.kotlin.com.logoactivity2.Constants.Companion.TAG
import practice.kotlin.com.logoactivity2.SharedData
import java.io.BufferedInputStream
import java.io.DataOutputStream
import java.io.FileInputStream
import java.net.Socket

class SendSelectedItemService : Service(){
    private var mServerIp: String? = null
    private var mDirName: String? = null
    private var mIsStop = false
    private val mSharedData = SharedData.instance

    override fun onBind(intent: Intent?): IBinder {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        mServerIp = intent.getStringExtra(Constants.SERVER_IP)
        mDirName = intent.getStringExtra(Constants.DIR_NAME)
        var sender = FileSender()
        sender.start()
        return Service.START_NOT_STICKY
    }

    private inner class FileSender : Thread() {
        private var mPort: Int = Constants.FILE_SEND_PORT
        override fun run() {
            // 서버 연결
            try {
                var sock = Socket(mServerIp, mPort)

                var dos = DataOutputStream(sock.getOutputStream())
                var fis: FileInputStream? = null
                var bis: BufferedInputStream? = null

                // 모드
                dos.writeInt(Constants.MODE_SELECT)
                dos.flush()

                // 폴더명 지정
                mDirName?.let { dos.writeUTF(it) }
                dos.flush()

                // 개수
                mSharedData.selectedImageList.size.let { dos.writeInt(it) }
                dos.flush()
                SharedData.instance.selectedModeTotalFileCount = mSharedData.selectedImageList.size

                for (i in 0 until mSharedData.selectedImageList.size) {
                    sock = Socket(mServerIp, mPort)
                    SharedData.instance
                        .selectedModeFileCount = SharedData.instance.selectedModeFileCount + 1
                    // 파일 이름 보내기
                    dos = DataOutputStream(sock.getOutputStream())
                    dos.writeUTF(mSharedData.selectedImageList.get(i).mFile)
                    dos.flush()

                    Log.d(TAG, "파일 이름 전송 완료")

                    val fName = mSharedData.selectedImageList.get(i).mData
                    fis = FileInputStream(fName)
                    bis = BufferedInputStream(fis)
                    dos = DataOutputStream(sock.getOutputStream())

                    var len: Int
                    val size = 1500
                    val data = ByteArray(size)
                    var isRunning = true
                    Log.d(TAG, "파일 전송 시작")
                    len = bis.read(data)
                    isRunning = len != -1
                    while (isRunning) {
                        dos.write(data, 0, len)
                        len = bis.read(data)
                        isRunning = len != -1
                    }
                    dos.flush()
                    dos.close()
                    bis?.close()
                    fis?.close()
                    sock.close()
                    if (mIsStop) {
                        break
                    }
                }
                Log.d(TAG, "파일 전송 완료")
                stopSelf()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        mIsStop = true
        super.onDestroy()
    }
}
