package sk.brehy.lector

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.fragment.app.activityViewModels
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.kizitonwose.calendar.core.yearMonth
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.MonthHeaderFooterBinder
import com.kizitonwose.calendar.view.ViewContainer
import sk.brehy.MainActivity
import sk.brehy.MainViewModel
import sk.brehy.adapters.PeopleAdapter
import sk.brehy.R
import sk.brehy.adapters.People
import sk.brehy.databinding.FragmentLectorBinding
import java.time.LocalDate
import java.time.YearMonth

class LectorFragment : Fragment() {

    private val lectorModel: LectorViewModel by activityViewModels()
    private val databaseModel: MainViewModel by activityViewModels()
    internal lateinit var binding: FragmentLectorBinding
    lateinit var selectedDay: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLectorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lectorModel.weekends.observe(viewLifecycleOwner) { weekends ->
            //binding.calendarView.setHighlightedDays(weekends) //TODO
        }

        lectorModel.calendarDatabase = databaseModel.calendarDatabase
        lectorModel.getData()

        lectorModel.lectorList.observe(viewLifecycleOwner) { _ ->
            setCalendar()
            writeToListView(LocalDate.now().toString())
        }

        /*lectorModel.highlightWeekends()*/
        binding.floatButton.setOnClickListener {
            if (MainViewModel().isConnectedToInternet(requireContext())) {
                val (number, date) = lectorModel.addNewLector(selectedDay.replace("-0", "-"))
                openDialog(number, date, "", "Pridať lektora")
            } else
                (activity as MainActivity).showToast(
                    "Nie ste pripojený na internet.",
                    R.drawable.network_background,
                    R.color.brown_light
                )
        }
    }

    private fun setCalendar() {
        var DAYS = listOf("Po", "Ut", "St", "Št", "Pi", "So", "Ne")
        var MONTHS = listOf(
            "Január",
            "Február",
            "Marec",
            "Apríl",
            "Máj",
            "Jún",
            "Júl",
            "August",
            "September",
            "Október",
            "November",
            "December"
        )

        val currentDate = LocalDate.now().toString()
        selectedDay = if(::selectedDay.isInitialized) selectedDay else currentDate

        binding.calendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {
            override fun create(view: View) = DayViewContainer(view)

            override fun bind(container: DayViewContainer, data: CalendarDay) {
                container.textView.text = data.date.dayOfMonth.toString()
                val date = "${data.date.year}-${data.date.monthValue}-${data.date.dayOfMonth}"
                container.day = data
                container.fragment = this@LectorFragment

                if (data.date.toString() == currentDate) {
                    container.textView.setTextColor(
                        ContextCompat.getColor(
                            requireActivity(),
                            R.color.red
                        )
                    )
                }

                if (date in lectorModel.lectorList.value!!.keys) {
                    container.numberOfLectors.text =
                        lectorModel.lectorList.value!![date]!!.size.toString()
                    if (data.date.toString() == selectedDay) {
                        container.numberOfLectors.setBackgroundDrawable(
                            ContextCompat.getDrawable(
                                requireActivity(),
                                R.drawable.round_red
                            )
                        )
                    } else {
                        container.numberOfLectors.setBackgroundDrawable(
                            ContextCompat.getDrawable(
                                requireActivity(),
                                R.drawable.round
                            )
                        )
                    }
                } else {
                    if (data.date.toString() == selectedDay) {
                        container.numberOfLectors.setBackgroundDrawable(
                            ContextCompat.getDrawable(
                                requireActivity(),
                                R.drawable.round_red
                            )
                        )
                    } else {
                        container.numberOfLectors.setBackgroundDrawable(
                            ContextCompat.getDrawable(
                                requireActivity(),
                                R.drawable.round_invisible
                            )
                        )
                    }
                }

                if (data.position != DayPosition.MonthDate) {
                    container.textView.setTextColor(
                        ContextCompat.getColor(
                            requireActivity(),
                            R.color.brown_semilight
                        )
                    )
                    if (container.numberOfLectors.text != "") {
                        container.numberOfLectors.setTextColor(
                            ContextCompat.getColor(
                                requireActivity(),
                                R.color.brown
                            )
                        )
                        container.numberOfLectors.setBackgroundDrawable(
                            ContextCompat.getDrawable(
                                requireActivity(),
                                R.drawable.round_light
                            )
                        )
                    }
                }
            }
        }

        binding.calendarView.monthHeaderBinder = object :
            MonthHeaderFooterBinder<MonthViewContainer> {
            override fun create(view: View) = MonthViewContainer(view)
            override fun bind(container: MonthViewContainer, data: CalendarMonth) {
                container.monthTitle.text =
                    "${MONTHS[data.yearMonth.monthValue - 1]} ${data.yearMonth.year}"
                container.daysContainer.children
                    .map { it as TextView }
                    .forEachIndexed { index, textView ->
                        textView.text = DAYS[index]
                    }
            }
        }

        val currentMonth = YearMonth.now()
        val startMonth = currentMonth.minusMonths(4) // Adjust as needed
        val endMonth = currentMonth.plusMonths(100) // Adjust as needed
        val firstDayOfWeek = firstDayOfWeekFromLocale() // Available from the library
        binding.calendarView.setup(startMonth, endMonth, firstDayOfWeek)
        binding.calendarView.scrollToMonth(currentMonth)
    }

    fun writeToListView(date: String) {
        val people = lectorModel.lectorList.value!![date]
        if (people != null) {
            binding.listviewCalendar.adapter = PeopleAdapter(requireActivity(), people)
            binding.listviewCalendar.setOnItemClickListener { _, _, position, _ ->
                val people = people[position]
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

    internal fun openDialog(number: String, date: String, name: String, title: String) {
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

class DayViewContainer(view: View) : ViewContainer(view) {
    val textView = view.findViewById<TextView>(R.id.calendarDayText)
    val numberOfLectors = view.findViewById<TextView>(R.id.number_lectors)
    lateinit var day: CalendarDay
    lateinit var fragment: LectorFragment

    init {
        view.setOnClickListener {
            var oldSelected = fragment.selectedDay
            fragment.selectedDay = day.date.toString()
            if (day.position == DayPosition.MonthDate) {
                fragment.binding.calendarView.notifyDateChanged(LocalDate.parse(oldSelected))
            } else {
                fragment.binding.calendarView.scrollToMonth(day.date.yearMonth)
            }
            fragment.binding.calendarView.notifyDateChanged(LocalDate.parse(day.date.toString()))
            fragment.writeToListView(day.date.toString().replace("-0", "-"))
        }
    }
}

class MonthViewContainer(view: View) : ViewContainer(view) {
    val monthTitle = view.findViewById<TextView>(R.id.month_title)
    val daysContainer = view.findViewById<ViewGroup>(R.id.days_container) as ViewGroup
}