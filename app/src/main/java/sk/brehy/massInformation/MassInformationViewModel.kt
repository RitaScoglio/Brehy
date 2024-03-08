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
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import sk.brehy.MainViewModel
import java.io.File
import java.lang.Exception
import java.util.ArrayList
import java.util.Calendar
import java.util.Locale
import kotlin.math.floor

class MassInformationViewModel : ViewModel() {

    var status = MutableLiveData<String>()
    var filePath = MutableLiveData<String>()

    fun getAvailableMassInformation(context: Context) {
        val calendar: Calendar = Calendar.getInstance(Locale.ITALY)
        calendar.firstDayOfWeek = 1
        val date = "${calendar.get(Calendar.DAY_OF_MONTH)}-${calendar.get(Calendar.MONTH)+1}-${calendar.get(Calendar.YEAR)}"
        val path = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        if (path != null) {
            saveCurrentWeek(path,"oznamy-${date}.jpg", context, Pair(calendar.get(Calendar.WEEK_OF_YEAR), calendar.get(Calendar.YEAR)))
            deletePrevious(path, "oznamy-${date}.jpg")
        }
    }

    private fun saveCurrentWeek(directory: File, filename: String, context: Context, currentWeek: Pair<Int, Int>) {
        val lastDownloadedInfo = directory.listFiles()!!.map{ it.name }
        Log.d("fatal", lastDownloadedInfo.toString())
        val massInfoFile = File("${directory}/${filename}")
        if (!massInfoFile.exists()) {
            if (MainViewModel().isConnectedToInternet(context)) {
                status.value = "Sťahuje sa..."
                viewModelScope.async {
                    runDownload(context, directory.toString(), filename)
                    status.value = "K dispozícií."
                }
            } else if(compareDownloadedInfo(lastDownloadedInfo, currentWeek))
                filePath.value = "${directory}/${lastDownloadedInfo.first()}"
            else
                filePath.value = ""
        } else {
            filePath.value = "${directory}/${filename}"
        }
    }

    private fun compareDownloadedInfo(lastDownloaded: List<String>, currentWeek: Pair<Int, Int>): Boolean {
        return if(lastDownloaded.isNotEmpty()) {
            val date = lastDownloaded.first().split("-").drop(1).map { it.toInt() }
            val calendar: Calendar = Calendar.getInstance(Locale.ITALY)
            calendar.set(date[2], date[1] - 1, date[0])
            Pair(calendar.get(Calendar.WEEK_OF_YEAR), calendar.get(Calendar.YEAR)) == currentWeek
        } else false
    }

    private fun deletePrevious(directory: File, actual: String) {
        for (file in directory.listFiles()!!) {
            if (file.name != actual)
                file.delete()
        }
    }

    private suspend fun runDownload(context: Context, directory: String, filename: String) {
        checkAndRequestPermissions(context)
        val fileURL = doInBackground()
        onPostExecute(context, fileURL, directory, filename)
    }

    private fun checkAndRequestPermissions(context: Context): Boolean {
        val permissionAccessNetworkState = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_NETWORK_STATE
        )
        val permissionInternet = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.INTERNET
        )
        val permissionWriteExternalStorage = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        val permissionReadExternalStorage = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )

        val listPermissionsNeeded: MutableList<String> = ArrayList()
        if (permissionAccessNetworkState != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.ACCESS_NETWORK_STATE)
        }
        if (permissionWriteExternalStorage != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (permissionReadExternalStorage != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (permissionInternet != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.INTERNET)
        }
        if (listPermissionsNeeded.isNotEmpty()) {
            for (item in listPermissionsNeeded)
                ActivityCompat.requestPermissions(
                    context as Activity,
                    arrayOf(item), 1
                )
            return false
        }
        return true
    }

    private suspend fun doInBackground(): String =
        withContext(Dispatchers.IO) { // to run code in Background Thread
            try {
                val document: Document =
                    Jsoup.connect("https://farabrehy.sk/stranky/Farske-oznamy/Farske-oznamy.php")
                        .get()
                val element: Element = document.getElementsByClass("obr-oznamy").first()
                return@withContext element.attr("src")
            } catch (ignored: Exception) {
                return@withContext ""
            }
        }

    @SuppressLint("Range")
    private fun onPostExecute(
        context: Context,
        fileURL: String,
        directory: String,
        filename: String
    ) {
        val file = File("${directory}/${filename}")
        val request = DownloadManager.Request(Uri.parse(fileURL))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationUri(Uri.fromFile(file))
            .setTitle(filename)
            .setDescription("Downloading")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadID = downloadManager.enqueue(request)
        var finishDownload = false
        while (!finishDownload) {
            val cursor: Cursor =
                downloadManager.query(DownloadManager.Query().setFilterById(downloadID))
            if (cursor.moveToFirst()) {
                when (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))) {
                    DownloadManager.STATUS_FAILED -> {
                        finishDownload = true
                        status.value = "Sťahovanie neúspešné."
                    }

                    DownloadManager.STATUS_PAUSED -> {
                        status.value = "Sťahovanie pozastavené."
                    }

                    DownloadManager.STATUS_PENDING -> {
                        status.value = "Čaká sa na sťahovanie."
                    }

                    DownloadManager.STATUS_RUNNING -> {
                        //to publish progress
                        val total: Long =
                            cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                        if (total >= 0) {
                            val downloaded: Long =
                                cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
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