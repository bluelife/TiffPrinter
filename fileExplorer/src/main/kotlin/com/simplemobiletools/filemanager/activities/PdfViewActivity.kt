package com.simplemobiletools.filemanager.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.support.v4.view.ViewPager
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener
import com.simplemobiletools.filemanager.R
import java.io.File

/**
 * Created by slomka.jin on 2017/10/25.
 */

public fun Context.ViewPdfIntent(path:String): Intent {
    return Intent(this, PdfViewActivity::class.java).apply {
        putExtra(INTENT_PATH, path)
    }
}
private const val INTENT_PATH = "path"
class PdfViewActivity : Activity(), OnPageChangeListener {
    private lateinit var pdfView: PDFView
    private var pageText: TextView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf)
        pdfView = findViewById(R.id.pdfView) as PDFView
        pageText = findViewById(R.id.res_pdf_page) as TextView
        val bundle = intent.extras
        val titleView = findViewById(R.id.res_pdf_title) as TextView
        val closeBtn = findViewById(R.id.res_pdf_close_btn) as ImageButton
        closeBtn.setOnClickListener { finish() }
        if (bundle != null) {
            val file = bundle.getString(INTENT_PATH)
            val title = "pdf"
            val pdf = File(file)
            titleView.text = title
            pdfView.setMidZoom(2f)
            pdfView.fromFile(pdf)
                    .enableSwipe(true) // allows to block changing pages using swipe
                    .swipeHorizontal(false)
                    .enableDoubletap(true)
                    .defaultPage(0)
                    .onPageChange(this)
                    .enableAnnotationRendering(false) // render annotations (such as comments, colors or forms)
                    .password(null)
                    .scrollHandle(null)
                    .enableAntialiasing(true) // improve rendering a little bit on low-res screens
                    .load()
        }
    }

    override fun onPageChanged(page: Int, pageCount: Int) {
        pageText!!.text = String.format(getString(R.string.pdf_page_count), page + 1, pageCount)
    }

}