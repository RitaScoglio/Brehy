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
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import sk.brehy.MainViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.time.Duration.Companion.hours


class MassInformationViewModel : ViewModel() {

    var status = MutableLiveData<String>()
    var filePath = MutableLiveData<String>()
    var statusToast = ""

    fun getAvailableMassInformation(context: Context) {
        val calendar: Calendar = Calendar.getInstance(Locale.ITALY)
        calendar.firstDayOfWeek = 1
        val path = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val currentMillis = calendar.timeInMillis
        if (path != null) {
            saveCurrentWeek(path, context, currentMillis)
        }
    }

    private fun saveCurrentWeek(
        directory: File,
        context: Context,
        currentMillis: Long
    ) {
        val lastDownloadedInfo = directory.listFiles()!!.map { it.name }
        Log.d("fatal", lastDownloadedInfo.toString())
        Log.d("mass_info", "filename: ${currentMillis}")
        if (!downloadIn12Hours(
                lastDownloadedInfo.ifEmpty { listOf("invalid") }.first(),
                currentMillis
            )
        ) {
            if (MainViewModel().isConnectedToInternet(context)) {
                Log.d("mass_info", "is connected")
                //Toast.makeText(context, "before async", Toast.LENGTH_SHORT).show()
                viewModelScope.async {
                    runDownload(context, directory.toString(), currentMillis.toString())
                    deletePrevious(directory, currentMillis)
                }
            } else if (massInfoFromCurrentWeek(lastDownloadedInfo.ifEmpty { listOf("invalid") }
                    .first())) {
                Log.d("mass_info", "in current week: ${lastDownloadedInfo.first()}")
                filePath.value = "${directory}/${lastDownloadedInfo.first()}"
            } else
                filePath.value = ""
        } else {
            Log.d("mass_info", "exists")
            filePath.value = "${directory}/${lastDownloadedInfo.first()}"
        }
    }

    private fun downloadIn12Hours(
        lastDownloaded: String,
        currentMillis: Long
    ): Boolean {
        val millis = lastDownloaded.toLongOrNull()
        return if (millis != null) {
            Log.d("mass_hours", millis.toString())
            Log.d("mass_hours", (12.hours).inWholeMilliseconds.toString())
            Log.d("mass_hours", (currentMillis - millis).toString())
            (currentMillis - millis) < (12.hours).inWholeMilliseconds
        } else false
    }

    private fun lastSunday(): Long {
        val calendar: Calendar = Calendar.getInstance(Locale.ITALY)
        calendar.firstDayOfWeek = 1
        calendar.add(Calendar.DAY_OF_WEEK, -(calendar[Calendar.DAY_OF_WEEK] - 1))
        return calendar.timeInMillis
    }

    private fun massInfoFromCurrentWeek(
        lastDownloaded: String
    ): Boolean {
        val millis = lastDownloaded.dropLast(4).toLongOrNull()
        return if (millis != null) {
            millis >= lastSunday()
        } else false
    }

    private fun deletePrevious(directory: File, currentMillis: Long) {
        for (file in directory.listFiles()!!) {
            if (file.name != currentMillis.toString()) {
                Log.d("mass_delete", "file: ${file.name}")
                file.delete()
            }
        }
    }

    @SuppressLint("Range", "SimpleDateFormat")
    private fun runDownload(context: Context, directory: String, filename: String) {
        //Toast.makeText(context, "run", Toast.LENGTH_SHORT).show()
        val ret = checkAndRequestPermissions(context)
        val dateString = SimpleDateFormat("ddMMyyyy").format(lastSunday())
        val fileURL = "https://www.farabrehy.sk//uploads/oznamy/${dateString}.jpg"

        Log.d("mass_download", "onPostExecute")
        status.value = "Sťahovanie nezačalo. Stlačte tlačídlo \"Stiahnúť\""
        Log.d("mass_download", "fileURL: ${fileURL}")
        if (fileURL.isNotEmpty()) {
            //Toast.makeText(context, "work started", Toast.LENGTH_SHORT).show()
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
            //Toast.makeText(context, "download manager", Toast.LENGTH_SHORT).show()
            Log.d("mass_download", "downloadManager done")
            val downloadID = downloadManager.enqueue(request)
            Log.d("mass_download", "downloadID done")
            var finishDownload = false
            while (!finishDownload) {
                //Toast.makeText(context, "pracuje na stahovani", Toast.LENGTH_SHORT).show()
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
                                Toast.makeText(context, statusToast, Toast.LENGTH_SHORT).show()
                            }
                        }

                        DownloadManager.STATUS_PAUSED -> {
                            if (statusToast != "Sťahovanie pozastavené.") {
                                statusToast = "Sťahovanie pozastavené."
                                Toast.makeText(context, statusToast, Toast.LENGTH_SHORT).show()
                            }
                        }

                        DownloadManager.STATUS_PENDING -> {
                            if (statusToast != "Čaká sa na sťahovanie.") {
                                statusToast = "Čaká sa na sťahovanie."
                                Toast.makeText(context, statusToast, Toast.LENGTH_SHORT).show()
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
                                    Toast.makeText(context, statusToast, Toast.LENGTH_SHORT).show()
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

}