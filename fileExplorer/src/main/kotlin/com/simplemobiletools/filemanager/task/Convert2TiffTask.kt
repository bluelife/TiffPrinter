package com.simplemobiletools.filemanager.task

/**
 * Created by slomka.jin on 2017/10/25.
 */
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import android.os.SystemClock
import android.util.Log

import com.shockwave.pdfium.PdfDocument
import com.shockwave.pdfium.PdfiumCore

import org.beyka.tiffbitmapfactory.TiffSaver

import java.io.File
import java.io.FileOutputStream
import org.beyka.tiffbitmapfactory.CompressionScheme
import org.beyka.tiffbitmapfactory.Orientation
import org.beyka.tiffbitmapfactory.TiffConverter


/**
 * Created by HiWin10 on 10/19/2017.
 */

class Convert2TiffTask(private val pdfiumCore: PdfiumCore, private val document: PdfDocument, private val index: Int, private val total: Int, private val path: String) {

    fun run(): Boolean {
        var done = false
        try {


            pdfiumCore.openPage(document, index)

            val width = pdfiumCore.getPageWidthPoint(document, index) * 1
            val height = pdfiumCore.getPageHeightPoint(document, index) * 1

            val bitmap = Bitmap.createBitmap(width, height,
                    Bitmap.Config.RGB_565)

            pdfiumCore.renderPageBitmap(document, bitmap, index, 0, 0,
                    width, height)
            Log.d("imagesize:", bitmap.toString() + " " + width)
            //SaveImage(bitmap, index)
            val options = TiffSaver.SaveOptions()
            options.compressionScheme = CompressionScheme.CCITTFAX3
//By default orientation is top left
            options.orientation = Orientation.TOP_LEFT
            Log.d("rr2", bitmap.toString() + " " + width)
            done = TiffSaver.appendBitmap(path, bitmap, options)

            Log.d("ooooo", "done")
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        return done
    }

    private fun SaveImage(finalBitmap: Bitmap, index: Int) {

        val root = Environment.getExternalStorageDirectory().absolutePath
        val myDir = File(root + "/saved_images")
        myDir.mkdirs()

        val fname = "Image.png"
        val file = File(myDir, fname)
        if (file.exists()) file.delete()
        try {
            val out = FileOutputStream(file)
            finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
            out.close()

        } catch (e: Exception) {
            e.printStackTrace()
        }

    }
}