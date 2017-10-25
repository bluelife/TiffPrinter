package com.simplemobiletools.filemanager.fragments

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.support.v4.app.Fragment
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.simplemobiletools.commons.dialogs.StoragePickerDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.models.FileDirItem
import com.simplemobiletools.commons.views.Breadcrumbs
import com.simplemobiletools.commons.views.MyScalableRecyclerView
import com.simplemobiletools.filemanager.R
import com.simplemobiletools.filemanager.activities.*
import com.simplemobiletools.filemanager.adapters.ItemsAdapter
import com.simplemobiletools.filemanager.dialogs.CreateNewItemDialog
import com.simplemobiletools.filemanager.extensions.config
import com.simplemobiletools.filemanager.extensions.isPathOnRoot
import com.simplemobiletools.filemanager.helpers.RootHelpers
import com.stericson.RootTools.RootTools
import kotlinx.android.synthetic.main.items_fragment.*
import kotlinx.android.synthetic.main.items_fragment.view.*
import java.io.File
import java.util.HashMap
import kotlin.collections.ArrayList

class ItemsFragment : Fragment(), ItemsAdapter.ItemOperationsListener, Breadcrumbs.BreadcrumbsListener {
    var currentPath = ""
    var isGetContentIntent = false
    var isPickMultipleIntent = false

    private var storedTextColor = 0
    private var showHidden = false
    private var storedItems = ArrayList<FileDirItem>()
    private var scrollStates = HashMap<String, Parcelable>()

    private lateinit var mView: View

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        mView = inflater!!.inflate(R.layout.items_fragment, container, false)!!
        storeConfigVariables()
        return mView
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mView.apply {
            items_swipe_refresh.setOnRefreshListener({ refreshItems() })

            breadcrumbs.listener = this@ItemsFragment
        }
    }

    override fun onResume() {
        super.onResume()
        context.updateTextColors(mView as ViewGroup)
        mView.items_fastscroller.updateHandleColor()
        val newColor = context.config.textColor
        if (storedTextColor != newColor) {
            storedItems = ArrayList()
            (items_list.adapter as ItemsAdapter).updateTextColor(newColor)
            mView.breadcrumbs.updateColor(newColor)
            storedTextColor = newColor
        }

        refreshItems()
    }

    override fun onPause() {
        super.onPause()
        storeConfigVariables()
    }

    private fun storeConfigVariables() {
        storedTextColor = context.config.textColor
    }

    fun openPath(path: String) {
        var realPath = path.trimEnd('/')
        if (realPath.isEmpty())
            realPath = "/"

        scrollStates.put(currentPath, getScrollState())
        currentPath = realPath
        showHidden = context.config.shouldShowHidden
        getItems(currentPath) {
            if (!isAdded)
                return@getItems

            FileDirItem.sorting = context.config.getFolderSorting(currentPath)
            it.sort()
            activity.runOnUiThread {
                addItems(it)
            }
        }
    }

    private fun addItems(items: ArrayList<FileDirItem>) {
        mView.apply {
            activity?.runOnUiThread {
                items_swipe_refresh?.isRefreshing = false
                if (items.hashCode() == storedItems.hashCode()) {
                    return@runOnUiThread
                }

                mView.breadcrumbs.setBreadcrumb(currentPath)
                storedItems = items
                val currAdapter = items_list.adapter
                if (currAdapter == null) {
                    items_list.apply {
                        this.adapter = ItemsAdapter(activity as SimpleActivity, storedItems, this@ItemsFragment, isPickMultipleIntent) {
                            itemClicked(it)
                        }

                        DividerItemDecoration(context, DividerItemDecoration.VERTICAL).apply {
                            setDrawable(context.resources.getDrawable(com.simplemobiletools.commons.R.drawable.divider))
                            addItemDecoration(this)
                        }

                        isDragSelectionEnabled = true
                    }
                    items_fastscroller.setViews(items_list, items_swipe_refresh)
                    setupRecyclerViewListener()
                } else {
                    (currAdapter as ItemsAdapter).updateItems(storedItems)

                    val savedState = scrollStates[currentPath]
                    if (savedState != null) {
                        getRecyclerLayoutManager().onRestoreInstanceState(savedState)
                    } else {
                        getRecyclerLayoutManager().scrollToPosition(0)
                    }
                }
            }
        }
    }

    private fun setupRecyclerViewListener() {
        mView.items_list?.listener = object : MyScalableRecyclerView.MyScalableRecyclerViewListener {
            override fun zoomIn() {

            }

            override fun zoomOut() {

            }

            override fun selectItem(position: Int) {
                getRecyclerAdapter().selectItem(position)
            }

            override fun selectRange(initialSelection: Int, lastDraggedIndex: Int, minReached: Int, maxReached: Int) {
                getRecyclerAdapter().selectRange(initialSelection, lastDraggedIndex, minReached, maxReached)
            }
        }
    }

    private fun getRecyclerAdapter() = (items_list.adapter as ItemsAdapter)

    fun getScrollState() = getRecyclerLayoutManager().onSaveInstanceState()

    private fun getRecyclerLayoutManager() = (mView.items_list.layoutManager as LinearLayoutManager)

    private fun getItems(path: String, callback: (items: ArrayList<FileDirItem>) -> Unit) {
        Thread({
            if (!context.config.enableRootAccess || !context.isPathOnRoot(path)) {
                getRegularItemsOf(path, callback)
            } else {
                RootHelpers().getFiles(activity as SimpleActivity, path, callback)
            }
        }).start()
    }

    private fun validFormat(fileName:String):Boolean{
        return fileName.endsWith(".pdf")||fileName.endsWith(".png")
                ||fileName.endsWith(".jpg")||fileName.endsWith(".bmp")||fileName.endsWith(".tif")
    }
    private fun getRegularItemsOf(path: String, callback: (items: ArrayList<FileDirItem>) -> Unit) {
        val items = ArrayList<FileDirItem>()
        val files = File(path).listFiles()
        if (files != null) {
            for (file in files) {
                val curPath = file.absolutePath
                val curName = curPath.getFilenameFromPath()
                if (!showHidden && curName.startsWith("."))
                    continue
                if(!file.isDirectory&&!validFormat(curName))
                    continue
                val children = getChildren(file)
                val size = file.length()

                items.add(FileDirItem(curPath, curName, file.isDirectory, children, size))
            }
        }
        callback(items)
    }

    private fun getChildren(file: File): Int {
        val fileList: Array<out File>? = file.listFiles() ?: return 0

        if (file.isDirectory) {
            return if (showHidden) {
                fileList!!.count { file -> file.isDirectory||(validFormat(file.name)) }
            } else {
                fileList!!.count { file->file.name[0]!='.'&&(file.isDirectory||validFormat(file.name)) }
            }
        }
        return 0
    }

    private fun itemClicked(item: FileDirItem) {
        if (item.isDirectory) {
            openPath(item.path)
        } else {
            val path = item.path
            when{
                path.endsWith(".pdf")->startActivity(activity.ViewPdfIntent(path))
                path.endsWith(".png")||path.endsWith(".jpg")||path.endsWith(".bmp")->startActivity(activity.ViewImageIntent(path))
                path.endsWith(".tif")->startActivity(activity.ViewTifIntent(path))
                else-> print("no format matched")
            }


        }
    }


    private fun fileClicked(path: String) {
        val file = File(path)
        val mimeType = path.getMimeTypeFromPath()
        Log.d("path",path);
        /*Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.fromFile(file), mimeType)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            try {
                startActivity(this)
            } catch (e: ActivityNotFoundException) {
                if (!tryGenericMimeType(this, mimeType, file)) {
                    activity.toast(R.string.no_app_found)
                }
            }
        }*/
    }
    override fun itemLongClicked(item: FileDirItem) {
        //items_list.setDragSelectActive(position)
        if (!item.isDirectory) {
            Log.d("clicklong",item.path);
        }
    }

    private fun tryGenericMimeType(intent: Intent, mimeType: String, file: File): Boolean {
        val genericMimeType = getGenericMimeType(mimeType)
        intent.setDataAndType(Uri.fromFile(file), genericMimeType)
        return try {
            startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }

    private fun createNewItem() {
        CreateNewItemDialog(activity as SimpleActivity, currentPath) {
            if (it) {
                refreshItems()
            }
        }
    }

    private fun getGenericMimeType(mimeType: String): String {
        if (!mimeType.contains("/"))
            return mimeType

        val type = mimeType.substring(0, mimeType.indexOf("/"))
        return "$type/*"
    }

    override fun breadcrumbClicked(id: Int) {
        if (id == 0) {
            StoragePickerDialog(activity, currentPath) {
                openPath(it)
            }
        } else {
            val item = breadcrumbs.getChildAt(id).tag as FileDirItem
            openPath(item.path)
        }
    }

    override fun refreshItems() {
        openPath(currentPath)
    }

    override fun deleteFiles(files: ArrayList<File>) {
        val hasFolder = files.any { it.isDirectory }
        if (context.isPathOnRoot(files.firstOrNull()?.absolutePath ?: context.config.internalStoragePath)) {
            files.forEach {
                RootTools.deleteFileOrDirectory(it.path, false)
            }
        } else {
            (activity as SimpleActivity).deleteFiles(files, hasFolder) {
                if (!it) {
                    activity.runOnUiThread {
                        activity.toast(R.string.unknown_error_occurred)
                    }
                }
            }
        }
    }


    override fun selectedPaths(paths: ArrayList<String>) {
        (activity as MainActivity).pickedPaths(paths)
    }
}
