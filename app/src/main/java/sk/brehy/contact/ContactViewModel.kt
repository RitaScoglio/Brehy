package sk.brehy.contact

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel

class ContactViewModel : ViewModel() {
    fun textClipboard(label: String, text: String, context: Context) {
        val clipboard = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }

    fun newIntent(uri: String, action: String, context: Context) {
        val sendIntent = Intent(action, Uri.parse(uri))
        context.startActivity(sendIntent)
    }
}