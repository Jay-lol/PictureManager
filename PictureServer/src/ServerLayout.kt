import java.awt.Dimension
import java.awt.Font
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.IOException
import java.net.NetworkInterface
import java.net.UnknownHostException
import java.util.Locale
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.JTabbedPane
import javax.swing.SwingConstants
import javax.swing.SwingWorker

class ServerLayout : JFrame() {
    val mFilePath: JLabel
    val mFileName: JLabel
    private val mPcName: JLabel
    private val mIpInfo: JLabel
    val mConnectInfo: JLabel
    val mProgressBar: JProgressBar
    val mRemainFiles: JLabel
    val mLastDate: JLabel

    init {
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                isVisible = false
                dispose()
                System.exit(0)
            }
        })
        title = "Picture Server"
        isVisible = true
        size = Dimension(655, 365)
        layout = null
        defaultCloseOperation = EXIT_ON_CLOSE

        val infoTab = JTabbedPane(SwingConstants.TOP)
        infoTab.font = Font(null, Font.PLAIN, 14)
        infoTab.setBounds(0, 0, 635, 323) // (x,y,w,h)
        add(infoTab)

        val sendInformation = JPanel()
        infoTab.addTab("전송 정보", null, sendInformation, null)
        sendInformation.locale = Locale.KOREA
        sendInformation.layout = null

        val connectionState = JLabel("연결 상태 :")
        connectionState.font = Font(null, Font.PLAIN, 14)
        connectionState.setBounds(12, 10, 79, 34)
        sendInformation.add(connectionState)

        mConnectInfo = JLabel("연결 대기중")
        mConnectInfo.font = Font(null, Font.PLAIN, 14)
        mConnectInfo.setBounds(102, 10, 381, 34)
        sendInformation.add(mConnectInfo)

        val noName = JLabel("파일 경로 :")
        noName.font = Font(null, Font.PLAIN, 14)
        noName.setBounds(12, 54, 79, 51)
        sendInformation.add(noName)

        mFilePath = JLabel("")
        mFilePath.font = Font(null, Font.PLAIN, 13)
        mFilePath.setBounds(102, 54, 381, 51)
        sendInformation.add(mFilePath)

        val lastDateLabel = JLabel("마지막 동기화 날짜 : ")
        lastDateLabel.font = Font(null, Font.PLAIN, 14)
        lastDateLabel.setBounds(12, 127, 144, 30)
        sendInformation.add(lastDateLabel)

        mLastDate = JLabel("")
        mLastDate.font = Font(null, Font.BOLD, 13)
        mLastDate.setBounds(168, 127, 315, 30)
        sendInformation.add(mLastDate)

        val fileName = JLabel("파일명 :")
        fileName.font = Font(null, Font.PLAIN, 14)
        fileName.setBounds(12, 181, 57, 30)
        sendInformation.add(fileName)

        this.mFileName = JLabel("")
        this.mFileName.font = Font(null, Font.PLAIN, 13)
        this.mFileName.setBounds(81, 181, 402, 30)
        sendInformation.add(this.mFileName)

        mProgressBar = JProgressBar()
        mProgressBar.setBounds(12, 230, 471, 45)
        sendInformation.add(mProgressBar)

        val openButton = JButton("저장위치 열기")
        openButton.font = Font(null, Font.PLAIN, 13)
        openButton.addActionListener {
            try {
                val runtime = Runtime.getRuntime()
                runtime.exec("cmd.exe /c explorer " + mFilePath.text)
            } catch (e1: IOException) {
                // TODO Auto-generated catch block
                e1.printStackTrace()
            }
        }
        openButton.setBounds(495, 55, 123, 51)
        sendInformation.add(openButton)

        mRemainFiles = JLabel("")
        mRemainFiles.setBounds(495, 181, 123, 30)
        sendInformation.add(mRemainFiles)

        val pcInformation = JPanel()
        infoTab.addTab("PC 정보", null, pcInformation, null)
        pcInformation.layout = null

        val pcName = JLabel("PC 이름 : ")
        pcName.font = Font(null, Font.PLAIN, 13)
        pcName.setBounds(12, 36, 160, 30)
        pcInformation.add(pcName)

        mPcName = JLabel("")
        mPcName.font = Font(null, Font.PLAIN, 13)
        pcInformation.add(mPcName)
        mPcName.setBounds(81, 36, 229, 30)

        val ipInfo = JLabel("IP :")
        ipInfo.font = Font(null, Font.PLAIN, 13)
        ipInfo.setBounds(12, 93, 60, 30)
        pcInformation.add(ipInfo)

        mIpInfo = JLabel("")
        mIpInfo.font = Font(null, Font.PLAIN, 13)
        mIpInfo.setBounds(81, 93, 229, 30)
        pcInformation.add(mIpInfo)
    }

    internal fun setPCName() {
        try {
            val localMachine = java.net.InetAddress.getLocalHost()
            mPcName.text = localMachine.hostName
            mIpInfo.text = localMachine.hostAddress
        } catch (e: UnknownHostException) {
            e.printStackTrace()
        }
    }


    internal fun setIP() {
        try {
            val en = NetworkInterface.getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val networkIf = en.nextElement()
                val ipAddress = networkIf.inetAddresses
                while (ipAddress.hasMoreElements()) {
                    val inetAddress = ipAddress.nextElement()
                    if (!inetAddress.isLoopbackAddress && !inetAddress.isLinkLocalAddress && inetAddress.isSiteLocalAddress) {
                        mIpInfo.text = inetAddress.hostAddress.toString()
                    }
                }
            }
        } catch (ex: Exception) {
        }

    }

    inner class ImageMirroring : SwingWorker<Int, Int>() {
        var mPath: String? = null
        private var mIsRunning = true
        private var mImagePreview: ImagePreview? = null

        @Throws(Exception::class)
        override fun doInBackground(): Int? {
            var currentPath = "0"
            while (mPath == null) {
                Thread.sleep(500)
            }
            mImagePreview = ImagePreview() // show image mirroring
            while (mIsRunning) {
                if (currentPath !== mPath) {
                    currentPath = mPath as String
                    mImagePreview?.setImage(currentPath)
                }
                Thread.sleep(1500)
            }
            return null
        }

        fun isImagePreview(): Boolean {
            return mImagePreview != null
        }

        fun stopRunning() {
            mIsRunning = false
            mImagePreview?.closeWindow()
        }

        override fun process(chunks: List<Int>?) {
            stopRunning()
        }

    }
}