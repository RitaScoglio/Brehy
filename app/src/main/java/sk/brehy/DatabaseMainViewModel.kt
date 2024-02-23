package sk.brehy

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging
import sk.brehy.adapters.People
import java.util.*


open class DatabaseMainViewModel : ViewModel() {

    lateinit var newsDatabase: DatabaseReference
    lateinit var calendarDatabase: DatabaseReference
    var lector_list = MutableLiveData<MutableMap<String, List<People>>>()
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
        val auth = FirebaseAuth.getInstance();
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
        if(!subscribed) FirebaseMessaging.getInstance().subscribeToTopic("aktuality")
        settings.edit().putBoolean("subscribed", true).apply()
    }

    fun isConnectedToInternet(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
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

    fun getLectorData() {
        deleteOldLectorData()
        calendarDatabase.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val list = mutableMapOf<String, List<People>>()
                Log.d("lektorov", "data snap")
                val years = dataSnapshot.children
                for (year in years) {
                    val y = year.key
                    val months = year.children
                    for (month in months) {
                        val m = month.key
                        val days = month.children
                        for (day in days) {
                            val d = day.key
                            val people = day.children
                            val all = mutableListOf<People>()
                            for (human in people) {
                                all.add(People(human.key as String, human.value as String))
                            }
                            list.put("$y-$m-$d", all)
                        }
                    }
                }
                lector_list.value = list
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FATAL_CalendarAddValue", error.message)
            }
        })
    }

    private fun deleteOldLectorData(){
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        val deleteMonth = currentMonth - 3
        if (deleteMonth > 0) calendarDatabase.child(Calendar.getInstance().get(Calendar.YEAR).toString())
            .child((deleteMonth + 1).toString()).removeValue()
        else calendarDatabase.child((Calendar.getInstance().get(Calendar.YEAR) - 1).toString())
            .child((deleteMonth + 13).toString()).removeValue()
    }

    fun dialogPositiveButtonLector(name: String, date: String, number: String, keys: Array<String>) {
        if (name != "") {
            calendarDatabase.child(keys[0]).child(keys[1]).child(keys[2]).child(number)
                .setValue(name)
        } else {
            val people = lector_list.value?.get(date) as MutableList
            if (people.size > number.toInt() - 1) {
                calendarDatabase.child(keys[0]).child(keys[1]).child(keys[2])
                    .removeValue()
                people.removeAt(number.toInt() - 1)
                if (people.isEmpty()) lector_list.value!!.remove(date) else {
                    var n = 1
                    val newPeople = ArrayList<People>()
                    for (human in people) {
                        newPeople.add(People(n.toString(), human.name))
                        calendarDatabase.child(keys[0]).child(keys[1]).child(keys[2])
                            .child(n.toString()).setValue(human.name)
                        n++
                    }
                    lector_list.value!!.put(date, newPeople)
                }
            }
        }
    }
}