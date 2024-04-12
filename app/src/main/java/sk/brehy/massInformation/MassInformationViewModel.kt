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
import kotlinx.coroutines.launch
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
    var statusToast = ""

    //val status: StateFlow<String> = statusMutableFlow
    fun getAvailableMassInformation(context: Context) {
        val calendar: Calendar = Calendar.getInstance(Locale.ITALY)
        calendar.firstDayOfWeek = 1
        val date = "${calendar.get(Calendar.DAY_OF_MONTH)}-${calendar.get(Calendar.MONTH) + 1}-${
            calendar.get(Calendar.YEAR)
        }"
        val path = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val currentWeek = Pair(calendar.get(Calendar.WEEK_OF_YEAR), calendar.get(Calendar.YEAR))
        if (path != null) {
            saveCurrentWeek(path, "oznamy-${date}.jpg", context, currentWeek)
            deletePrevious(path, "oznamy-${date}.jpg", currentWeek)
        }
    }

    private fun saveCurrentWeek(
        directory: File,
        filename: String,
        context: Context,
        currentWeek: Pair<Int, Int>
    ) {
        val lastDownloadedInfo = directory.listFiles()!!.map { it.name }
        Log.d("fatal", lastDownloadedInfo.toString())
        val massInfoFile = File("${directory}/${filename}")
        Log.d("mass_info", "filename: ${filename}")
        if (!massInfoFile.exists()) {
            if (MainViewModel().isConnectedToInternet(context)) {
                Log.d("mass_info", "is connected")
                viewModelScope.async {
                    runDownload(context, directory.toString(), filename)
                }
            } else if (massInfoFromCurrentWeek(lastDownloadedInfo.ifEmpty { listOf("invalid") }
                    .first(), currentWeek)) {
                Log.d("mass_info", "in current week: ${lastDownloadedInfo.first()}")
                filePath.value = "${directory}/${lastDownloadedInfo.first()}"
            } else
                filePath.value = ""
        } else {
            Log.d("mass_info", "exists")
            filePath.value = "${directory}/${filename}"
        }
    }

    private fun massInfoFromCurrentWeek(
        lastDownloaded: String,
        currentWeek: Pair<Int, Int>
    ): Boolean {
        val date = lastDownloaded.dropLast(4).split("-").drop(1).map { it.toInt() }
        return if (date.size >= 3) {
            val calendar: Calendar = Calendar.getInstance(Locale.ITALY)
            calendar.firstDayOfWeek = 1
            calendar.set(date[2], date[1] - 1, date[0])
            Log.d(
                "mass_info",
                "${
                    Pair(
                        calendar.get(Calendar.WEEK_OF_YEAR),
                        calendar.get(Calendar.YEAR)
                    )
                } ${currentWeek}"
            )
            Pair(calendar.get(Calendar.WEEK_OF_YEAR), calendar.get(Calendar.YEAR)) == currentWeek
        } else false
    }

    private fun deletePrevious(directory: File, actual: String, currentWeek: Pair<Int, Int>) {
        for (file in directory.listFiles()!!) {
            if (file.name != actual && !massInfoFromCurrentWeek(file.name, currentWeek)) {
                Log.d("mass_delete", "file: ${file.name}")
                file.delete()
            }
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
            Log.d("mass_download", "doInBackground")
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
        Log.d("mass_download", "onPostExecute")
        status.value = "Sťahovanie nezačalo. Stlačte tlačídlo \"Stiahnúť\""
        Log.d("mass_download", "fileURL: ${fileURL}")
        if (fileURL.isNotEmpty()) {
            status.value = "Pracuje sa na sťahovaní."
            val file = File("${directory}/${filename}")
            val request = DownloadManager.Request(Uri.parse(fileURL))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationUri(Uri.fromFile(file))
                .setTitle(filename)
                .setDescription("Downloading")
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
            Log.d("mass_download", "request done")
            val downloadManager =
                context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            Log.d("mass_download", "downloadManager done")
            val downloadID = downloadManager.enqueue(request)
            Log.d("mass_download", "downloadID done")
            var finishDownload = false
            Log.d("mass_download", "failed: ${DownloadManager.STATUS_FAILED}")
            Log.d("mass_download", "paused: ${DownloadManager.STATUS_PAUSED}")
            Log.d("mass_download", "pending: ${DownloadManager.STATUS_PENDING}")
            Log.d("mass_download", "running: ${DownloadManager.STATUS_RUNNING}")
            Log.d("mass_download", "successful: ${DownloadManager.STATUS_SUCCESSFUL}")
            while (!finishDownload) {
                val cursor: Cursor =
                    downloadManager.query(DownloadManager.Query().setFilterById(downloadID))
                if (cursor.moveToFirst()) {
                    Log.d(
                        "mass_download",
                        "cursor first: ${cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))}"
                    )
                    when (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))) {
                        DownloadManager.STATUS_FAILED -> {
                            finishDownload = true
                            if (statusToast != "Sťahovanie neúspešné.") {
                                statusToast = "Sťahovanie neúspešné."
                                MassInformationFragment().showDownloadToast(statusToast)
                            }
                        }

                        DownloadManager.STATUS_PAUSED -> {
                            if (statusToast != "Sťahovanie pozastavené.") {
                                statusToast = "Sťahovanie pozastavené."
                                MassInformationFragment().showDownloadToast(statusToast)
                            }
                        }

                        DownloadManager.STATUS_PENDING -> {
                            if (statusToast != "Čaká sa na sťahovanie.") {
                                statusToast = "Čaká sa na sťahovanie."
                                MassInformationFragment().showDownloadToast(statusToast)
                            }
                        }

                        DownloadManager.STATUS_RUNNING -> {
                            //to publish progress
                            val total: Long =
                                cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                            if (total >= 0) {
                                //val downloaded: Long =
                                  //  cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                                if (statusToast != "Prebieha sťahovanie.") {
                                    statusToast = "Prebieha sťahovanie."
                                    MassInformationFragment().showDownloadToast(statusToast)
                                }
                                //statusToast =
                                  //  "Stiahnuté: ${floor((downloaded * 100.0) / total)}%"
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

}