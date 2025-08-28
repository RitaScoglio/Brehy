package sk.brehy.lector

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import sk.brehy.adapters.People
import sk.brehy.exception.BrehyException
import java.util.ArrayList
import java.util.Calendar

class LectorViewModel : ViewModel() {

    lateinit var calendarDatabase: DatabaseReference

    var lectorList = MutableLiveData<MutableMap<String, List<People>>>()
    var weekends = MutableLiveData<MutableList<Calendar>>()

    fun getData() {
        try {
            deleteOldData()
            calendarDatabase.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    try {
                        val list = mutableMapOf<String, List<People>>()
                        val years = dataSnapshot.children
                        for (year in years) {
                            val y = year.key
                                ?: throw BrehyException("Year key is null in database snapshot.")
                            val months = year.children
                            for (month in months) {
                                val m = month.key
                                    ?: throw BrehyException("Month key is null in database snapshot.")
                                val days = month.children
                                for (day in days) {
                                    val d = day.key
                                        ?: throw BrehyException("Day key is null in database snapshot.")
                                    val people = day.children
                                    val all = mutableListOf<People>()
                                    for (human in people) {
                                        val humanKey = human.key
                                            ?: throw BrehyException("Human key is null in database snapshot.")
                                        val humanValue = human.value as? String
                                            ?: throw BrehyException("Human value is not a String or is null.")
                                        all.add(People(humanKey, humanValue))
                                    }
                                    list["$y-$m-$d"] = all
                                }
                            }
                        }
                        lectorList.value = list
                        Log.d("CLICK", "${lectorList.value}")
                    } catch (e: Exception) {
                        throw BrehyException(
                            "Failed while processing data snapshot in getData().",
                            e
                        )
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FATAL_CalendarAddValue", error.message)
                    throw BrehyException(
                        "Firebase database read cancelled: ${error.message}",
                        error.toException()
                    )
                }
            })
        } catch (e: Exception) {
            throw BrehyException("Failed to add Firebase ValueEventListener in getData().", e)
        }
    }

    private fun deleteOldData() {
        try {
            val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val deleteMonth = currentMonth - 3
            if (deleteMonth > 0) {
                calendarDatabase.child(currentYear.toString())
                    .child((deleteMonth + 1).toString()).removeValue()
            } else {
                calendarDatabase.child((currentYear - 1).toString())
                    .child((deleteMonth + 13).toString()).removeValue()
            }
        } catch (e: Exception) {
            throw BrehyException("Failed to delete old data in deleteOldData().", e)
        }
    }

    fun dialogPositiveButton(name: String, date: String, number: String, keys: Array<String>) {
        try {
            if (name.isNotEmpty()) {
                calendarDatabase.child(keys[0]).child(keys[1]).child(keys[2]).child(number)
                    .setValue(name)
            } else {
                val people = lectorList.value?.get(date) as? MutableList<People>
                    ?: throw BrehyException("People list for date '$date' is null or not mutable.")
                if (people.size > number.toInt() - 1) {
                    calendarDatabase.child(keys[0]).child(keys[1]).child(keys[2])
                        .removeValue()
                    people.removeAt(number.toInt() - 1)
                    if (people.isEmpty()) {
                        lectorList.value!!.remove(date)
                    } else {
                        var order = 1
                        val newPeople = ArrayList<People>()
                        for (human in people) {
                            newPeople.add(People(order.toString(), human.name))
                            calendarDatabase.child(keys[0]).child(keys[1]).child(keys[2])
                                .child(order.toString()).setValue(human.name)
                            order++
                        }
                        lectorList.value!![date] = newPeople
                    }
                }
            }
        } catch (e: Exception) {
            throw BrehyException("Failed during dialogPositiveButton operation.", e)
        }
    }

    fun getCheckBoxValue(context: Context): Boolean {
        return try {
            val settings = context.getSharedPreferences("FarnostBrehy", 0)
            settings.getBoolean("weekMode", false)
        } catch (e: Exception) {
            throw BrehyException("Failed to get checkbox value from SharedPreferences.", e)
        }
    }

    fun saveCheckBoxValue(context: Context, value: Boolean) {
        try {
            val settings = context.getSharedPreferences("FarnostBrehy", 0)
            val editor = settings.edit()
            editor.putBoolean("weekMode", value).apply()
        } catch (e: Exception) {
            throw BrehyException("Failed to save checkbox value to SharedPreferences.", e)
        }
    }

    fun checkLogIn(context: Context): Boolean {
        return try {
            val settings = context.getSharedPreferences("FarnostBrehy", 0)
            settings.getBoolean("logedIn", false)
        } catch (e: Exception) {
            throw BrehyException("Failed to get login status from SharedPreferences.", e)
        }
    }

    fun saveLogIn(context: Context) {
        try {
            val settings = context.getSharedPreferences("FarnostBrehy", 0)
            val editor = settings.edit()
            editor.putBoolean("logedIn", true).apply()
        } catch (e: Exception) {
            throw BrehyException("Failed to save login status to SharedPreferences.", e)
        }
    }

    fun addNewLector(date: String): Pair<String, String> {
        try {
            val current = lectorList.value?.get(date)
            val number: Int = if (current == null) 1 else current.size + 1
            return Pair(number.toString(), date)
        } catch (e: Exception) {
            throw BrehyException("Failed to add new lector for date: $date", e)
        }
    }
}
