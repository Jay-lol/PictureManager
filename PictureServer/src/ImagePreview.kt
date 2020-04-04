import java.awt.GraphicsEnvironment
import java.awt.Point
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO
import javax.swing.ImageIcon
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JScrollPane

class ImagePreview : JFrame("미리 보기") {
    private val mImageIcon: ImageIcon
    private var mImage: BufferedImage? = null
    private val mImageLabel: JLabel

    init {
        title = "Simple review your Stories"
        val center = GraphicsEnvironment.getLocalGraphicsEnvironment().centerPoint
        location = Point(center.x - 400, center.y - 300)
        mImageIcon = ImageIcon()
        mImageLabel = JLabel(mImageIcon)
        val panel = JScrollPane()
        panel.setViewportView(mImageLabel)
        contentPane.add(panel)

        setSize(800, 600)
        isVisible = true
    }

    private fun imageResizer(img: BufferedImage?, width: Int, height: Int): BufferedImage {
        val resizedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g2 = resizedImage.createGraphics()
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        g2.drawImage(img, 0, 0, width, height, null)
        g2.dispose()
        return resizedImage
    }

    fun setImage(path: String) {
        try {
            mImage = ImageIO.read(File(path))

            contentPane.removeAll()
            mImageIcon.image = imageResizer(mImage, mImageLabel.width, mImageLabel.height)
            mImageLabel.icon = mImageIcon
            val panel = JScrollPane(mImageLabel)
            contentPane.add(panel)
            contentPane.revalidate()
        } catch (e: IOException) {
        }

    }

    fun closeWindow() {
        isVisible = false
    }
}