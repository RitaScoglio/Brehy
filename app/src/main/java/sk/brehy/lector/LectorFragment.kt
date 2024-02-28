package sk.brehy.lector

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import com.applandeo.materialcalendarview.CalendarDay
import com.applandeo.materialcalendarview.CalendarView
import com.applandeo.materialcalendarview.listeners.OnCalendarDayClickListener
import com.applandeo.materialcalendarview.listeners.OnCalendarPageChangeListener
import com.applandeo.materialcalendarview.utils.CalendarProperties
import sk.brehy.MainActivity
import sk.brehy.MainViewModel
import sk.brehy.adapters.PeopleAdapter
import sk.brehy.R
import sk.brehy.databinding.FragmentLectorBinding
import java.util.Calendar

class LectorFragment : Fragment() {

    private val lectorModel: LectorViewModel by activityViewModels()
    private lateinit var binding: FragmentLectorBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLectorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.calendarView.setOnPreviousPageChangeListener(object : OnCalendarPageChangeListener {
            override fun onChange() {
                lectorModel.highlightWeekends()
            }
        })
        binding.calendarView.setOnForwardPageChangeListener(object : OnCalendarPageChangeListener {
            override fun onChange() {
                lectorModel.highlightWeekends()
            }
        })
        binding.calendarView.setOnCalendarDayClickListener(object : OnCalendarDayClickListener{
            override fun onClick(calendarDay: CalendarDay) {
                /*val today = Calendar.getInstance()
                val clickedDay = calendarDay.calendar
                val sameDay = today[Calendar.DAY_OF_YEAR] == clickedDay[Calendar.DAY_OF_YEAR] &&
                        today[Calendar.YEAR] == clickedDay[Calendar.YEAR]
                if (!sameDay) {
                    //properties.selectionColor =
                      //  ContextCompat.getColor(requireContext(), R.color.brown_dark)
                } else {
                    //properties.selectionColor = ContextCompat.getColor(requireContext(), R.color.red)
                    //binding.calendarView.requestLayout()
                }*/
                writeToListView(calendarDay.calendar)
            }
        })
        lectorModel.highlightWeekends()
        binding.floatButton.setOnClickListener {
            if (MainViewModel().isConnectedToInternet(requireContext())) {
                val (number, date) = lectorModel.addNewLector(binding.calendarView.firstSelectedDate)
                openDialog(number, date, "", "Pridať lektora")
            } else
                (activity as MainActivity).showToast(
                    "Nie ste pripojený na internet.",
                    R.drawable.network_background,
                    R.color.brown_light
                )
        }

        lectorModel.weekends.observe(viewLifecycleOwner) { weekends ->
            binding.calendarView.setHighlightedDays(weekends) //TODO
        }

        lectorModel.lectorList.observe(viewLifecycleOwner) { _ ->
            binding.calendarView.setCalendarDays(lectorModel.refreshScreenData(requireActivity()))
            val selected = binding.calendarView.firstSelectedDate
            writeToListView(selected)
        }
    }

    private fun writeToListView(selected: Calendar) {
        val date =
            "${selected[Calendar.YEAR]}-${(selected[Calendar.MONTH] + 1)}-${selected[Calendar.DAY_OF_MONTH]}"
        val current = lectorModel.lectorList.value!![date]
        if (current != null) {
            binding.listviewCalendar.adapter = PeopleAdapter(requireActivity(), current)
            binding.listviewCalendar.setOnItemClickListener { _, _, position, _ ->
                val people = current[position]
                if (MainViewModel().isConnectedToInternet(requireContext()))
                    openDialog(
                        people.number,
                        date,
                        people.name,
                        "Upraviť lektora"
                    )
                else
                    (activity as MainActivity).showToast(
                        "Nie ste pripojený na internet.",
                        R.drawable.network_background,
                        R.color.brown_light
                    )
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
        positive.setOnClickListener { _: View? ->
            lectorModel.dialogPositiveButton(
                edit.text.toString(),
                date,
                number,
                keys
            )
            dialog.dismiss()
        }
        val negative = dialog.findViewById<View>(R.id.negative) as Button
        negative.setOnClickListener { dialog.dismiss() }
        dialog.show()
        dialog.window!!.attributes = layout
    }
}