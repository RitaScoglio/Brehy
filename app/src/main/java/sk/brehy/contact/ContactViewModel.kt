package sk.brehy.contact

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import sk.brehy.exception.BrehyException
import androidx.core.net.toUri

class ContactViewModel : ViewModel() {
    fun textClipboard(label: String, text: String, context: Context) {
        try {
            val clipboard = context.getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager
                ?: throw BrehyException("ClipboardManager service not available in context.")
            val clip = ClipData.newPlainText(label, text)
            clipboard.setPrimaryClip(clip)
        } catch (e: Exception) {
            throw BrehyException("Failed to copy text to clipboard.", e)
        }
    }

    fun newIntent(uri: String, action: String, context: Context) {
        try {
            val sendIntent = Intent(action, uri.toUri())
            if (sendIntent.resolveActivity(context.packageManager) == null) {
                throw BrehyException("No activity found to handle intent with action '$action' and uri '$uri'.")
            }
            context.startActivity(sendIntent)
        } catch (e: ActivityNotFoundException) {
            throw BrehyException(
                "Failed to start activity for intent with action '$action' and uri '$uri'.",
                e
            )
        } catch (e: Exception) {
            throw BrehyException("Unexpected error when creating or starting intent.", e)
        }
    }
}