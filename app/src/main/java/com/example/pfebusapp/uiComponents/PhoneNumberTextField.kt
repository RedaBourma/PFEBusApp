package com.example.pfebusapp.uiComponents

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
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

@Composable
fun PhoneNumberTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Phone Number",
    isError: Boolean = false,
    errorMessage: String = "",
) {
    var textValue by remember { mutableStateOf(TextFieldValue(text = value)) }

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
        label = { Text(label)},
        modifier = Modifier,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        isError = isError,
        supportingText = if(isError) {{ Text(errorMessage)}} else null,
        singleLine = true
    )
}

private fun formatPhoneNumber(digits: String) : String {
    return when {
        digits.length <= 2 -> digits
        digits.length <= 4 -> "${digits.substring(0, 2)} ${digits.substring(2)}"
        digits.length <= 6 -> "${digits.substring(0, 2)} ${digits.substring(2, 4)} ${digits.substring(4)}"
        digits.length <= 8 -> "${digits.substring(0, 2)} ${digits.substring(2, 4)} ${digits.substring(4, 6)} ${digits.substring(6)}"
        else -> "${digits.substring(0, 2)} ${digits.substring(2, 4)} ${digits.substring(4, 6)} ${digits.substring(6, 8)} ${digits.substring(8)}"
    }
}

@Preview
@Composable
private fun PhoneNumberTextFieldPreview() {
    var phoneNumber by remember { mutableStateOf("") }
   PhoneNumberTextField(
       value = phoneNumber,
       onValueChange = {
           phoneNumber = it
       }
   )
}