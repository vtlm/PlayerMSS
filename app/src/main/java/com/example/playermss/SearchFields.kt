package com.example.playermss

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import com.example.playermss.data.MediaViewModel
import com.example.playermss.data.TextFieldViewModel

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun SearchField(textValue: TextFieldViewModel, onDone:  () -> Unit? = {}){
    val text = textValue.textField.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    OutlinedTextField(
        value = text.value,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        onValueChange = {str -> textValue.setText(str)},
        label = { Text(textValue.description) },
        isError = false,
        keyboardOptions = KeyboardOptions.Default.copy(
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                keyboardController?.hide()
                onDone()
            }
        ))
}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun SearchFields(searchFields: List<TextFieldViewModel>, mediaViewModel: MediaViewModel){
    LazyColumn {
        searchFields.forEach {
            item {
                SearchField(it) { mediaViewModel.query(searchFields) }
            }
        }
    }
}

