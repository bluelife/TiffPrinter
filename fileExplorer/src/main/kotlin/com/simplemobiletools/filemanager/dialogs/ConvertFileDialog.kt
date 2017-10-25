package com.simplemobiletools.filemanager.dialogs


import android.os.Environment
import android.support.v7.app.AlertDialog
import android.view.View
import android.view.WindowManager
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.filemanager.R
import com.simplemobiletools.filemanager.activities.SimpleActivity
import kotlinx.android.synthetic.main.dialog_create_new.view.*
import java.io.File
import java.io.IOException

import android.os.ParcelFileDescriptor.MODE_READ_ONLY
import android.os.Environment.getExternalStorageDirectory
import android.os.ParcelFileDescriptor
import android.util.Log
import com.shockwave.pdfium.PdfDocument
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.disposables.Disposable
import com.shockwave.pdfium.PdfiumCore
import com.simplemobiletools.filemanager.task.Convert2TiffTask
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import java.util.concurrent.TimeUnit
import kotlinx.android.synthetic.main.dialog_convert.view.*
import org.beyka.tiffbitmapfactory.TiffConverter
import org.beyka.tiffbitmapfactory.CompressionScheme






/**
 * Created by slomka.jin on 2017/10/25.
 */
class ConvertFileDialog(val activity: SimpleActivity, val path: String, val callback: (success: Boolean) -> Unit) {
    private val view = activity.layoutInflater.inflate(R.layout.dialog_convert, null)
    private lateinit var pdfiumCore: PdfiumCore
    private lateinit var pdfDocument: PdfDocument
    private var dialog:AlertDialog;
    private val startTime: Long = 0
    private lateinit var disposables: CompositeDisposable
    var count = 0
    var total=0;
    init {
        dialog=AlertDialog.Builder(activity)
                /*.setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)*/
                .create().apply {
            activity.setupDialogStuff(view, this, R.string.title_convert_file)
            setCancelable(false)
            disposables= CompositeDisposable();
            if(path.endsWith(".pdf")){
                startTask()
            }
            else{
                startImageTask()
            }

        }
    }

    fun startTask() {
        view.apply { animation_view.playAnimation() }
        val observable = Observable.fromIterable(getTasks()).map({ task -> task.run() })
        val disposable = observable.subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ done -> onTaskDone(done) }, { error -> onError(error) }, { onComplete() })
        disposables.add(disposable)
        Log.d("oow", "start")
    }
    fun startImageTask(){
        val observable=Observable.just(path).map { path->convertImage() }
        val disposable=observable.subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ done->updateProgress("done") },{error->onError(error)}, { onImageComplete() })
        disposables.add(disposable)
    }
    fun convertImage(){
        val options = TiffConverter.ConverterOptions()
        options.throwExceptions = false //Set to true if you want use java exception mechanism;
        options.availableMemory = (128 * 1024 * 1024).toLong() //Available 128Mb for work;
        options.compressionScheme = CompressionScheme.CCITTFAX3 //compression scheme for tiff
        options.appendTiff = false//If set to true - will be created one more tiff directory, otherwise file will be overwritten
        TiffConverter.convertToTiff(path, Environment.getExternalStorageDirectory().getAbsolutePath() + "/Download/resultimage.tif"
                , options, {percent,totals->
                Log.d("ss", "ss $percent $totals")
                val content= "${percent*100f / totals}% proceed!"
                activity.runOnUiThread { updateProgress(content) }

        })
    }
    private fun getTasks(): List<Convert2TiffTask> {
        val tasks = mutableListOf<Convert2TiffTask>()
        try {
            val pdfPath = path;
            val targetPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Download/result.tif"
            val pdfFile = File(pdfPath)
            val tifFile = File(targetPath)
            pdfiumCore = PdfiumCore(this.activity)
            if (tifFile.exists())
                tifFile.delete()
            val fileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfDocument = pdfiumCore.newDocument(fileDescriptor)


            total = pdfiumCore.getPageCount(pdfDocument)
            for (i in 0 until total) {
                tasks.add(Convert2TiffTask(pdfiumCore, pdfDocument, i, total, targetPath))
            }
            Log.d("oo", "$total")
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        return tasks
    }

    private fun onTaskDone(done: Boolean) {
        Log.d("work", done.toString() + "")
        count++
        updateProgress("progress:$count,total $total")
    }
    private fun updateProgress(progress:String){
        view.apply {  convert_tip.setText(progress)}
    }
    private fun onError(error: Throwable) {
        error.printStackTrace()
        Log.d("work", "error")
        closePdf()
        removeDialog()
    }

    private fun onComplete() {
        Log.d("work", "complete")
        view.apply {  animation_view.cancelAnimation()}
        val elapseTime = System.nanoTime() - startTime
        Log.d("converttime", "${TimeUnit.NANOSECONDS.toMillis(elapseTime)}")
        view.apply { convert_tip.setText("work completed! elapse time is " + TimeUnit.NANOSECONDS.toMillis(elapseTime) + "ms")}
        closePdf()
        removeDialog()
    }
    private fun onImageComplete(){
        val elapseTime = System.nanoTime() - startTime
        Log.d("work", "complete")
        Log.d("time", "${TimeUnit.NANOSECONDS.toMillis(elapseTime)}")
        view.apply {  animation_view.cancelAnimation()}
        removeDialog()
    }

    private fun closePdf() {
        pdfiumCore.closeDocument(pdfDocument)
    }

    private fun removeDialog() {
        dialog.dismiss()
        disposables.clear()
        callback(true)
    }
}
