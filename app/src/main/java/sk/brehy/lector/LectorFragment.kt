package sk.brehy.lector

import android.app.Dialog
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.Observer
import com.applandeo.materialcalendarview.CalendarView
import com.applandeo.materialcalendarview.utils.CalendarProperties
import sk.brehy.MainActivity
import sk.brehy.DatabaseMainViewModel
import sk.brehy.adapters.People
import sk.brehy.adapters.PeopleAdapter
import sk.brehy.R
import sk.brehy.databinding.FragmentLectorBinding
import java.util.Calendar

class LectorFragment : Fragment() {

    companion object {
        fun newInstance() = LectorFragment()
    }

    private lateinit var lectorModel: LectorViewModel
    private lateinit var databaseModel: DatabaseMainViewModel
    private lateinit var binding: FragmentLectorBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLectorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        lectorModel = ViewModelProvider(this).get(LectorViewModel::class.java)
        databaseModel = ViewModelProvider(requireActivity()).get(DatabaseMainViewModel::class.java)

        val calendar = CalendarView::class.java.getDeclaredField("mCalendarProperties")
        calendar.isAccessible = true
        val properties = calendar[binding.calendarView] as CalendarProperties
        properties.selectionColor = resources.getColor(R.color.red)
        binding.calendarView.requestLayout()
        binding.calendarView.setOnPreviousPageChangeListener {
            lectorModel.refreshScreenData(databaseModel.lector_list.value!!, requireActivity())
            lectorModel.highlightWeekends()
        }
        binding.calendarView.setOnForwardPageChangeListener {
            lectorModel.refreshScreenData(databaseModel.lector_list.value!!, requireActivity())
            lectorModel.highlightWeekends()

        }
        binding.calendarView.setOnDayClickListener { eventDay ->
            val today = Calendar.getInstance()
            val clickedDay = eventDay.calendar
            val sameDay = today[Calendar.DAY_OF_YEAR] == clickedDay[Calendar.DAY_OF_YEAR] &&
                    today[Calendar.YEAR] == clickedDay[Calendar.YEAR]
            if (!sameDay) {
                properties.selectionColor = resources.getColor(R.color.brown_dark)
                binding.calendarView.requestLayout()
            } else {
                properties.selectionColor = resources.getColor(R.color.red)
                binding.calendarView.requestLayout()
            }
            writeToListView(databaseModel.lector_list.value!!, clickedDay)
        }

        lectorModel.highlightWeekends()
        binding.floatButton.setOnClickListener(View.OnClickListener {
            if (databaseModel.isConnectedToInternet(requireContext()))
                writeToDatabase(binding.calendarView.selectedDate)
            else
                (activity as MainActivity).showToast("Nie ste pripojený na internet.", R.drawable.network_background, R.color.brown_light)
        })

        lectorModel.weekends.observe(viewLifecycleOwner, Observer { weekends ->
            binding.calendarView.setHighlightedDays(weekends)
        })

        databaseModel.lector_list.observe(viewLifecycleOwner, Observer { list ->
            binding.calendarView.setEvents(lectorModel.refreshScreenData(list, requireActivity()))
            val selected = binding.calendarView.selectedDate
            writeToListView(list, selected)
        })
    }

    private fun writeToDatabase(calendar: Calendar) {
        val date =
            "${calendar[Calendar.YEAR]}-${(calendar[Calendar.MONTH] + 1)}-${calendar[Calendar.DAY_OF_MONTH]}"
        val current = databaseModel.lector_list.value?.get(date)
        val number: Int
        number = if (current == null) 1 else current.size + 1
        openDialog(Integer.toString(number), date, "", "Pridať lektora")
    }

    fun writeToListView(list: MutableMap<String, List<People>>, selected: Calendar) {
        val date =
            "${selected[Calendar.YEAR]}-${(selected[Calendar.MONTH] + 1)}-${selected[Calendar.DAY_OF_MONTH]}"
        val current = list[date]
        if (current != null) {
            binding.listviewCalendar.adapter = PeopleAdapter(requireActivity(), current)
            binding.listviewCalendar.setOnItemClickListener { parent, view, position, id ->
                val people = current[position]
                if (databaseModel.isConnectedToInternet(requireContext()))
                    openDialog(
                        people.number,
                        date,
                        people.name,
                        "Upraviť lektora"
                    )
                else
                    (activity as MainActivity).showToast("Nie ste pripojený na internet.", R.drawable.network_background, R.color.brown_light)
            }
        } else binding.listviewCalendar.adapter = null
    }

    private fun openDialog(number: String, date: String, name: String, title: String) {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.lector_dialog)
        val keys = date.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val layout = WindowManager.LayoutParams()
        layout.copyFrom(dialog.window!!.attributes)
        layout.width = WindowManager.LayoutParams.MATCH_PARENT
        val titleView = dialog.findViewById<TextView>(R.id.head)
        titleView.text = title
        val dateView = dialog.findViewById<TextView>(R.id.date)
        val text =
            keys[2] + ". " + resources.getStringArray(R.array.material_calendar_months_array)[keys[1].toInt() - 1] + " " + keys[0]
        dateView.text = text
        val numberView = dialog.findViewById<TextView>(R.id.number)
        numberView.text = "Poradie: $number"
        val edit = dialog.findViewById<View>(R.id.name) as EditText
        edit.setText(name)
        val positive = dialog.findViewById<View>(R.id.positive) as Button
        positive.setOnClickListener { v: View? ->
            databaseModel.dialogPositiveButtonLector(
                edit.text.toString(),
                date,
                number,
                keys
            )
            //lectorModel.refreshScreenData(, requireActivity())
            dialog.dismiss()
        }
        val negative = dialog.findViewById<View>(R.id.negative) as Button
        negative.setOnClickListener { dialog.dismiss() }
        dialog.show()
        dialog.window!!.attributes = layout
    }
}