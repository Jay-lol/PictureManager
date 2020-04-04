package practice.kotlin.com.logoactivity2.mode.selected

import android.Manifest
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.AdapterView
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_send_selected_item.*
import practice.kotlin.com.logoactivity2.Constants
import practice.kotlin.com.logoactivity2.R
import practice.kotlin.com.logoactivity2.SharedData
import practice.kotlin.com.logoactivity2.mode.progress.ProgressActivity
import practice.kotlin.com.logoactivity2.mode.progress.ProgressNotificationService
import kotlin.system.exitProcess


class SendSelectedItemActivity : AppCompatActivity(), AdapterView.OnItemClickListener {
    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val adapter = parent?.adapter as ImageAdapter
        val rowData = adapter.getItem(position) as SelectedImageData
        val curCheckState = rowData.mCheckedState

        rowData.mCheckedState = !curCheckState

        if (rowData.mCheckedState) {
            mAllFileList.add(rowData)
        } else {
            mAllFileList.remove(rowData);
        }
        adapter.notifyDataSetChanged()
    }


    private var mSharedData = SharedData.instance
    private lateinit var mFindImageList: FindImageList
    private var mAllImageList = ArrayList<SelectedImageData>()
    private var mAllVideoList =  ArrayList<SelectedImageData>()
    private lateinit var mProgressDialog: ProgressDialog
    private var mAllFileList = ArrayList<SelectedImageData>()
    private lateinit var mImageAdapter : ImageAdapter
    private lateinit var mVideoAdapter : ImageAdapter
    private lateinit var mProgressIntent: Intent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_selected_item)

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)
        } else {
            init()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when(requestCode){
            1 -> {
                if(grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    // 권한 허가
                    init()
                } else{

                    val digBuilder = AlertDialog.Builder(this)
                        .setTitle("권한 오류")
                        .setMessage("사진 정보를 읽기 위해 권한에 동의해야 합니다\n"+ "어플을 종료합니다")
                        .setPositiveButton("확인"){
                            dialog, which -> ActivityCompat.finishAffinity(this)
                            exitProcess(0)
                        }
                        .setIcon(R.mipmap.ic_launcher)
                    val dig = digBuilder.create()
                    dig.show()
                }
                return
            }
        }


    }

    private fun init() {
        var serverIp = intent.getStringExtra(Constants.IP)
        mFindImageList = FindImageList()
        mFindImageList.execute()

        GridImageList.setOnItemClickListener(this)
        GridVideoList.setOnItemClickListener(this)

        SelectOkButton.setOnClickListener ( object : View.OnClickListener{
            override fun onClick(v: View?) {

                mSharedData.selectedImageList.clear()
                for (i in mAllFileList.indices) {
                    if (mAllFileList[i].mCheckedState) {
                        val selectImage = mAllFileList[i]
                        val data_info = mAllFileList[i].mData
                        val index = data_info?.let { it.lastIndexOf("/") + 1 }
                        val file = data_info?.let { index?.let { ind -> it.substring(ind, it.length) } }
                        file?.let { selectImage.mFile = it }
                        mSharedData.selectedImageList.add(selectImage)
                    }
                }


                val alert = AlertDialog.Builder(this@SendSelectedItemActivity)
                if(mSharedData.selectedImageList.size !== 0){
                    alert.setMessage("저장할 폴더명을 입력하세요")
                    val input = EditText(this@SendSelectedItemActivity)
                    alert.setView(input)
                    alert.setPositiveButton(
                        "확인"
                    ){
                        dialog, which ->
                        val dirName = input.text.toString()
                        mSharedData.isConnected = false
                        startSendService(dirName, serverIp)
                        finish()
                    }
                    alert.setNegativeButton("취소",
                        object : DialogInterface.OnClickListener{
                            override fun onClick(dialog: DialogInterface?, which: Int) {

                            }
                        })
                        alert.show()
                }else{
                    alert.setMessage("선택된 사진이 없습니다")
                    alert.setPositiveButton("확인", object : DialogInterface.OnClickListener{
                        override fun onClick(dialogInterface: DialogInterface, whichButto : Int){}
                    })
                    alert.show()
                }
            }
        } )
        SelectCancelButton.setOnClickListener ( object : View.OnClickListener{
            override fun onClick(v: View?) {
                finish()
            }
        } )
    }

    private fun findFileInfo(): Int {
        //select 컬럼
        val imageProjection =
            arrayOf(MediaStore.Images.Media.DATA, MediaStore.Images.Media.SIZE)
        val videoProjection =
            arrayOf(MediaStore.Video.Media.DATA, MediaStore.Video.Media.SIZE)

        // 쿼리 수행
        val ImageCursor = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageProjection, null, null, MediaStore.Images.Media.DATE_TAKEN + " desc ")

        val imageDataIndex = ImageCursor!!.getColumnIndex(MediaStore.Images.Media.DATA)

        if (ImageCursor != null) {
            ImageCursor.moveToFirst()
            while (true) {
                // 컬럼 인덱스
                val image = SelectedImageData()
                image.mData = ImageCursor.getString(imageDataIndex)
                image.mType = Constants.TYPE_IMAGE
                image.mCheckedState = false
                mAllImageList.add(image)
                if (!ImageCursor.moveToNext()) {
                    break
                }
            }
        }
        ImageCursor.close()

        val videoCursor = contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            videoProjection, null, null, MediaStore.Video.Media.DATE_TAKEN + " desc "
        )

        val videoDataIndex = videoCursor!!.getColumnIndex(MediaStore.Video.Media.DATA)

        if (videoCursor != null) {
            videoCursor.moveToFirst()
            while (true) {
                // 컬럼 인덱스
                val image = SelectedImageData()
                image.mData = videoCursor.getString(videoDataIndex)
                image.mCheckedState = false
                image.mType = Constants.TYPE_VIDEO
                mAllVideoList.add(image)
                if (!videoCursor.moveToNext()) {
                    break
                }
            }
        }
        videoCursor.close()
        return (mAllImageList.size + mAllVideoList.size)
    }

    private inner class  FindImageList  : AsyncTask<Int,Int,Int>(){
        @Suppress("DEPRECATION")
        override fun onPreExecute() {
            mProgressDialog = ProgressDialog(this@SendSelectedItemActivity)
            mProgressDialog.setMessage("사진 정보 준비중...")
            mProgressDialog.setCancelable(false)
            mProgressDialog.show()
        }

        override fun doInBackground(vararg params: Int?): Int {
            return findFileInfo()
        }

        override fun onPostExecute(result: Int?) {
            updateUi()
            mProgressDialog.dismiss()
        }

    }

    private fun updateUi(){
        mImageAdapter = ImageAdapter(this, R.layout.gridview_item, mAllImageList)
        GridImageList.adapter = mImageAdapter
        mVideoAdapter = ImageAdapter(this, R.layout.gridview_item, mAllVideoList)
        GridVideoList.adapter = mVideoAdapter
    }

    private fun startSendService(dirName: String, serverIp: String){
        mSharedData.selectedModeSenderIntent.putExtra(Constants.SERVER_IP, serverIp)
        mSharedData.selectedModeSenderIntent.putExtra(Constants.DIR_NAME, dirName)
        mSharedData.selectedModeSenderIntent.setClass(this, SendSelectedItemService::class.java)
        startService(mSharedData.selectedModeSenderIntent)

        mSharedData.selectedModeProgressServiceIntent.putExtra(Constants.MODE, Constants.MODE_SELECT) //선택모드
        mSharedData.selectedModeProgressServiceIntent.setClass(this, ProgressNotificationService::class.java)
        startService(mSharedData.selectedModeProgressServiceIntent)

        mProgressIntent = Intent()
        mProgressIntent.putExtra(Constants.MODE, Constants.MODE_SELECT) //선택모드
        mProgressIntent.setClass(this, ProgressActivity::class.java)
        startActivity(mProgressIntent)
    }

    // end main
}
