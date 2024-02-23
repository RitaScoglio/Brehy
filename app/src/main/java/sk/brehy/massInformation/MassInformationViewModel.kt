package sk.brehy.massInformation

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import sk.brehy.DatabaseMainViewModel
import java.io.File
import java.lang.Exception
import java.util.ArrayList
import java.util.Calendar
import java.util.Locale
import kotlin.math.floor

class MassInformationViewModel : DatabaseMainViewModel() {

    var status = MutableLiveData<String>()
    var filePath = MutableLiveData<String>()

    fun getAvailableMassInformation(context: Context) {
        val calendar : Calendar = Calendar.getInstance(Locale.ITALY)
        calendar.firstDayOfWeek = 1
        val currentWeek = calendar.get(Calendar.WEEK_OF_YEAR)
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val previousWeek = calendar.get(Calendar.WEEK_OF_YEAR)
        val path = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        saveCurrentWeek(path, "oznamy-${currentWeek}.jpg", context)
        deletePreviousWeek("${path}/oznamy-${previousWeek}.jpg")
    }

    private fun saveCurrentWeek(path: File?, filename: String, context: Context) {
        val massInfoFile = File("${path}/${filename}")
        if(!massInfoFile.exists()){
            if(isConnectedToInternet(context)) {
                status.value = "Sťahuje sa..."
                viewModelScope.async {
                    runDownload(context, path.toString(), filename)
                    status.value = "K dispozícií."
                }
            } else
                filePath.value = ""
        } else {
            filePath.value = "${path}/${filename}"
        }
    }

    private fun deletePreviousWeek(path: String) {
        val massInfoFilePrevious = File(path)
        if(massInfoFilePrevious.exists()) {
            massInfoFilePrevious.delete()
        }
    }

    suspend fun runDownload(context: Context, directory: String, filename: String) {
        checkAndRequestPermissions(context)
        val fileURL = doInBackground()
        onPostExecute(context, fileURL, directory, filename)
    }

    private fun checkAndRequestPermissions(context : Context): Boolean {
        val permissionACCESS_NETWORK_STATE = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_NETWORK_STATE
        )
        val permissionINTERNET = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.INTERNET
        )
        val permissionWRITE_EXTERNAL_STORAGE = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        val permissionREAD_EXTERNAL_STORAGE = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )

        val listPermissionsNeeded: MutableList<String> = ArrayList()
        if (permissionACCESS_NETWORK_STATE != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.ACCESS_NETWORK_STATE)
        }
        if (permissionWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (permissionREAD_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (permissionINTERNET != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.INTERNET)
        }
        if (!listPermissionsNeeded.isEmpty()) {
            for (item in listPermissionsNeeded)
                ActivityCompat.requestPermissions(
                    context as Activity,
                    arrayOf(item), 1)
            return false
        }
        return true
    }

    private suspend fun doInBackground(): String = withContext(Dispatchers.IO) { // to run code in Background Thread
        try {
            val document: Document =
                Jsoup.connect("https://farabrehy.sk/stranky/Farske-oznamy/Farske-oznamy.php").get()
            val element: Element = document.getElementsByClass("obr-oznamy").first()
            return@withContext element.attr("src")
        } catch (ignored: Exception) {
            return@withContext ""
        }
    }

    @SuppressLint("Range")
    private fun onPostExecute(context: Context, fileURL: String, directory: String, filename: String) {

        val file = File("${directory}/${filename}")
        val request = DownloadManager.Request(Uri.parse(fileURL))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
            .setDestinationUri(Uri.fromFile(file))
            .setTitle(filename)
            .setDescription("Downloading")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadID =
            downloadManager.enqueue(request)
        var finishDownload = false
        while (!finishDownload) {
            val cursor: Cursor =
                downloadManager.query(DownloadManager.Query().setFilterById(downloadID))
            if (cursor.moveToFirst()) {
                when (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))) {
                    DownloadManager.STATUS_FAILED -> {
                        finishDownload = true
                    }
                    DownloadManager.STATUS_PAUSED -> {
                    }
                    DownloadManager.STATUS_PENDING -> {
                    }
                    DownloadManager.STATUS_RUNNING -> {
                        //to publish progress
                        val total: Long =
                            cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                        if (total >= 0) {
                            val downloaded: Long = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                            status.value = "Stiahnuté: ${floor((downloaded * 100.0) / total)}%"
                        }
                    }
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        finishDownload = true
                        filePath.value = file.path
                    }
                }
            }
        }
    }
}