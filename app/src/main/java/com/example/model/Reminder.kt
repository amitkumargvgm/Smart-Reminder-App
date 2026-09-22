package com.example.model

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.R

enum class Priority(val title: String, @StringRes val labelRes: Int, val level: Int, val color: Color) {
    LOW("Low", R.string.priority_low, 1, Color(0xFF64748B)),
    MEDIUM("Medium", R.string.priority_medium, 2, Color(0xFF3B82F6)),
    HIGH("High", R.string.priority_high, 3, Color(0xFFF59E0B)),
    URGENT("Urgent", R.string.priority_urgent, 4, Color(0xFFEF4444));

    val titleRes: Int get() = labelRes
}

enum class Category(val title: String, @StringRes val labelRes: Int, val color: Color) {
    PERSONAL("Personal", R.string.category_personal, Color(0xFF8B5CF6)),
    STUDY("Study", R.string.category_study, Color(0xFF14B8A6)),
    WORK("Work", R.string.category_work, Color(0xFF0284C7)),
    BILLS("Bills", R.string.category_bills, Color(0xFFF59E0B)),
    SHOPPING("Shopping", R.string.category_shopping, Color(0xFFEC4899)),
    FAMILY("Family", R.string.category_family, Color(0xFFE11D48)),
    HEALTH("Health", R.string.category_health, Color(0xFF10B981)),
    OTHER("Other", R.string.category_other, Color(0xFF6366F1));

    val titleRes: Int get() = labelRes

    fun getIcon(): ImageVector {
        return when (this) {
            PERSONAL -> Icons.Default.Person
            STUDY -> Icons.Default.School
            WORK -> Icons.Default.Work
            BILLS -> Icons.Default.AttachMoney
            SHOPPING -> Icons.Default.ShoppingCart
            FAMILY -> Icons.Default.FamilyRestroom
            HEALTH -> Icons.Default.FitnessCenter
            OTHER -> Icons.Default.Label
        }
    }
}

enum class Recurrence(val title: String, @StringRes val labelRes: Int) {
    NONE("Once", R.string.repeat_once),
    DAILY("Daily", R.string.repeat_daily),
    WEEKDAYS("Mon - Fri", R.string.repeat_weekdays),
    WEEKLY("Weekly", R.string.repeat_weekly),
    MONTHLY("Monthly", R.string.repeat_monthly),
    YEARLY("Yearly", R.string.repeat_yearly);

    val titleRes: Int get() = labelRes
}

@Entity(tableName = "reminders")
data class ReminderItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val dueTimestamp: Long,
    val isCompleted: Boolean = false,
    val priority: Priority = Priority.MEDIUM,
    val category: Category = Category.PERSONAL,
    val recurrence: Recurrence = Recurrence.NONE,
    val notificationEnabled: Boolean = true,
    val createdTimestamp: Long = System.currentTimeMillis(),
    val memberId: Long? = null,
    val memberName: String? = null
)

data class SmartParseResult(
    val cleanTitle: String,
    val dueTimestamp: Long?,
    val category: Category?,
    val priority: Priority?,
    val recurrence: Recurrence?
)
