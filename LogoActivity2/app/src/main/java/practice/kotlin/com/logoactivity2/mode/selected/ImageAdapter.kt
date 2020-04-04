package practice.kotlin.com.logoactivity2.mode.selected

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.ImageView
import practice.kotlin.com.logoactivity2.Constants
import practice.kotlin.com.logoactivity2.R

class ImageAdapter(private val mContext: Context, private val mCellLayout: Int,
                   private val mImageList: ArrayList<SelectedImageData>) : BaseAdapter() {
    private val mLayoutInflater = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val mCache = Cache<String, Bitmap>()

    private inner class ImageViewHolder {
        var mIvImage: ImageView? = null
        var mIvCheckBox: CheckBox? = null
    }

    override fun getCount(): Int {
        return mImageList.size
    }

    override fun getItem(position: Int): Any {
        return mImageList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var convertView = convertView
        if (convertView == null) {
            convertView = mLayoutInflater.inflate(mCellLayout, parent, false)
            val holder = ImageViewHolder()

            holder.mIvImage = convertView.findViewById(R.id.thumbnail_imageView) as ImageView
            holder.mIvCheckBox = convertView.findViewById(R.id.thumbnail_checkBox) as CheckBox

            convertView.tag = holder
        }

        val holder = convertView?.tag as ImageViewHolder
        holder.mIvCheckBox?.isChecked = mImageList[position].mCheckedState

        try {
            val path = mImageList[position].mData
            var bmp: Bitmap? = path?.let { mCache.get(it) }
            if (bmp != null) {
                holder.mIvImage?.setImageBitmap(bmp)
            } else {
                if (mImageList[position].mType === Constants.TYPE_IMAGE) {
                    bmp = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(path), 100, 100)
                } else if (mImageList[position].mType === Constants.TYPE_VIDEO) {
                    bmp = ThumbnailUtils.createVideoThumbnail(path, MediaStore.Images.Thumbnails.MINI_KIND)
                }
                bmp?.let {
                    holder.mIvImage?.setImageBitmap(it)
                    path?.let { path -> mCache.put(path, it) }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return convertView
    }
}