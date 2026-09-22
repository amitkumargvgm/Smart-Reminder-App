package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "members")
data class MemberItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val relationship: String = "Family", // Family, Friend, Colleague, Doctor, Client, Self, Other
    val phone: String = "",
    val notes: String = "",
    val colorHex: Long = 0xFF3B82F6,
    val createdTimestamp: Long = System.currentTimeMillis()
)
