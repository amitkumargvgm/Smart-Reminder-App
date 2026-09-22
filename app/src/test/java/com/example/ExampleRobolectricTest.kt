package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.Category
import com.example.model.Priority
import com.example.parser.SmartReminderParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Smart Reminder", appName)
  }

  @Test
  fun `test smart reminder parser`() {
    val result = SmartReminderParser.parse("Doctor appointment tomorrow at 10am urgent")
    assertEquals(Category.HEALTH, result.category)
    assertEquals(Priority.URGENT, result.priority)
    assertNotNull(result.dueTimestamp)
  }
}
