package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.alarm.AlarmScheduler
import com.example.data.AppPreferences
import com.example.data.ReminderDatabase
import com.example.data.ReminderRepository
import com.example.model.AppLanguage
import com.example.model.Category
import com.example.model.MemberItem
import com.example.model.Priority
import com.example.model.Recurrence
import com.example.model.ReminderItem
import com.example.model.SmartParseResult
import com.example.parser.SmartReminderParser
import com.example.util.LocaleHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class ReminderFilter(val labelRes: Int) {
    ALL(R.string.tab_all),
    TODAY(R.string.section_today),
    UPCOMING(R.string.section_upcoming),
    HIGH_PRIORITY(R.string.tab_important),
    COMPLETED(R.string.section_completed)
}

data class ReminderStats(
    val total: Int = 0,
    val today: Int = 0,
    val overdue: Int = 0,
    val upcoming: Int = 0,
    val completed: Int = 0,
    val highPriority: Int = 0
)

class ReminderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ReminderRepository
    private val alarmScheduler = AlarmScheduler(application)
    val preferences = AppPreferences(application)

    init {
        val db = ReminderDatabase.getDatabase(application)
        repository = ReminderRepository(db.reminderDao(), db.memberDao())
    }

    // App Language State
    private val _currentLanguage = MutableStateFlow(AppLanguage.fromCode(preferences.selectedLanguageCode))
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    // Theme Mode: "SYSTEM", "LIGHT", "DARK"
    private val _themeMode = MutableStateFlow(preferences.themeMode)
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    // Premium Ads-Free State
    private val _isPremiumNoAds = MutableStateFlow(preferences.isPremiumNoAds)
    val isPremiumNoAds: StateFlow<Boolean> = _isPremiumNoAds.asStateFlow()

    // Onboarding State
    private val _isOnboardingCompleted = MutableStateFlow(preferences.hasCompletedOnboarding)
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()

    val allReminders: StateFlow<List<ReminderItem>> = repository.allReminders.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allMembers: StateFlow<List<MemberItem>> = repository.allMembers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(ReminderFilter.ALL)
    val selectedFilter: StateFlow<ReminderFilter> = _selectedFilter.asStateFlow()

    private val _selectedCategory = MutableStateFlow<Category?>(null)
    val selectedCategory: StateFlow<Category?> = _selectedCategory.asStateFlow()

    private val _quickInputText = MutableStateFlow("")
    val quickInputText: StateFlow<String> = _quickInputText.asStateFlow()

    private val _smartParsedPreview = MutableStateFlow<SmartParseResult?>(null)
    val smartParsedPreview: StateFlow<SmartParseResult?> = _smartParsedPreview.asStateFlow()

    val stats: StateFlow<ReminderStats> = allReminders.combine(_selectedFilter) { list, _ ->
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfToday = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        val endOfToday = cal.timeInMillis

        val total = list.size
        val completed = list.count { it.isCompleted }
        val todayCount = list.count { !it.isCompleted && it.dueTimestamp in startOfToday..endOfToday }
        val overdueCount = list.count { !it.isCompleted && it.dueTimestamp < now }
        val upcomingCount = list.count { !it.isCompleted && it.dueTimestamp > endOfToday }
        val highPriorityCount = list.count { !it.isCompleted && (it.priority == Priority.HIGH || it.priority == Priority.URGENT) }

        ReminderStats(
            total = total,
            today = todayCount,
            overdue = overdueCount,
            upcoming = upcomingCount,
            completed = completed,
            highPriority = highPriorityCount
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReminderStats()
    )

    val filteredReminders: StateFlow<List<ReminderItem>> = combine(
        allReminders,
        _searchQuery,
        _selectedFilter,
        _selectedCategory
    ) { reminders, query, filter, catFilter ->
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfToday = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        val endOfToday = cal.timeInMillis

        reminders.filter { reminder ->
            val matchesQuery = query.isBlank() ||
                    reminder.title.contains(query, ignoreCase = true) ||
                    reminder.description.contains(query, ignoreCase = true) ||
                    reminder.category.title.contains(query, ignoreCase = true)

            val matchesCategory = catFilter == null || reminder.category == catFilter

            val matchesFilter = when (filter) {
                ReminderFilter.ALL -> true
                ReminderFilter.TODAY -> !reminder.isCompleted && reminder.dueTimestamp in startOfToday..endOfToday
                ReminderFilter.UPCOMING -> !reminder.isCompleted && reminder.dueTimestamp > endOfToday
                ReminderFilter.HIGH_PRIORITY -> !reminder.isCompleted && (reminder.priority == Priority.HIGH || reminder.priority == Priority.URGENT)
                ReminderFilter.COMPLETED -> reminder.isCompleted
            }

            matchesQuery && matchesCategory && matchesFilter
        }.sortedWith(
            compareBy<ReminderItem> { it.isCompleted }
                .thenBy {
                    // Put overdue first among uncompleted items
                    if (!it.isCompleted && it.dueTimestamp < now) 0 else 1
                }
                .thenBy { it.dueTimestamp }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onQuickInputChange(newText: String) {
        _quickInputText.value = newText
        if (newText.isNotBlank()) {
            _smartParsedPreview.value = SmartReminderParser.parse(newText)
        } else {
            _smartParsedPreview.value = null
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onFilterSelected(filter: ReminderFilter) {
        _selectedFilter.value = filter
    }

    fun onCategorySelected(category: Category?) {
        _selectedCategory.value = if (_selectedCategory.value == category) null else category
    }

    fun addFromQuickInput() {
        val text = _quickInputText.value.trim()
        if (text.isBlank()) return

        val parsed = SmartReminderParser.parse(text)
        val defaultDue = System.currentTimeMillis() + (60 * 60 * 1000) // 1 hour from now

        val reminder = ReminderItem(
            title = parsed.cleanTitle.ifBlank { text },
            dueTimestamp = parsed.dueTimestamp ?: defaultDue,
            category = parsed.category ?: Category.PERSONAL,
            priority = parsed.priority ?: Priority.MEDIUM,
            recurrence = parsed.recurrence ?: Recurrence.NONE,
            notificationEnabled = true
        )

        viewModelScope.launch {
            val id = repository.insertReminder(reminder)
            val created = reminder.copy(id = id)
            alarmScheduler.schedule(created)
            _quickInputText.value = ""
            _smartParsedPreview.value = null
        }
    }

    fun addTemplateReminder(title: String, category: Category, priority: Priority, offsetMinutes: Int) {
        val dueTime = System.currentTimeMillis() + (offsetMinutes * 60 * 1000)
        val reminder = ReminderItem(
            title = title,
            dueTimestamp = dueTime,
            category = category,
            priority = priority,
            recurrence = Recurrence.NONE,
            notificationEnabled = true
        )
        viewModelScope.launch {
            val id = repository.insertReminder(reminder)
            val created = reminder.copy(id = id)
            alarmScheduler.schedule(created)
        }
    }

    fun saveReminder(
        id: Long = 0,
        title: String,
        description: String,
        dueTimestamp: Long,
        category: Category,
        priority: Priority,
        recurrence: Recurrence,
        notificationEnabled: Boolean,
        memberId: Long? = null,
        memberName: String? = null
    ) {
        if (title.isBlank()) return

        val reminder = ReminderItem(
            id = id,
            title = title.trim(),
            description = description.trim(),
            dueTimestamp = dueTimestamp,
            category = category,
            priority = priority,
            recurrence = recurrence,
            notificationEnabled = notificationEnabled,
            memberId = memberId,
            memberName = memberName
        )

        viewModelScope.launch {
            val savedId = repository.insertReminder(reminder)
            val scheduledReminder = reminder.copy(id = if (id == 0L) savedId else id)
            if (notificationEnabled && !scheduledReminder.isCompleted) {
                alarmScheduler.schedule(scheduledReminder)
            } else {
                alarmScheduler.cancel(scheduledReminder.id)
            }
        }
    }

    fun toggleComplete(reminder: ReminderItem) {
        val newStatus = !reminder.isCompleted
        viewModelScope.launch {
            if (newStatus && reminder.recurrence != Recurrence.NONE) {
                // If completing a recurring reminder, schedule the next occurrence!
                val nextTimestamp = calculateNextRecurrence(reminder.dueTimestamp, reminder.recurrence)
                // Mark this completed, and create the next occurrence
                repository.setCompleted(reminder.id, true)
                alarmScheduler.cancel(reminder.id)

                val nextReminder = reminder.copy(
                    id = 0,
                    dueTimestamp = nextTimestamp,
                    isCompleted = false,
                    createdTimestamp = System.currentTimeMillis()
                )
                val newId = repository.insertReminder(nextReminder)
                alarmScheduler.schedule(nextReminder.copy(id = newId))
            } else {
                repository.setCompleted(reminder.id, newStatus)
                if (newStatus) {
                    alarmScheduler.cancel(reminder.id)
                } else if (reminder.dueTimestamp > System.currentTimeMillis()) {
                    alarmScheduler.schedule(reminder.copy(isCompleted = false))
                }
            }
        }
    }

    fun snooze(reminder: ReminderItem, minutes: Int) {
        val newTime = System.currentTimeMillis() + (minutes * 60 * 1000)
        viewModelScope.launch {
            repository.snoozeReminder(reminder.id, newTime)
            val updated = reminder.copy(dueTimestamp = newTime, isCompleted = false)
            alarmScheduler.schedule(updated)
        }
    }

    fun deleteReminder(reminder: ReminderItem) {
        viewModelScope.launch {
            alarmScheduler.cancel(reminder.id)
            repository.deleteById(reminder.id)
        }
    }

    fun triggerTestNotification() {
        alarmScheduler.scheduleTestNotification(
            title = "Drink Water & Stretch!",
            category = Category.HEALTH.name,
            priority = Priority.HIGH.name
        )
    }

    private fun calculateNextRecurrence(baseTime: Long, recurrence: Recurrence): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = if (baseTime > System.currentTimeMillis()) baseTime else System.currentTimeMillis()
        }
        when (recurrence) {
            Recurrence.DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
            Recurrence.WEEKDAYS -> {
                do {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                } while (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
            }
            Recurrence.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            Recurrence.MONTHLY -> cal.add(Calendar.MONTH, 1)
            Recurrence.YEARLY -> cal.add(Calendar.YEAR, 1)
            Recurrence.NONE -> {}
        }
        return cal.timeInMillis
    }

    fun setAppLanguage(language: AppLanguage) {
        preferences.selectedLanguageCode = language.code
        _currentLanguage.value = language
        LocaleHelper.setAppLocale(getApplication(), language.code)
    }

    fun setThemeMode(mode: String) {
        preferences.themeMode = mode
        _themeMode.value = mode
    }

    fun setPremiumNoAds(enabled: Boolean) {
        preferences.isPremiumNoAds = enabled
        _isPremiumNoAds.value = enabled
    }

    fun completeOnboarding() {
        preferences.hasCompletedOnboarding = true
        _isOnboardingCompleted.value = true
    }

    // Member Management
    fun addMember(name: String, relationship: String, phone: String, notes: String, colorHex: Long) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.insertMember(
                MemberItem(
                    name = name.trim(),
                    relationship = relationship.trim().ifBlank { "Family" },
                    phone = phone.trim(),
                    notes = notes.trim(),
                    colorHex = colorHex
                )
            )
        }
    }

    fun updateMember(member: MemberItem) {
        viewModelScope.launch {
            repository.updateMember(member)
        }
    }

    fun deleteMember(member: MemberItem) {
        viewModelScope.launch {
            repository.deleteMember(member)
        }
    }

    fun clearCompletedReminders() {
        viewModelScope.launch {
            val completed = allReminders.value.filter { it.isCompleted }
            completed.forEach { reminder ->
                repository.deleteReminder(reminder)
            }
        }
    }
}
