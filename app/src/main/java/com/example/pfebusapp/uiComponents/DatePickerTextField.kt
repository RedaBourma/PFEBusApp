package com.example.pfebusapp.uiComponents

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DatePickerTextField(
    modifier: Modifier = Modifier,
    label: String,
    initialDate: Date,
    onDateSelected: (Date) -> Unit,
    enabled: Boolean = true
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    calendar.time = initialDate
    
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val formattedDate = dateFormat.format(initialDate)
    
    OutlinedTextField(
        value = formattedDate,
        onValueChange = { /* Handled by date picker */ },
        label = { Text(label) },
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) {
                DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        calendar.set(Calendar.YEAR, year)
                        calendar.set(Calendar.MONTH, month)
                        calendar.set(Calendar.DAY_OF_MONTH, day)
                        onDateSelected(calendar.time)
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            },
        readOnly = true,
        enabled = enabled,
        trailingIcon = {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = "Select date"
            )
        }
    )
} 