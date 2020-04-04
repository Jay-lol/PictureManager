package practice.kotlin.com.logoactivity2.mode.auto

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import practice.kotlin.com.logoactivity2.Constants
import practice.kotlin.com.logoactivity2.Constants.Companion.TAG
import practice.kotlin.com.logoactivity2.SharedData
import java.io.BufferedInputStream
import java.io.DataOutputStream
import java.io.FileInputStream
import java.net.Socket
import java.util.ArrayList

class SendAutoItemService : Service() {
    private var mServerIp: String? = null
    private var mSelectImageList: ArrayList<AutoImageData> = ArrayList()

    private val mImages = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    private val mVideos = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    private var mIsStop = false

    override fun onBind(intent: Intent?): IBinder {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val sender = FileSender()
        //start
        val imageProjection =
            arrayOf(MediaStore.Images.Media.DATA, MediaStore.Images.Media.DATE_TAKEN)
        val videoProjection =
            arrayOf(MediaStore.Video.Media.DATA, MediaStore.Video.Media.DATE_TAKEN)

        val lastDate = intent?.getStringExtra(Constants.LAST_DATE)
        mServerIp = intent?.getStringExtra(Constants.SERVER_IP)

        // 사진 파일 불러오기


        val imageCursor = contentResolver.query(
            mImages,
            imageProjection,
            null,
            null,
            MediaStore.Images.Media.DATE_TAKEN + " desc "
        ) // 내림차순
        val imageDateIndex = imageCursor!!.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
        val imageDataIndex = imageCursor.getColumnIndex(MediaStore.Images.Media.DATA)
        // 최신 사진부터 전송
        if (imageCursor != null && imageCursor.count > 0) {
            imageCursor.moveToFirst() //내림차순이라 첫번째가 최신 사진
            while (true) {
                val imageDate = imageCursor.getString(imageDateIndex)
                val imageData = imageCursor.getString(imageDataIndex)
                // 최종 동기화 날짜보다 이전 날짜일 경우 중지
                if (!isValidDate(lastDate, imageDate)) {
                    break
                }
                val index = imageData.lastIndexOf("/") + 1
                val file = imageData.substring(index, imageData.length)
                Log.d("TAG","imageData : " + imageData + " file : " + file);
                val image = AutoImageData()
                image.mFile = file
                image.mData = imageData
                image.mDate = imageDate
                imageDate.length
                mSelectImageList.add(image)

                if (!imageCursor.moveToNext()) {
                    break
                }
            }
        }

        // 동영상 파일 불러오기
        val videoCursor =
            contentResolver.query(
                mVideos,
                videoProjection,
                null,
                null,
                MediaStore.Video.Media.DATE_TAKEN + " desc "
            )
        val videoDateIndex = videoCursor!!.getColumnIndex(MediaStore.Video.Media.DATE_TAKEN)
        val videoDataIndex = videoCursor.getColumnIndex(MediaStore.Video.Media.DATA)
        // 최신 동영상부터 전송
        if (videoCursor != null && videoCursor.count > 0) {
            videoCursor.moveToFirst()
            while (true) {
                val videoDate = videoCursor.getString(videoDateIndex)
                val videoData = videoCursor.getString(videoDataIndex)
                // 최종 동기화 날짜보다 이전 날짜일 경우 중지
                if (!isValidDate(lastDate, videoDate)) {
                    break
                }
                val index = videoData.lastIndexOf("/") + 1
                val file = videoData.substring(index, videoData.length)
                val image = AutoImageData()
                image.mFile = file
                image.mData = videoData
                image.mDate = videoDate
                mSelectImageList.add(image)

                if (!videoCursor.moveToNext()) {
                    break
                }
            }
        }
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
                dos.writeInt(Constants.MODE_ALL)
                dos.flush()

                // 개수
                dos.writeInt(mSelectImageList.size)
                dos.flush()
                SharedData.instance.allModeTotalFileCount = mSelectImageList.size

                for (i in mSelectImageList.indices) {
                    SharedData.instance.allModeFileCount = SharedData.instance.allModeFileCount + 1
                    sock = Socket(mServerIp, mPort)
                    dos = DataOutputStream(sock.getOutputStream())

                    // 날짜
                    dos.writeUTF(mSelectImageList[i].mDate)
                    dos.flush()

                    // 파일명
                    dos.writeUTF(mSelectImageList[i].mFile)
                    dos.flush()

                    Log.d(TAG, "날짜 및 파일 이름 전송 완료")

                    val fName = mSelectImageList[i].mData
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

    private fun isValidDate(lastDate: String?, Date: String): Boolean {
        if (lastDate != null) {
            return lastDate.compareTo(Date) <= 0
        }
        return false
    }
}