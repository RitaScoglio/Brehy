package sk.brehy.lector

import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import com.amulyakhare.textdrawable.TextDrawable
import com.applandeo.materialcalendarview.EventDay
import sk.brehy.DatabaseMainViewModel
import sk.brehy.adapters.People
import sk.brehy.R
import java.util.Calendar

class LectorViewModel : DatabaseMainViewModel() {

    var weekends = MutableLiveData<MutableList<Calendar>>()

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

    fun refreshScreenData(list: MutableMap<String, List<People>>, activity: FragmentActivity): MutableList<EventDay> {
        val events = mutableListOf<EventDay>()
        for ((key, value) in list) {
            val date = key.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val name = value.size
            val calendar = Calendar.getInstance()
            calendar[date[0].toInt(), date[1].toInt() - 1] = date[2].toInt()
            val d = TextDrawable.builder()
                .beginConfig()
                .textColor(activity.resources.getColor(R.color.brown_superlight))
                .bold()
                .endConfig()
                .buildRound(Integer.toString(name), activity.resources.getColor(R.color.brown_dark))
            events.add(EventDay(calendar, d))
        }
        return events
    }

}