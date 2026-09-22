package com.example.parser

import com.example.model.Category
import com.example.model.Priority
import com.example.model.Recurrence
import com.example.model.SmartParseResult
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

object SmartReminderParser {

    fun parse(input: String): SmartParseResult {
        val raw = input.trim()
        if (raw.isEmpty()) {
            return SmartParseResult(cleanTitle = "", dueTimestamp = null, category = null, priority = null, recurrence = null)
        }

        val lower = raw.lowercase(Locale.ROOT)

        // 1. Detect Category
        val category = detectCategory(lower)

        // 2. Detect Priority
        val priority = detectPriority(lower)

        // 3. Detect Recurrence
        val recurrence = detectRecurrence(lower)

        // 4. Detect Date & Time
        val (dueTimestamp, matchedTimeTokens) = detectDateTime(lower)

        // 5. Clean Title: remove time strings, urgent words, etc.
        var cleanTitle = raw
        // Remove common prefixes like "remind me to", "remind me", "please remind me to"
        cleanTitle = cleanTitle.replace(Regex("(?i)^\\s*(please\\s+)?remind\\s+me\\s+(to\\s+)?"), "")
        cleanTitle = cleanTitle.replace(Regex("(?i)\\b(remind\\s+me\\s+to|remind\\s+me)\\b"), "")

        for (token in matchedTimeTokens) {
            cleanTitle = cleanTitle.replace(Regex("(?i)\\b" + Pattern.quote(token) + "\\b"), " ")
        }
        // Remove priority words from title if standalone
        val priorityWords = listOf("urgent", "asap", "critical", "important")
        for (w in priorityWords) {
            cleanTitle = cleanTitle.replace(Regex("(?i)\\b$w\\b"), " ")
        }
        // Remove recurrence words
        val recWords = listOf("every day", "everyday", "daily", "every week", "weekly", "weekdays", "every month", "monthly", "every year", "yearly")
        for (w in recWords) {
            cleanTitle = cleanTitle.replace(Regex("(?i)\\b$w\\b"), " ")
        }

        cleanTitle = cleanTitle.replace(Regex("\\s+"), " ").trim()
        if (cleanTitle.isEmpty()) {
            cleanTitle = raw
        }

        return SmartParseResult(
            cleanTitle = cleanTitle,
            dueTimestamp = dueTimestamp,
            category = category,
            priority = priority,
            recurrence = recurrence
        )
    }

    private fun detectCategory(text: String): Category? {
        val healthKeywords = listOf("medicine", "meds", "tablet", "pill", "doctor", "dentist", "clinic", "hospital", "workout", "gym", "walk", "run", "water", "hydration", "yoga", "exercise", "dawai", "paani", "checkup")
        val billsKeywords = listOf("pay", "bill", "rent", "recharge", "electricity", "wifi", "subscription", "fees", "fee", "emi", "credit card", "loan", "invoice", "payment", "chalan")
        val shoppingKeywords = listOf("buy", "shop", "groceries", "grocery", "milk", "eggs", "veggies", "fruits", "vegetables", "supermarket", "market", "order", "amazon", "flipkart")
        val workKeywords = listOf("meeting", "sync", "standup", "client", "presentation", "report", "review", "email", "interview", "deadline", "project", "demo", "office", "boss", "colleague")
        val studyKeywords = listOf("study", "exam", "test", "homework", "assignment", "revision", "lecture", "chapter", "reading", "quiz", "notes", "math", "science", "class")
        val familyKeywords = listOf("mom", "dad", "mother", "father", "parents", "brother", "sister", "wife", "husband", "son", "daughter", "kids", "children", "baby", "bhai", "behen", "papa", "mummy")
        val personalKeywords = listOf("call", "birthday", "gift", "party", "friend", "dinner", "movie", "flight", "travel", "vacation", "haircut", "dentist", "car wash")

        return when {
            familyKeywords.any { text.contains(it) } -> Category.FAMILY
            healthKeywords.any { text.contains(it) } -> Category.HEALTH
            billsKeywords.any { text.contains(it) } -> Category.BILLS
            shoppingKeywords.any { text.contains(it) } -> Category.SHOPPING
            workKeywords.any { text.contains(it) } -> Category.WORK
            studyKeywords.any { text.contains(it) } -> Category.STUDY
            personalKeywords.any { text.contains(it) } -> Category.PERSONAL
            else -> null
        }
    }

    private fun detectPriority(text: String): Priority? {
        return when {
            text.contains("urgent") || text.contains("asap") || text.contains("emergency") || text.contains("critical") || text.contains("right now") -> Priority.URGENT
            text.contains("important") || text.contains("priority") || text.contains("must do") || text.contains("crucial") -> Priority.HIGH
            text.contains("maybe") || text.contains("optional") || text.contains("someday") || text.contains("casual") -> Priority.LOW
            else -> null
        }
    }

    private fun detectRecurrence(text: String): Recurrence? {
        return when {
            text.contains("every day") || text.contains("everyday") || text.contains("daily") || text.contains("each day") -> Recurrence.DAILY
            text.contains("weekdays") || text.contains("mon to fri") || text.contains("mon-fri") || text.contains("workdays") -> Recurrence.WEEKDAYS
            text.contains("weekly") || text.contains("every week") -> Recurrence.WEEKLY
            text.contains("monthly") || text.contains("every month") -> Recurrence.MONTHLY
            text.contains("yearly") || text.contains("every year") || text.contains("annually") -> Recurrence.YEARLY
            else -> null
        }
    }

    private fun detectDateTime(text: String): Pair<Long?, List<String>> {
        val matched = mutableListOf<String>()
        val cal = Calendar.getInstance()
        var timeFound = false

        // Pattern 1: "in X minutes/mins/min"
        val inMinutesRegex = Regex("\\bin (\\d+)\\s*(minutes|minute|mins|min)\\b")
        val inMinutesMatch = inMinutesRegex.find(text)
        if (inMinutesMatch != null) {
            val mins = inMinutesMatch.groupValues[1].toIntOrNull() ?: 10
            cal.add(Calendar.MINUTE, mins)
            matched.add(inMinutesMatch.value)
            return Pair(cal.timeInMillis, matched)
        }

        // Pattern 2: "in X hours/hrs/hr"
        val inHoursRegex = Regex("\\bin (\\d+)\\s*(hours|hour|hrs|hr)\\b")
        val inHoursMatch = inHoursRegex.find(text)
        if (inHoursMatch != null) {
            val hrs = inHoursMatch.groupValues[1].toIntOrNull() ?: 1
            cal.add(Calendar.HOUR_OF_DAY, hrs)
            matched.add(inHoursMatch.value)
            return Pair(cal.timeInMillis, matched)
        }

        // Pattern 3: "in X days"
        val inDaysRegex = Regex("\\bin (\\d+)\\s*(days|day)\\b")
        val inDaysMatch = inDaysRegex.find(text)
        if (inDaysMatch != null) {
            val days = inDaysMatch.groupValues[1].toIntOrNull() ?: 1
            cal.add(Calendar.DAY_OF_YEAR, days)
            cal.set(Calendar.HOUR_OF_DAY, 9)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            matched.add(inDaysMatch.value)
            timeFound = true
        }

        // Detect day modifier: "tomorrow", "tonight", "today"
        var isTomorrow = false
        if (text.contains("tomorrow")) {
            isTomorrow = true
            matched.add("tomorrow")
            cal.add(Calendar.DAY_OF_YEAR, 1)
            timeFound = true
        } else if (text.contains("tonight")) {
            matched.add("tonight")
            cal.set(Calendar.HOUR_OF_DAY, 20)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            if (cal.timeInMillis < System.currentTimeMillis()) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            return Pair(cal.timeInMillis, matched)
        }

        // Detect specific time pattern: "at 5pm", "at 10:30 am", "at 14:00", "5:30pm", "7pm", "9am"
        val timeRegex = Regex("\\b(?:at\\s+)?(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm|a\\.m\\.|p\\.m\\.)?\\b")
        val timeMatches = timeRegex.findAll(text)
        var explicitHourSet = false

        for (match in timeMatches) {
            val fullMatch = match.value.trim()
            val hourStr = match.groupValues[1]
            val minuteStr = match.groupValues[2]
            val amPm = match.groupValues[3].lowercase()

            val hourVal = hourStr.toIntOrNull() ?: continue
            val minuteVal = if (minuteStr.isNotEmpty()) minuteStr.toIntOrNull() ?: 0 else 0

            if (amPm.isNotEmpty()) {
                var h = hourVal
                if (amPm.startsWith("p") && h < 12) h += 12
                if (amPm.startsWith("a") && h == 12) h = 0
                cal.set(Calendar.HOUR_OF_DAY, h)
                cal.set(Calendar.MINUTE, minuteVal)
                cal.set(Calendar.SECOND, 0)
                matched.add(fullMatch)
                explicitHourSet = true
                timeFound = true
                break
            } else if (fullMatch.startsWith("at ") || match.range.first > 0 && text.substring(0, match.range.first).trimEnd().endsWith("at")) {
                // E.g. "at 5" or "at 14:00"
                var h = hourVal
                if (h in 1..7) h += 12 // Default "at 5" to 5 PM
                cal.set(Calendar.HOUR_OF_DAY, h)
                cal.set(Calendar.MINUTE, minuteVal)
                cal.set(Calendar.SECOND, 0)
                matched.add(fullMatch)
                explicitHourSet = true
                timeFound = true
                break
            }
        }

        // Broad day period words if no explicit hour set
        if (!explicitHourSet) {
            when {
                text.contains("morning") -> {
                    matched.add("morning")
                    cal.set(Calendar.HOUR_OF_DAY, 9)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    timeFound = true
                }
                text.contains("afternoon") -> {
                    matched.add("afternoon")
                    cal.set(Calendar.HOUR_OF_DAY, 14)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    timeFound = true
                }
                text.contains("evening") -> {
                    matched.add("evening")
                    cal.set(Calendar.HOUR_OF_DAY, 18)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    timeFound = true
                }
                text.contains("night") -> {
                    matched.add("night")
                    cal.set(Calendar.HOUR_OF_DAY, 21)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    timeFound = true
                }
                isTomorrow -> {
                    // Default tomorrow to 9:00 AM
                    cal.set(Calendar.HOUR_OF_DAY, 9)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                }
            }
        }

        // If timeFound but the calculated timestamp is in the past, push by 1 day
        if (timeFound && cal.timeInMillis <= System.currentTimeMillis() && !isTomorrow) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        return if (timeFound) {
            Pair(cal.timeInMillis, matched)
        } else {
            Pair(null, matched)
        }
    }
}
