package com.simplemobiletools.filemanager.activities

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.simplemobiletools.filemanager.R
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_image_view.*

/**
 * Created by slomka.jin on 2017/10/25.
 */
public fun Context.ViewImageIntent(path:String): Intent {
    return Intent(this, ImageViewActivity::class.java).apply {
        putExtra(INTENT_PATH, path)
    }
}
private const val INTENT_PATH = "path"
class ImageViewActivity: AppCompatActivity() {
    private lateinit var disposables: CompositeDisposable
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_view)
        val path=intent.getStringExtra(INTENT_PATH)
        disposables= CompositeDisposable()
        setImage(path)
    }

    fun setImage(path: String){
        val observable=Observable.just(path).doOnNext{showLoading()}.map { path->loadBitmap(path) }
        val disposable=observable.subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ bitmap->updateImage(bitmap) },{error->onError(error)})
        disposables.add(disposable)
    }

    fun loadBitmap(path:String):Bitmap{
       return BitmapFactory.decodeFile(path)
    }

    fun showLoading(){
        photoLoadBar.show()
    }
    fun updateImage(bitmap: Bitmap){
        photoLoadBar.hide()
        photoView.setImageBitmap(bitmap)
    }

    private fun onError(error: Throwable) {
        error.printStackTrace()
        photoLoadBar.hide()
        Log.d("loadimage", "error")

    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
    }
}