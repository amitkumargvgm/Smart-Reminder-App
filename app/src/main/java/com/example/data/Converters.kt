package com.example.data

import androidx.room.TypeConverter
import com.example.model.Category
import com.example.model.Priority
import com.example.model.Recurrence

class Converters {
    @TypeConverter
    fun fromPriority(priority: Priority?): String {
        return priority?.name ?: Priority.MEDIUM.name
    }

    @TypeConverter
    fun toPriority(value: String?): Priority {
        return value?.let {
            try {
                Priority.valueOf(it)
            } catch (e: Exception) {
                Priority.MEDIUM
            }
        } ?: Priority.MEDIUM
    }

    @TypeConverter
    fun fromCategory(category: Category?): String {
        return category?.name ?: Category.OTHER.name
    }

    @TypeConverter
    fun toCategory(value: String?): Category {
        return value?.let {
            try {
                Category.valueOf(it)
            } catch (e: Exception) {
                Category.OTHER
            }
        } ?: Category.OTHER
    }

    @TypeConverter
    fun fromRecurrence(recurrence: Recurrence?): String {
        return recurrence?.name ?: Recurrence.NONE.name
    }

    @TypeConverter
    fun toRecurrence(value: String?): Recurrence {
        return value?.let {
            try {
                Recurrence.valueOf(it)
            } catch (e: Exception) {
                Recurrence.NONE
            }
        } ?: Recurrence.NONE
    }
}
