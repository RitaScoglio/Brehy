package sk.brehy.lector

import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import sk.brehy.adapters.People
import sk.brehy.R
import java.util.ArrayList
import java.util.Calendar

class LectorViewModel : ViewModel() {

    lateinit var calendarDatabase: DatabaseReference

    var lectorList = MutableLiveData<MutableMap<String, List<People>>>()
    var weekends = MutableLiveData<MutableList<Calendar>>()

    fun getData() {
        deleteOldData()
        calendarDatabase.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val list = mutableMapOf<String, List<People>>()
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
                            list["$y-$m-$d"] = all
                        }
                    }
                }
                lectorList.value = list
                Log.d("CLICK", "${lectorList.value}")

            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FATAL_CalendarAddValue", error.message)
            }
        })
    }

    private fun deleteOldData() {
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        val deleteMonth = currentMonth - 3
        if (deleteMonth > 0) calendarDatabase.child(
            Calendar.getInstance().get(Calendar.YEAR).toString()
        )
            .child((deleteMonth + 1).toString()).removeValue()
        else calendarDatabase.child((Calendar.getInstance().get(Calendar.YEAR) - 1).toString())
            .child((deleteMonth + 13).toString()).removeValue()
    }

    fun dialogPositiveButton(name: String, date: String, number: String, keys: Array<String>) {
        if (name != "") {
            calendarDatabase.child(keys[0]).child(keys[1]).child(keys[2]).child(number)
                .setValue(name)
        } else {
            val people = lectorList.value?.get(date) as MutableList
            if (people.size > number.toInt() - 1) {
                calendarDatabase.child(keys[0]).child(keys[1]).child(keys[2])
                    .removeValue()
                people.removeAt(number.toInt() - 1)
                if (people.isEmpty()) lectorList.value!!.remove(date) else {
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
    }

    fun checkLogIn(context: Context): Boolean {
        val settings = context.getSharedPreferences("FarnostBrehy", 0)
        return settings.getBoolean("logedIn", false)
    }

    fun saveLogIn(context: Context) {
        val settings = context.getSharedPreferences("FarnostBrehy", 0)
        val editor = settings.edit()
        editor.putBoolean("logedIn", true).apply()
    }

    fun highlightWeekends() {
        weekends.value = mutableListOf()
        val calendar = Calendar.getInstance()
        calendar[Calendar.DAY_OF_MONTH] = 1
        val month = calendar[Calendar.MONTH]
        var i = 1
        do {
            val day = calendar[Calendar.DAY_OF_WEEK]
            if (day == Calendar.SATURDAY || day == Calendar.SUNDAY) {
                val weekend = Calendar.getInstance()
                weekend[Calendar.DAY_OF_MONTH] = i
                weekends.value!!.add(weekend)
            }
            i++
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        } while (calendar[Calendar.MONTH] == month)

    }

    fun addNewLector(date:String): Pair<String, String> {
        val current = lectorList.value?.get(date)
        val number: Int = if (current == null) 1 else current.size + 1
        return Pair(number.toString(), date)
    }

}