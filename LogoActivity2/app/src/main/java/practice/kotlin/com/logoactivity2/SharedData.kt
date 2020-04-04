package practice.kotlin.com.logoactivity2

import android.content.Intent
import practice.kotlin.com.logoactivity2.mode.selected.SelectedImageData

class SharedData {
    val allModeSenderIntent = Intent()
    val allModeProgressServiceIntent = Intent()
    var isConnected: Boolean = false
    var threadCount: Int = 0

    val selectedModeSenderIntent = Intent()
    val selectedModeProgressServiceIntent = Intent()
    var allModeFileCount: Int = 0
    var allModeTotalFileCount: Int = 0

    var selectedModeFileCount: Int = 0
    var selectedModeTotalFileCount: Int = 0
    
    
    var selectedImageList = ArrayList<SelectedImageData>()

    companion object {
        val instance = SharedData()
    }
}