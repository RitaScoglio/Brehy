package sk.brehy.lector

import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import android.widget.Button
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.TextView
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.activityViewModels
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.Week
import com.kizitonwose.calendar.core.WeekDay
import com.kizitonwose.calendar.core.WeekDayPosition
import com.kizitonwose.calendar.core.atStartOfMonth
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.kizitonwose.calendar.core.yearMonth
import com.kizitonwose.calendar.view.CalendarView
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.MonthHeaderFooterBinder
import com.kizitonwose.calendar.view.ViewContainer
import com.kizitonwose.calendar.view.WeekCalendarView
import com.kizitonwose.calendar.view.WeekDayBinder
import com.kizitonwose.calendar.view.WeekHeaderFooterBinder
import sk.brehy.MainActivity
import sk.brehy.MainViewModel
import sk.brehy.adapters.PeopleAdapter
import sk.brehy.R
import sk.brehy.databinding.FragmentLectorBinding
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth

class LectorFragment : Fragment() {

    private val lectorModel: LectorViewModel by activityViewModels()
    private val databaseModel: MainViewModel by activityViewModels()
    internal lateinit var binding: FragmentLectorBinding
    lateinit var selectedDay: LocalDate

    //private val monthCalendarView: CalendarView get() = binding.MonthCalendarView
    //private val weekCalendarView: WeekCalendarView get() = binding.WeekCalendarView

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
            setCalendars()
            writeToListView(LocalDate.now().toString())
        }

        /*lectorModel.highlightWeekends()*/
        binding.floatButton.setOnClickListener {
            if (MainViewModel().isConnectedToInternet(requireContext())) {
                val (number, date) = lectorModel.addNewLector(selectedDay.toString().replace("-0", "-"))
                openDialog(number, date, "", "Pridať lektora")
            } else
                (activity as MainActivity).showToast(
                    "Nie ste pripojený na internet.",
                    R.drawable.network_background,
                    R.color.brown_light
                )
        }
    }

    private fun setCalendars() {
        binding.weekModeCheckBox.isChecked = lectorModel.getCheckBoxValue(requireContext())

        binding.MonthCalendarView.isInvisible = binding.weekModeCheckBox.isChecked
        binding.WeekCalendarView.isInvisible = !binding.weekModeCheckBox.isChecked
        binding.weekModeCheckBox.setOnCheckedChangeListener(weekModeToggled)

        val currentDate = LocalDate.now()
        selectedDay = if (::selectedDay.isInitialized) selectedDay else currentDate
        val currentMonth = YearMonth.now()
        val startMonth = currentMonth.minusMonths(4) // Adjust as needed
        val endMonth = currentMonth.plusMonths(100) // Adjust as needed
        val firstDayOfWeek = firstDayOfWeekFromLocale() // Available from the library

        setMonthCalendar(currentDate)
        binding.MonthCalendarView.setup(startMonth, endMonth, firstDayOfWeek)
        binding.MonthCalendarView.scrollToMonth(currentMonth)

        setWeekCalendar(currentDate)
        binding.WeekCalendarView.setup(startMonth.atStartOfMonth(), endMonth.atEndOfMonth(), firstDayOfWeek)
        binding.WeekCalendarView.scrollToWeek(currentDate)
    }

    private fun setMonthCalendar(currentDate: LocalDate) {
        binding.MonthCalendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {
            override fun create(view: View) = DayViewContainer(view)

            override fun bind(container: DayViewContainer, data: CalendarDay) {
                container.textView.text = data.date.dayOfMonth.toString()
                val date = "${data.date.year}-${data.date.monthValue}-${data.date.dayOfMonth}"
                container.day = data
                container.fragment = this@LectorFragment

                if (data.date == currentDate) {
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
                    if (data.date == selectedDay) {
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
                    if (data.date == selectedDay) {
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
        binding.MonthCalendarView.monthHeaderBinder = object :
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
    }

    private fun setWeekCalendar(currentDate: LocalDate) {
        binding.WeekCalendarView.dayBinder = object : WeekDayBinder<WeekDayViewContainer> {

            override fun create(view: View) = WeekDayViewContainer(view)

            override fun bind(container: WeekDayViewContainer, data: WeekDay) {
                container.textView.text = data.date.dayOfMonth.toString()
                val date = "${data.date.year}-${data.date.monthValue}-${data.date.dayOfMonth}"
                container.day = data
                container.fragment = this@LectorFragment

                if (data.date == currentDate) {
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
                    if (data.date == selectedDay) {
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
                    if (data.date == selectedDay) {
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

                /*if (data.position != DayPosition.MonthDate) {
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
                }*/
            }
        }

        binding.WeekCalendarView.weekHeaderBinder = object :
            WeekHeaderFooterBinder<WeekViewContainer> {
            override fun create(view: View) = WeekViewContainer(view)
            override fun bind(container: WeekViewContainer, data: Week) {
                val monthBegin = data.days.first().date.monthValue - 1
                val monthEnd = data.days.last().date.monthValue - 1
                val yearBegin = data.days.first().date.year
                val yearEnd = data.days.last().date.year
                val month = monthBegin.let { if(it != monthEnd)
                    "${MONTHS[monthBegin]}\\${MONTHS[monthEnd]}"
                else MONTHS[monthBegin] }
                val year = yearBegin.let { if(it != yearEnd)
                    "${yearBegin}\\${yearEnd}"
                else yearBegin}
                container.monthTitle.text = "$month $year"
                container.daysContainer.children
                    .map { it as TextView }
                    .forEachIndexed { index, textView ->
                        textView.text = DAYS[index]
                    }
            }
        }
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

    private val weekModeToggled = object : CompoundButton.OnCheckedChangeListener {
        override fun onCheckedChanged(buttonView: CompoundButton, monthToWeek: Boolean) {
            // We want the first visible day to remain visible after the
            // change so we scroll to the position on the target calendar.
            lectorModel.saveCheckBoxValue(requireContext(), binding.weekModeCheckBox.isChecked)

            if (monthToWeek) {
                //val targetDate = binding.WeekCalendarView.findFirstVisibleDay()?.date ?: return
                binding.WeekCalendarView.scrollToWeek(selectedDay)
            } else {
                // It is possible to have two months in the visible week (30 | 31 | 1 | 2 | 3 | 4 | 5)
                // We always choose the second one. Please use what works best for your use case.
                //val targetMonth = binding.WeekCalendarView.findLastVisibleDay()?.date?.yearMonth ?: return
                binding.MonthCalendarView.scrollToMonth(selectedDay.yearMonth)
            }

            val weekHeight = binding.WeekCalendarView.height
            // If OutDateStyle is EndOfGrid, you could simply multiply weekHeight by 6.
            val visibleMonthHeight = weekHeight *
                    binding.MonthCalendarView.findFirstVisibleMonth()?.weekDays.orEmpty().count()

            val oldHeight = if (monthToWeek) visibleMonthHeight else weekHeight
            val newHeight = if (monthToWeek) weekHeight else visibleMonthHeight

            // Animate calendar height changes.
            val animator = ValueAnimator.ofInt(oldHeight, newHeight)
            animator.addUpdateListener { anim ->
                binding.MonthCalendarView.updateLayoutParams {
                    height = anim.animatedValue as Int
                }
                // A bug is causing the month calendar to not redraw its children
                // with the updated height during animation, this is a workaround.
                binding.MonthCalendarView.children.forEach { child ->
                    child.requestLayout()
                }
            }

            animator.doOnStart {
                if (!monthToWeek) {
                    binding.WeekCalendarView.isInvisible = true
                    binding.MonthCalendarView.isVisible = true
                }
            }
            animator.doOnEnd {
                if (monthToWeek) {
                    binding.WeekCalendarView.isVisible = true
                    binding.MonthCalendarView.isInvisible = true
                } else {
                    // Allow the month calendar to be able to expand to 6-week months
                    // in case we animated using the height of a visible 5-week month.
                    // Not needed if OutDateStyle is EndOfGrid.
                    binding.MonthCalendarView.updateLayoutParams { height = WRAP_CONTENT }
                }
                //updateTitle()
            }
            animator.duration = 250
            animator.start()
        }
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
            fragment.selectedDay = day.date
            if (day.position == DayPosition.MonthDate) {
                fragment.binding.MonthCalendarView.notifyDateChanged(oldSelected)
            } else {
                fragment.binding.MonthCalendarView.scrollToMonth(day.date.yearMonth)
            }
            fragment.binding.MonthCalendarView.notifyDateChanged(day.date)
            notifyWeekDateChanged(oldSelected, day.date)
            fragment.writeToListView(day.date.toString().replace("-0", "-"))
        }
    }

    private fun notifyWeekDateChanged(oldSelected: LocalDate, date: LocalDate) {
        fragment.binding.WeekCalendarView.notifyDateChanged(oldSelected)
        fragment.binding.WeekCalendarView.notifyDateChanged(date)
    }
}

class WeekDayViewContainer(view: View) : ViewContainer(view) {
    val textView = view.findViewById<TextView>(R.id.calendarDayText)
    val numberOfLectors = view.findViewById<TextView>(R.id.number_lectors)
    lateinit var day: WeekDay
    lateinit var fragment: LectorFragment

    init {
        view.setOnClickListener {
            var oldSelected = fragment.selectedDay
            fragment.selectedDay = day.date
            fragment.binding.WeekCalendarView.notifyDateChanged(oldSelected)
            fragment.binding.WeekCalendarView.notifyDateChanged(day.date)
            notifyMonthDateChanged(oldSelected, day.date)
            fragment.writeToListView(day.date.toString().replace("-0", "-"))
        }
    }

    private fun notifyMonthDateChanged(oldSelected: LocalDate, date: LocalDate) {
        fragment.binding.MonthCalendarView.notifyDateChanged(oldSelected)
        fragment.binding.MonthCalendarView.notifyDateChanged(date)
    }
}

class WeekViewContainer(view: View) : ViewContainer(view) {
    val monthTitle = view.findViewById<TextView>(R.id.month_title)
    val daysContainer = view.findViewById<ViewGroup>(R.id.days_container) as ViewGroup
}

class MonthViewContainer(view: View) : ViewContainer(view) {
    val monthTitle = view.findViewById<TextView>(R.id.month_title)
    val daysContainer = view.findViewById<ViewGroup>(R.id.days_container) as ViewGroup
}
