package sk.brehy

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import sk.brehy.exception.BrehyException
import androidx.core.content.edit

open class MainViewModel : ViewModel() {

    lateinit var newsDatabase: DatabaseReference
    lateinit var calendarDatabase: DatabaseReference

    internal fun initiateFirebase() {
        try {
            val database = FirebaseDatabase.getInstance(Secret.firebase.url)
            database.setPersistenceEnabled(true)
            try {
                newsDatabase = database.getReference("aktuality")
                newsDatabase.keepSynced(true)
            } catch (e: Exception) {
                throw BrehyException("Error initializing newsDatabase", e)
            }
            try {
                calendarDatabase = database.getReference("kalendar")
                calendarDatabase.keepSynced(true)
            } catch (e: Exception) {
                throw BrehyException("Error initializing calendarDatabase", e)
            }
        } catch (e: Exception) {
            throw BrehyException("Error initializing FirebaseDatabase", e)
        }
    }

    fun initiateFirebaseAuth(activity: Activity) {
        try {
            val auth = FirebaseAuth.getInstance()
            auth.signInWithEmailAndPassword(Secret.firebase.email, Secret.firebase.password)
                .addOnCompleteListener(activity) { task ->
                    if (task.isSuccessful) {
                        Log.d("FirebaseAuth", "createUserWithEmail:success")
                    } else {
                        // We only log here, but consider throwing if needed
                        Log.e("FATAL", "createUserWithEmail:failure", task.exception)
                    }
                }
        } catch (e: Exception) {
            throw BrehyException("Error initializing FirebaseAuth", e)
        }
    }

    fun initiateGoogleMessagingService(context: Context) {
        try {
            val settings = context.getSharedPreferences("FarnostBrehy", 0)
            val subscribed = settings.getBoolean("subscribed", false)
            if (!subscribed) {
                try {
                    FirebaseMessaging.getInstance().subscribeToTopic("aktuality")
                } catch (e: Exception) {
                    throw BrehyException("Error subscribing to topic 'aktuality'", e)
                }
            }
            settings.edit { putBoolean("subscribed", true) }
        } catch (e: Exception) {
            throw BrehyException("Error initializing Google Messaging Service", e)
        }
    }

    fun isConnectedToInternet(context: Context): Boolean {
        try {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork ?: return false
                val activeNetwork =
                    connectivityManager.getNetworkCapabilities(network) ?: return false
                when {
                    activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                    activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                    else -> false
                }
            } else {
                @Suppress("DEPRECATION") val networkInfo =
                    connectivityManager.activeNetworkInfo ?: return false
                @Suppress("DEPRECATION")
                networkInfo.isConnected
            }
        } catch (e: Exception) {
            throw BrehyException("Error checking internet connectivity", e)
        }
    }
}
