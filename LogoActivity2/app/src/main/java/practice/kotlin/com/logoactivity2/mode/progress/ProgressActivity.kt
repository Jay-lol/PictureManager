package practice.kotlin.com.logoactivity2.mode.progress

import android.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Message
import kotlinx.android.synthetic.main.activity_progress.*
import practice.kotlin.com.logoactivity2.Constants.Companion.MODE
import practice.kotlin.com.logoactivity2.Constants.Companion.MODE_ALL
import practice.kotlin.com.logoactivity2.R
import practice.kotlin.com.logoactivity2.SharedData

class ProgressActivity : AppCompatActivity() {
    private var mStateHandler: StateHandler = StateHandler()
    //공용 data 가져와야할 값
    private var mCurrentFileCount: Int = 0
    private var mTotalFileCount: Int = 0
    private var mPreviousCount: Int = 0
    private var mMode: Int = 0
    private var mIsRunning = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_progress)

        mMode = getIntent().getIntExtra(MODE, 0)

        if (mMode == 0) {
            mTotalFileCount = SharedData.instance.allModeTotalFileCount
        } else {
            mTotalFileCount = SharedData.instance.selectedModeTotalFileCount
        }

        setInitState()
        mCounterThread.start()
    }

    private fun setInitState() {
        progressbar.max = mTotalFileCount
        progressbar.progress = 0
        progressbar.secondaryProgress = 1

        button.setOnClickListener {
            if (mCurrentFileCount == mTotalFileCount) {
                finish()
            } else {
                val builder = AlertDialog.Builder(this@ProgressActivity)
                builder.setTitle("전송 종료 확인")
                    .setMessage("사진 전송을 취소하시겠습니까? \n 이전까지 전송된 파일은 삭제되지 않습니다.")
                    .setCancelable(false) // 뒤로 버튼 클릭시 취소 설정
                    .setPositiveButton("확인") { dialog, whichButton ->
                        if (mMode == 0) {
                            stopService(SharedData.instance.allModeSenderIntent)
                            stopService(SharedData.instance.allModeProgressServiceIntent)
                        } else {
                            stopService(SharedData.instance.selectedModeSenderIntent)
                            stopService(SharedData.instance.selectedModeProgressServiceIntent)
                        }
                        finish()
                        mIsRunning = false
                    }
                    .setNegativeButton("취소") { dialog, whichButton -> dialog.cancel() }
                val dialog = builder.create() // 알림창 객체 생성
                dialog.show() // 알림창 띄우기
            }
        }
    }

    private var mCounterThread = Thread(Runnable {
        while (mIsRunning) {
            if (mMode == MODE_ALL) {
                mCurrentFileCount = SharedData.instance.allModeFileCount
            } else {
                mCurrentFileCount = SharedData.instance.selectedModeFileCount
            }

            if (mCurrentFileCount > mPreviousCount) {
                mPreviousCount = mCurrentFileCount
                mStateHandler.sendEmptyMessage(mCurrentFileCount)
            }
        }
    })

    private inner class StateHandler : Handler() {
        override fun handleMessage(msg: Message) {
            progressbar.progress = msg.what
            progressbar.secondaryProgress = msg.what + 1
            progress_text.text = "전체 " + mTotalFileCount + " 개 중 " + msg.what + "개 전송\n"

            if (msg.what == mTotalFileCount && mTotalFileCount != 0) {
                progress_text.text = "전송이 완료되었습니다!"
                mIsRunning = false
                button.text = "확인"
            }
        }
    }
}

