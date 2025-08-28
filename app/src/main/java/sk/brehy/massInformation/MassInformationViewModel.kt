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
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import sk.brehy.MainViewModel
import sk.brehy.exception.BrehyException
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.time.Duration.Companion.hours

class MassInformationViewModel : ViewModel() {

    var status = MutableLiveData<String>()
    var filePath = MutableLiveData<String>()
    private var statusToast = ""

    fun getAvailableMassInformation(context: Context) {
        try {
            val calendar: Calendar = Calendar.getInstance(Locale.ITALY)
            calendar.firstDayOfWeek = 1
            val path = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: throw BrehyException("Failed to get external downloads directory.")

            val currentMillis = calendar.timeInMillis
            saveCurrentWeek(path, context, currentMillis)
        } catch (e: Exception) {
            throw BrehyException("Failed to get available mass information.", e)
        }
    }

    private fun saveCurrentWeek(
        directory: File,
        context: Context,
        currentMillis: Long
    ) {
        try {
            val files = directory.listFiles()
                ?: throw BrehyException("Failed to list files in directory: ${directory.path}")
            val lastDownloadedInfo = files.map { it.name }

            if (!downloadIn12Hours(
                    lastDownloadedInfo.ifEmpty { listOf("invalid") }.first(),
                    currentMillis
                )
            ) {
                if (MainViewModel().isConnectedToInternet(context)) {
                    viewModelScope.async {
                        try {
                            runDownload(context, directory.toString(), currentMillis.toString())
                            deletePrevious(directory, currentMillis)
                        } catch (e: Exception) {
                            throw BrehyException("Download or cleanup failed.", e)
                        }
                    }
                } else if (massInfoFromCurrentWeek(lastDownloadedInfo.ifEmpty { listOf("invalid") }
                        .first())) {
                    filePath.value = "${directory}/${lastDownloadedInfo.first()}"
                } else
                    filePath.value = ""
            } else {
                filePath.value = "${directory}/${lastDownloadedInfo.first()}"
            }
        } catch (e: Exception) {
            throw BrehyException("Failed during saveCurrentWeek process.", e)
        }
    }

    private fun downloadIn12Hours(
        lastDownloaded: String,
        currentMillis: Long
    ): Boolean {
        return try {
            val millis = lastDownloaded.toLongOrNull()
                ?: throw BrehyException("Invalid timestamp format in filename: $lastDownloaded")
            (currentMillis - millis) < (12.hours).inWholeMilliseconds
        } catch (e: Exception) {
            throw BrehyException("Error checking if download is within 12 hours.", e)
        }
    }

    private fun lastSunday(): Long {
        return try {
            val calendar: Calendar = Calendar.getInstance(Locale.ITALY)
            calendar.firstDayOfWeek = 1
            calendar.add(Calendar.DAY_OF_WEEK, -(calendar[Calendar.DAY_OF_WEEK] - 1))
            calendar.timeInMillis
        } catch (e: Exception) {
            throw BrehyException("Failed to calculate last Sunday.", e)
        }
    }

    private fun massInfoFromCurrentWeek(
        lastDownloaded: String
    ): Boolean {
        return try {
            val millis = lastDownloaded.dropLast(4).toLongOrNull()
                ?: throw BrehyException("Invalid timestamp format in filename: $lastDownloaded")
            millis >= lastSunday()
        } catch (e: Exception) {
            throw BrehyException("Error checking if mass info is from current week.", e)
        }
    }

    private fun deletePrevious(directory: File, currentMillis: Long) {
        try {
            val files = directory.listFiles()
                ?: throw BrehyException("Failed to list files in directory: ${directory.path}")
            for (file in files) {
                if (file.name != currentMillis.toString()) {
                    if (!file.delete()) {
                        throw BrehyException("Failed to delete file: ${file.path}")
                    }
                }
            }
        } catch (e: Exception) {
            throw BrehyException("Error during deleting previous files.", e)
        }
    }

    @SuppressLint("Range", "SimpleDateFormat")
    private fun runDownload(context: Context, directory: String, filename: String) {
        try {
            if (!checkAndRequestPermissions(context)) {
                throw BrehyException("Necessary permissions not granted.")
            }

            val dateString = SimpleDateFormat("ddMMyyyy").format(lastSunday())
            val fileURL = "https://www.farabrehy.sk//uploads/oznamy/${dateString}.jpg"

            status.value = "Download has not started. Press the \"Download\" button."
            if (fileURL.isEmpty()) {
                throw BrehyException("Download URL is empty.")
            }

            status.value = "Downloading in progress."
            val file = File("$directory/$filename")
            val request = DownloadManager.Request(Uri.parse(fileURL))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationUri(Uri.fromFile(file))
                .setTitle(filename)
                .setDescription("Downloading")
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val downloadManager =
                context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                    ?: throw BrehyException("Download manager not available.")

            val downloadID = downloadManager.enqueue(request)
            var finishDownload = false
            while (!finishDownload) {
                val cursor: Cursor =
                    downloadManager.query(DownloadManager.Query().setFilterById(downloadID))
                        ?: throw BrehyException("Failed to query download status.")
                try {
                    if (cursor.moveToFirst()) {
                        when (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))) {
                            DownloadManager.STATUS_FAILED -> {
                                finishDownload = true
                                if (statusToast != "Download failed.") {
                                    statusToast = "Download failed."
                                    Toast.makeText(context, statusToast, Toast.LENGTH_SHORT).show()
                                }
                                throw BrehyException("Download failed for file: $filename")
                            }

                            DownloadManager.STATUS_PAUSED -> {
                                if (statusToast != "Download paused.") {
                                    statusToast = "Download paused."
                                    Toast.makeText(context, statusToast, Toast.LENGTH_SHORT).show()
                                }
                            }

                            DownloadManager.STATUS_PENDING -> {
                                if (statusToast != "Waiting for download.") {
                                    statusToast = "Waiting for download."
                                    Toast.makeText(context, statusToast, Toast.LENGTH_SHORT).show()
                                }
                            }

                            DownloadManager.STATUS_RUNNING -> {
                                val total: Long =
                                    cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                                if (total >= 0 && statusToast != "Downloading.") {
                                    statusToast = "Downloading."
                                    Toast.makeText(context, statusToast, Toast.LENGTH_SHORT).show()
                                }
                            }

                            DownloadManager.STATUS_SUCCESSFUL -> {
                                finishDownload = true
                                filePath.value = file.path
                            }

                            else -> {
                                throw BrehyException("Unknown download status encountered.")
                            }
                        }
                    } else {
                        throw BrehyException("Failed to move cursor to first position.")
                    }
                } finally {
                    cursor.close()
                }
            }
        } catch (e: Exception) {
            throw BrehyException("Error during file download.", e)
        }
    }

    private fun checkAndRequestPermissions(context: Context): Boolean {
        try {
            val permissions = listOf(
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.INTERNET,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )

            val listPermissionsNeeded = permissions.filter {
                ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
            }

            if (listPermissionsNeeded.isNotEmpty()) {
                for (permission in listPermissionsNeeded) {
                    ActivityCompat.requestPermissions(context as Activity, arrayOf(permission), 1)
                }
                return false
            }
            return true
        } catch (e: Exception) {
            throw BrehyException("Error while checking or requesting permissions.", e)
        }
    }
}
