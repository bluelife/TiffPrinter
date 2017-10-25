package com.simplemobiletools.filemanager.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity

/**
 * Created by slomka.jin on 2017/10/25.
 */
fun Context.ViewTifIntent(path:String): Intent {
    return Intent(this, TifViewActivity::class.java).apply {
        putExtra(INTENT_PATH, path)
    }
}
private const val INTENT_PATH = "path"
class TifViewActivity:AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }
}