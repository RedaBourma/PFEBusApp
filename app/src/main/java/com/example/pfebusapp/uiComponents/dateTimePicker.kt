package com.example.pfebusapp.uiComponents

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.Calendar
import java.util.Date

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerTextField(
    modifier: Modifier = Modifier,
    label: String,
    initialDate: Date? = null,
    onDateSelected: (Date) -> Unit,
    enabled: Boolean = true
) {
    var showDatePicker by remember { mutableStateOf(false) }
    
    // Format initial date if provided
    var selectedDate by remember { 
        val initialDateStr = if (initialDate != null) {
            val cal = Calendar.getInstance()
            cal.time = initialDate
            String.format(
                "%02d/%02d/%04d",
                cal.get(Calendar.DAY_OF_MONTH),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.YEAR)
            )
        } else {
            ""
        }
        mutableStateOf(initialDateStr)
    }
    
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate?.time ?: calendar.timeInMillis
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val cal = Calendar.getInstance()
                            cal.timeInMillis = millis
                            // Format in French date style: DD/MM/YYYY
                            selectedDate = String.format(
                                "%02d/%02d/%04d",
                                cal.get(Calendar.DAY_OF_MONTH),
                                cal.get(Calendar.MONTH) + 1,
                                cal.get(Calendar.YEAR)
                            )
                            onDateSelected(cal.time)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Annuler")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    OutlinedTextField(
        singleLine = true,
        value = selectedDate,
        onValueChange = { /* Prevent manual changes */ },
        label = { Text(label, style = MaterialTheme.typography.bodyMedium) },
        readOnly = true,
        enabled = enabled,
        trailingIcon = {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = "Sélectionner une date",
                modifier = Modifier.clickable(enabled = enabled) { if (enabled) showDatePicker = true },
                tint = MaterialTheme.colorScheme.primary
            )
        },
        modifier = modifier
            .clickable(enabled = enabled) { if (enabled) showDatePicker = true },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedLabelColor = MaterialTheme.colorScheme.primary
        ),
        shape = RoundedCornerShape(12.dp),
        textStyle = MaterialTheme.typography.bodyLarge
    )
}