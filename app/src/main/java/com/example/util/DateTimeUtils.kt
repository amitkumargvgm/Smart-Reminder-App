package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateTimeUtils {

    fun formatRelativeDueTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = timestamp - now
        val isPast = diff < 0
        val absDiff = Math.abs(diff)

        val calDue = Calendar.getInstance().apply { timeInMillis = timestamp }
        val calNow = Calendar.getInstance()

        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val timeStr = timeFormat.format(Date(timestamp))

        val isToday = calDue.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
                calDue.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR)

        val calTomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        val isTomorrow = calDue.get(Calendar.YEAR) == calTomorrow.get(Calendar.YEAR) &&
                calDue.get(Calendar.DAY_OF_YEAR) == calTomorrow.get(Calendar.DAY_OF_YEAR)

        val calYesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val isYesterday = calDue.get(Calendar.YEAR) == calYesterday.get(Calendar.YEAR) &&
                calDue.get(Calendar.DAY_OF_YEAR) == calYesterday.get(Calendar.DAY_OF_YEAR)

        return when {
            isPast -> {
                val minsPast = absDiff / (60 * 1000)
                val hoursPast = absDiff / (60 * 60 * 1000)
                val daysPast = absDiff / (24 * 60 * 60 * 1000)
                when {
                    minsPast < 60 -> "Overdue by ${minsPast.coerceAtLeast(1)}m"
                    hoursPast < 24 -> "Overdue by ${hoursPast}h"
                    isYesterday -> "Yesterday at $timeStr"
                    else -> "Overdue by ${daysPast}d ($timeStr)"
                }
            }
            else -> {
                val minsUntil = diff / (60 * 1000)
                val hoursUntil = diff / (60 * 60 * 1000)
                when {
                    minsUntil < 1 -> "Due now"
                    minsUntil < 60 -> "In $minsUntil mins"
                    isToday -> "Today at $timeStr"
                    isTomorrow -> "Tomorrow at $timeStr"
                    hoursUntil < 48 -> "Tomorrow at $timeStr"
                    else -> {
                        val dateFormat = SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault())
                        dateFormat.format(Date(timestamp))
                    }
                }
            }
        }
    }

    fun formatDateOnly(timestamp: Long): String {
        val sdf = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatFullDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMMM d, yyyy • h:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatTimeOnly(timestamp: Long): String {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
