package com.example.pfebusapp.uiComponents

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun PhoneNumberTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Numéro de téléphone",
    isError: Boolean = false,
    errorMessage: String = "",
    enabled: Boolean = true
) {
    val initialFormattedValue = remember(value) { formatPhoneNumber(value.filter { it.isDigit() }) }
    var textValue by remember(value) { mutableStateOf(TextFieldValue(text = initialFormattedValue)) }

    OutlinedTextField(
        value = textValue,
        onValueChange = { newVal ->
            val digitsOnly = newVal.text.filter { it.isDigit() }
            val formattedNumber = formatPhoneNumber(digitsOnly)

            if(digitsOnly.length <= 10){
                textValue = newVal.copy(text = formattedNumber)
                onValueChange(digitsOnly)
            }
        },
        label = { Text(label, style = MaterialTheme.typography.bodyMedium) },
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        isError = isError,
        supportingText = if(isError) { { 
            Text(
                errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            ) 
        }} else null,
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            errorBorderColor = MaterialTheme.colorScheme.error,
            errorLabelColor = MaterialTheme.colorScheme.error
        ),
        shape = RoundedCornerShape(12.dp),
        textStyle = MaterialTheme.typography.bodyLarge,
        enabled = enabled
    )
}

private fun formatPhoneNumber(digits: String): String {
    return when {
        digits.length <= 2 -> digits
        digits.length <= 5 -> "${digits.substring(0, 2)} ${digits.substring(2)}"
        digits.length <= 8 -> "${digits.substring(0, 2)} ${digits.substring(2, 5)} ${digits.substring(5)}"
        else -> "${digits.substring(0, 2)} ${digits.substring(2, 5)} ${digits.substring(5, 8)} ${digits.substring(8)}"
    }
}

@Preview(showBackground = true)
@Composable
fun PhoneNumberTextFieldPreview() {
    PhoneNumberTextField(
        value = "0612345678",
        onValueChange = {}
    )
}