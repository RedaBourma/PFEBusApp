package com.example.pfebusapp.uiComponents

import android.app.DatePickerDialog
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import java.util.Calendar
import java.util.Date

@Composable
fun DatePickerTextField(
    modifier: Modifier,
    label: String,
    onDateSelected: (Date) -> Unit
) {
    val context = LocalContext.current
    var date by remember { mutableStateOf("") }
    val calendar = Calendar.getInstance()

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val selectedDate = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)
            date = selectedDate
            val d = Date(year, month+1, dayOfMonth)
            onDateSelected(d)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    OutlinedTextField(
        singleLine = true,
        value = date,
        onValueChange = { /* Prevent manual changes */ },
        label = { Text(label) },
        readOnly = true, // Prevent manual input
        trailingIcon = {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = "Select Date",
                modifier = Modifier.clickable { datePickerDialog.show() }
            )
        },
        modifier = Modifier
            .clickable { datePickerDialog.show() } // Open picker on tap
    )
}