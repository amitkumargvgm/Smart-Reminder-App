package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.model.Category
import com.example.model.Priority
import com.example.model.ReminderItem
import com.example.ui.ReminderCard
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun reminder_card_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        ReminderCard(
          reminder = ReminderItem(
            id = 1,
            title = "Doctor Appointment",
            description = "Bring medical reports",
            dueTimestamp = System.currentTimeMillis() + 3600000,
            priority = Priority.HIGH,
            category = Category.HEALTH
          ),
          onToggleComplete = {},
          onEdit = {},
          onDelete = {},
          onSnooze = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
