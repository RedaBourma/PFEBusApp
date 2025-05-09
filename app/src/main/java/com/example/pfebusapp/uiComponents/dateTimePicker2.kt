package com.example.pfebusapp.uiComponents

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun DatePickerWithDateSelectableDatesSample() {
    val datePickerState =
        rememberDatePickerState(
            selectableDates =
            object : SelectableDates {
                // Blocks Sunday and Saturday from being selected.
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val dayOfWeek =
                            Instant.ofEpochMilli(utcTimeMillis)
                                .atZone(ZoneId.of("UTC"))
                                .toLocalDate()
                                .dayOfWeek
                        dayOfWeek != DayOfWeek.SUNDAY && dayOfWeek != DayOfWeek.SATURDAY
                    } else {
                        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                        calendar.timeInMillis = utcTimeMillis
                        calendar[Calendar.DAY_OF_WEEK] != Calendar.SUNDAY &&
                                calendar[Calendar.DAY_OF_WEEK] != Calendar.SATURDAY
                    }
                }

                // Allow selecting dates from year 2023 forward.
                override fun isSelectableYear(year: Int): Boolean {
                    return year > 2022
                }
            }
        )

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DatePicker(state = datePickerState)
        Text(
            "Selected date timestamp: ${datePickerState.selectedDateMillis ?: "no selection"}",
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}