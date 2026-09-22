package com.example.data

import com.example.model.MemberItem
import com.example.model.ReminderItem
import kotlinx.coroutines.flow.Flow

class ReminderRepository(
    private val reminderDao: ReminderDao,
    private val memberDao: MemberDao
) {

    val allReminders: Flow<List<ReminderItem>> = reminderDao.getAllReminders()
    val allMembers: Flow<List<MemberItem>> = memberDao.getAllMembers()

    suspend fun getReminderById(id: Long): ReminderItem? = reminderDao.getReminderById(id)

    suspend fun getUpcomingActiveReminders(currentTime: Long): List<ReminderItem> =
        reminderDao.getUpcomingActiveReminders(currentTime)

    suspend fun insertReminder(reminder: ReminderItem): Long = reminderDao.insertReminder(reminder)

    suspend fun updateReminder(reminder: ReminderItem) = reminderDao.updateReminder(reminder)

    suspend fun deleteReminder(reminder: ReminderItem) = reminderDao.deleteReminder(reminder)

    suspend fun deleteById(id: Long) = reminderDao.deleteById(id)

    suspend fun setCompleted(id: Long, completed: Boolean) = reminderDao.setCompleted(id, completed)

    suspend fun snoozeReminder(id: Long, newDueTimestamp: Long) = reminderDao.snoozeReminder(id, newDueTimestamp)

    // Member Operations
    suspend fun insertMember(member: MemberItem): Long = memberDao.insertMember(member)

    suspend fun updateMember(member: MemberItem) = memberDao.updateMember(member)

    suspend fun deleteMember(member: MemberItem) = memberDao.deleteMember(member)
}
