package sk.brehy.lector

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
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
import com.kizitonwose.calendar.core.atStartOfMonth
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.kizitonwose.calendar.core.yearMonth
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.MonthHeaderFooterBinder
import com.kizitonwose.calendar.view.ViewContainer
import com.kizitonwose.calendar.view.WeekDayBinder
import com.kizitonwose.calendar.view.WeekHeaderFooterBinder
import sk.brehy.MainActivity
import sk.brehy.MainViewModel
import sk.brehy.adapters.PeopleAdapter
import sk.brehy.R
import sk.brehy.databinding.FragmentLectorBinding
import sk.brehy.exception.BrehyException
import java.time.LocalDate
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
        return try {
            binding = FragmentLectorBinding.inflate(inflater, container, false)
            binding.root
        } catch (e: Exception) {
            throw BrehyException("Failed to inflate layout for FragmentLectorBinding.", e)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lectorModel.weekends.observe(viewLifecycleOwner) { weekends ->
            // binding.calendarView.setHighlightedDays(weekends) //TODO
        }

        lectorModel.calendarDatabase = databaseModel.calendarDatabase
            ?: throw BrehyException("Calendar database is not initialized in databaseModel.")

        lectorModel.getData()

        lectorModel.lectorList.observe(viewLifecycleOwner) {
            setCalendars()
            writeToListView(LocalDate.now().toString())
        }

        binding.floatButton.setOnClickListener {
            if (MainViewModel().isConnectedToInternet(requireContext())) {
                val (number, date) = try {
                    lectorModel.addNewLector(selectedDay.toString().replace("-0", "-"))
                } catch (e: Exception) {
                    throw BrehyException("Failed to add new lector.", e)
                }
                openDialog(number, date, "", "Pridať lektora")
            } else {
                (activity as? MainActivity)?.showToast(
                    "Nie ste pripojený na internet.",
                    R.drawable.network_background,
                    R.color.brown_light
                ) ?: throw BrehyException("Activity is not MainActivity or is null.")
            }
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
        binding.WeekCalendarView.setup(
            startMonth.atStartOfMonth(),
            endMonth.atEndOfMonth(),
            firstDayOfWeek
        )
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
                        ContextCompat.getColor(requireActivity(), R.color.red)
                    )
                }

                val lectorMap = lectorModel.lectorList.value
                    ?: throw BrehyException("Lector list is null during binding.")

                if (lectorMap.containsKey(date)) {
                    val list = lectorMap[date]
                        ?: throw BrehyException("No lector data found for date $date.")
                    container.numberOfLectors.text = list.size.toString()
                    container.numberOfLectors.setBackgroundResource(
                        if (data.date == selectedDay) R.drawable.round_red else R.drawable.round
                    )
                } else {
                    container.numberOfLectors.setBackgroundResource(
                        if (data.date == selectedDay) R.drawable.round_red else R.drawable.round_invisible
                    )
                }

                if (data.position != DayPosition.MonthDate) {
                    container.textView.setTextColor(
                        ContextCompat.getColor(requireActivity(), R.color.brown_semilight)
                    )
                    if (container.numberOfLectors.text.isNotEmpty()) {
                        container.numberOfLectors.setTextColor(
                            ContextCompat.getColor(requireActivity(), R.color.brown)
                        )
                        container.numberOfLectors.setBackgroundResource(R.drawable.round_light)
                    }
                }
            }
        }

        binding.MonthCalendarView.monthHeaderBinder =
            object : MonthHeaderFooterBinder<MonthViewContainer> {
                override fun create(view: View) = MonthViewContainer(view)

                override fun bind(container: MonthViewContainer, data: CalendarMonth) {
                    container.monthTitle.text =
                        "${MONTHS[data.yearMonth.monthValue - 1]} ${data.yearMonth.year}"
                    container.daysContainer.children.map {
                        it as? TextView ?: throw BrehyException("Day header view is not a TextView")
                    }
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
                        ContextCompat.getColor(requireActivity(), R.color.red)
                    )
                }

                val lectorMap = lectorModel.lectorList.value
                    ?: throw BrehyException("Lector list is null during week binding.")

                if (lectorMap.containsKey(date)) {
                    val list = lectorMap[date]
                        ?: throw BrehyException("No lector data for date $date.")
                    container.numberOfLectors.text = list.size.toString()
                    container.numberOfLectors.setBackgroundResource(
                        if (data.date == selectedDay) R.drawable.round_red else R.drawable.round
                    )
                } else {
                    container.numberOfLectors.setBackgroundResource(
                        if (data.date == selectedDay) R.drawable.round_red else R.drawable.round_invisible
                    )
                }
            }
        }

        binding.WeekCalendarView.weekHeaderBinder =
            object : WeekHeaderFooterBinder<WeekViewContainer> {
                override fun create(view: View) = WeekViewContainer(view)

                override fun bind(container: WeekViewContainer, data: Week) {
                    val days = data.days
                    if (days.isEmpty()) throw BrehyException("Week data contains no days.")
                    val monthBegin = days.first().date.monthValue - 1
                    val monthEnd = days.last().date.monthValue - 1
                    val yearBegin = days.first().date.year
                    val yearEnd = days.last().date.year
                    val month =
                        if (monthBegin != monthEnd) "${MONTHS[monthBegin]}\\${MONTHS[monthEnd]}" else MONTHS[monthBegin]
                    val year = if (yearBegin != yearEnd) "$yearBegin\\$yearEnd" else "$yearBegin"

                    container.monthTitle.text = "$month $year"
                    container.daysContainer.children.map {
                        it as? TextView ?: throw BrehyException("Header item is not a TextView")
                    }
                        .forEachIndexed { index, textView ->
                            textView.text = DAYS[index]
                        }
                }
            }
    }

    fun writeToListView(date: String) {
        val lectorMap = lectorModel.lectorList.value
            ?: throw BrehyException("Lector list is null when trying to display data for date: $date")
        val people = lectorMap[date]
        if (people != null) {
            binding.listviewCalendar.adapter = PeopleAdapter(requireActivity(), people)
            binding.listviewCalendar.setOnItemClickListener { _, _, position, _ ->
                val person = people.getOrNull(position)
                    ?: throw BrehyException("No person found at position $position on date $date.")

                if (MainViewModel().isConnectedToInternet(requireContext())) {
                    openDialog(
                        person.number,
                        date,
                        person.name,
                        "Upraviť lektora"
                    )
                } else {
                    (activity as? MainActivity)?.showToast(
                        "Nie ste pripojený na internet.",
                        R.drawable.network_background,
                        R.color.brown_light
                    ) ?: throw BrehyException("Activity is not MainActivity or is null.")
                }
            }
        } else {
            binding.listviewCalendar.adapter = null
        }
    }

    @SuppressLint("SetTextI18n")
    internal fun openDialog(number: String, date: String, name: String, title: String) {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.lector_dialog)
        val keys = date.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (keys.size != 3)
            throw BrehyException("Date format is incorrect. Expected YYYY-MM-DD, got: $date")
        val layout = WindowManager.LayoutParams()
        layout.copyFrom(
            dialog.window?.attributes
                ?: throw BrehyException("Dialog window is null.")
        )
        layout.width = WindowManager.LayoutParams.MATCH_PARENT
        val titleView = dialog.findViewById<TextView>(R.id.head)
            ?: throw BrehyException("Dialog title view is missing.")
        titleView.text = title
        val dateView = dialog.findViewById<TextView>(R.id.date)
            ?: throw BrehyException("Dialog date view is missing.")
        dateView.text =
            "${keys[2]}. ${resources.getStringArray(R.array.material_calendar_months_array)[keys[1].toInt() - 1]} ${keys[0]}"
        val numberView = dialog.findViewById<TextView>(R.id.number)
            ?: throw BrehyException("Dialog number view is missing.")
        numberView.text = "Poradie: $number"

        val edit = dialog.findViewById<EditText>(R.id.name)
            ?: throw BrehyException("Dialog EditText is missing.")
        edit.setText(name)

        val positive = dialog.findViewById<Button>(R.id.positive)
            ?: throw BrehyException("Dialog positive button is missing.")
        positive.setOnClickListener {
            lectorModel.dialogPositiveButton(edit.text.toString(), date, number, keys)
            dialog.dismiss()
        }
        val negative = dialog.findViewById<Button>(R.id.negative)
            ?: throw BrehyException("Dialog negative button is missing.")
        negative.setOnClickListener { dialog.dismiss() }

        dialog.show()
        dialog.window?.attributes = layout
    }

    private val weekModeToggled =
        CompoundButton.OnCheckedChangeListener { buttonView, monthToWeek ->
            lectorModel.saveCheckBoxValue(requireContext(), binding.weekModeCheckBox.isChecked)
            if (monthToWeek) {
                binding.WeekCalendarView.scrollToWeek(selectedDay)
            } else {
                binding.MonthCalendarView.scrollToMonth(selectedDay.yearMonth)
            }
            val weekHeight = binding.WeekCalendarView.height
            val visibleMonthHeight = weekHeight *
                    binding.MonthCalendarView.findFirstVisibleMonth()?.weekDays.orEmpty().count()
            val oldHeight = if (monthToWeek) visibleMonthHeight else weekHeight
            val newHeight = if (monthToWeek) weekHeight else visibleMonthHeight
            val animator = ValueAnimator.ofInt(oldHeight, newHeight)
            animator.addUpdateListener { anim ->
                binding.MonthCalendarView.updateLayoutParams {
                    height = anim.animatedValue as Int
                }
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
                    binding.MonthCalendarView.updateLayoutParams { height = WRAP_CONTENT }
                }
            }
            animator.duration = 250
            animator.start()
        }
}

class DayViewContainer(view: View) : ViewContainer(view) {
    val textView = view.findViewById<TextView>(R.id.calendarDayText)
    val numberOfLectors = view.findViewById<TextView>(R.id.number_lectors)
    lateinit var day: CalendarDay
    lateinit var fragment: LectorFragment

    init {
        view.setOnClickListener {
            val oldSelected = fragment.selectedDay
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

    val textView: TextView = view.findViewById(R.id.calendarDayText)
        ?: throw BrehyException("TextView with id R.id.calendarDayText not found in WeekDayViewContainer.")

    val numberOfLectors: TextView = view.findViewById(R.id.number_lectors)
        ?: throw BrehyException("TextView with id R.id.number_lectors not found in WeekDayViewContainer.")

    lateinit var day: WeekDay
    lateinit var fragment: LectorFragment

    init {
        view.setOnClickListener {
            if (!::day.isInitialized) {
                throw BrehyException("WeekDayViewContainer: 'day' property was not initialized before click.")
            }
            if (!::fragment.isInitialized) {
                throw BrehyException("WeekDayViewContainer: 'fragment' property was not initialized before click.")
            }

            val oldSelected = fragment.selectedDay
            fragment.selectedDay = day.date

            val weekCalendarView = fragment.binding.WeekCalendarView
                ?: throw BrehyException("WeekCalendarView binding is null in WeekDayViewContainer.")

            weekCalendarView.notifyDateChanged(oldSelected)
            weekCalendarView.notifyDateChanged(day.date)

            notifyMonthDateChanged(oldSelected, day.date)

            fragment.writeToListView(day.date.toString().replace("-0", "-"))
        }
    }

    private fun notifyMonthDateChanged(oldSelected: LocalDate, date: LocalDate) {
        val monthCalendarView = fragment.binding.MonthCalendarView
            ?: throw BrehyException("MonthCalendarView binding is null in WeekDayViewContainer.")

        monthCalendarView.notifyDateChanged(oldSelected)
        monthCalendarView.notifyDateChanged(date)
    }
}

class WeekViewContainer(view: View) : ViewContainer(view) {

    val monthTitle: TextView = view.findViewById(R.id.month_title)
        ?: throw BrehyException("TextView with id R.id.month_title not found in WeekViewContainer.")

    val daysContainer: ViewGroup = view.findViewById(R.id.days_container) as? ViewGroup
        ?: throw BrehyException("ViewGroup with id R.id.days_container not found or is not a ViewGroup in WeekViewContainer.")
}

class MonthViewContainer(view: View) : ViewContainer(view) {

    val monthTitle: TextView = view.findViewById(R.id.month_title)
        ?: throw BrehyException("TextView with id R.id.month_title not found in MonthViewContainer.")

    val daysContainer: ViewGroup = view.findViewById(R.id.days_container) as? ViewGroup
        ?: throw BrehyException("ViewGroup with id R.id.days_container not found or is not a ViewGroup in MonthViewContainer.")
}
