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
import java.util.*


open class MainViewModel : ViewModel() {

    lateinit var newsDatabase: DatabaseReference
    lateinit var calendarDatabase: DatabaseReference

    internal fun initiateFirebase() {
            val database = FirebaseDatabase.getInstance(Secret.firebase.url)
            database.setPersistenceEnabled(true)
            newsDatabase = database.getReference("aktuality")
            newsDatabase.keepSynced(true)
            calendarDatabase = database.getReference("kalendar")
            calendarDatabase.keepSynced(true)
        //write
        //databaseReference.child("try").setValue("trying")
        //read
        /* myRef.addValueEventListener(object: ValueEventListener {
             override fun onDataChange(snapshot: DataSnapshot) {
                 // This method is called once with the initial value and again
                 // whenever data at this location is updated.
                 val value = snapshot.getValue<String>()
                 Log.d("mValue", "Value is: " + value)
             }
             override fun onCancelled(error: DatabaseError) {
                 Log.w("mValue", "Failed to read value.", error.toException())
             }
         })*/
    }

    fun initiateFirebaseAuth(activity: Activity) {
        val auth = FirebaseAuth.getInstance()
        auth.signInWithEmailAndPassword(Secret.firebase.email, Secret.firebase.password)
            .addOnCompleteListener(activity) { task ->
                if (task.isSuccessful) {
                    Log.d("FirebaseAuth", "createUserWithEmail:success")
                } else {
                    Log.e("FATAL", "createUserWithEmail:failure", task.exception)
                }
            }
    }

    fun initiateGoogleMessagingService(context: Context) {
        val settings = context.getSharedPreferences("FarnostBrehy", 0)
        val subscribed = settings.getBoolean("subscribed", false)
        if (!subscribed) FirebaseMessaging.getInstance().subscribeToTopic("aktuality")
        settings.edit().putBoolean("subscribed", true).apply()
    }

    fun isConnectedToInternet(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
            return when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                else -> false
            }
        } else {
            @Suppress("DEPRECATION") val networkInfo =
                connectivityManager.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION")
            return networkInfo.isConnected
        }
    }
}