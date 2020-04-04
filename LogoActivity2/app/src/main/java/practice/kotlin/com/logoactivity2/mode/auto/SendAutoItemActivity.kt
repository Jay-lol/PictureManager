package practice.kotlin.com.logoactivity2.mode.auto

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import practice.kotlin.com.logoactivity2.Constants
import practice.kotlin.com.logoactivity2.R
import practice.kotlin.com.logoactivity2.SharedData
import practice.kotlin.com.logoactivity2.mode.progress.ProgressActivity
import practice.kotlin.com.logoactivity2.mode.progress.ProgressNotificationService

class SendAutoItemActivity : AppCompatActivity() {

    private var mSharedData = SharedData.instance
    private lateinit var mProgressIntent: Intent
    private lateinit var mLastDate: String
    private lateinit var mServerIP: String
    private var mIsNewFile = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_auto_item)

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)
        } else {
            mSharedData.isConnected = false
            getFileInfo()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            1 -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 권한 허가
                    getFileInfo()
                } else {
                    //권한 거부
                    val dialogBuilder = AlertDialog.Builder(this@SendAutoItemActivity)
                        .setTitle("Permission Error")
                        .setMessage(
                            "사진 정보를 읽기 위해 권한에 동의해야 합니다.\n" +
                                    "어플을 종료합니다."
                        )
                        .setPositiveButton(
                            "확인"
                        ) { dialog, which ->
                            ActivityCompat.finishAffinity(this@SendAutoItemActivity)
                            System.exit(0)
                        }
                        .setIcon(R.mipmap.ic_launcher)
                    val dialog = dialogBuilder.create()
                    dialog.show()
                }
                return
            }
        }
    }

    private fun getFileInfo() {
        mServerIP = intent.getStringExtra(Constants.IP)
        mLastDate = intent.getStringExtra(Constants.DATE)

        getImageInfo()
        getVideoInfo()
        setAlertDialog()
    }

    private fun getImageInfo() {
        val images = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val imagesProjection = arrayOf(MediaStore.Images.Media.DATE_TAKEN)

        // 사진 파일 불러오기
        val imageCursor =
            contentResolver.query(images, imagesProjection, null, null, MediaStore.Images.Media.DATE_TAKEN + " desc ")
        val imageDateIndex = imageCursor!!.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
        // 최신 사진으로 이동
        if (imageCursor != null && imageCursor.count > 0) {
            imageCursor.moveToFirst()
            val imageDate = imageCursor.getString(imageDateIndex)
            // 최종 동기화 날짜보다 이전 날짜일 경우 중지
            if (isValidDate(mLastDate, imageDate)) {
                mIsNewFile = true
            }
        }
    }

    private fun getVideoInfo() {
        val videos = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val videosProjection = arrayOf(MediaStore.Video.Media.DATE_TAKEN)
        // 동영상 파일 불러오기
        val videoCursor =
            contentResolver.query(videos, videosProjection, null, null, MediaStore.Video.Media.DATE_TAKEN + " desc ")
        val videoDateIndex = videoCursor!!.getColumnIndex(MediaStore.Video.Media.DATE_TAKEN)
        // 최신 동영상으로 이동
        if (videoCursor != null && videoCursor.count > 0) {
            videoCursor.moveToFirst()

            val videoDate = videoCursor.getString(videoDateIndex)
            // 최종 동기화 날짜보다 이전 날짜일 경우 중지
            if (isValidDate(mLastDate, videoDate)) {
                mIsNewFile = true
            }
        }
    }

    private fun setAlertDialog() {
        val alert = AlertDialog.Builder(this)
        if (!mIsNewFile) {
            alert.setTitle("알림")
            alert.setMessage("새로 찍은 사진이나 동영상이 없습니다.")
            alert.setPositiveButton(
                "확인"
            ) { dialog, whichButton -> finish() }
            alert.show()
        } else {
            startSendService()
            finish()
        }
    }

    private fun startSendService() {
        mSharedData.allModeSenderIntent.putExtra(Constants.LAST_DATE, mLastDate)
        mSharedData.allModeSenderIntent.putExtra(Constants.SERVER_IP, mServerIP)
        mSharedData.allModeSenderIntent.setClass(this, SendAutoItemService::class.java)
        startService(mSharedData.allModeSenderIntent)
        mSharedData.allModeProgressServiceIntent.putExtra(Constants.MODE, Constants.MODE_ALL) //전체모드
        mSharedData.allModeProgressServiceIntent.setClass(this, ProgressNotificationService::class.java)
        startService(mSharedData.allModeProgressServiceIntent)

        mProgressIntent = Intent()
        mProgressIntent.putExtra(Constants.MODE, Constants.MODE_ALL) //전체모드
        mProgressIntent.setClass(this, ProgressActivity::class.java)
        startActivity(mProgressIntent)
    }

    // 마지막 동기화 날짜와 전송할 사진의 날짜 비교
    private fun isValidDate(lastDate: String, date: String): Boolean {
        return lastDate.compareTo(date) <= 0
    }
}
