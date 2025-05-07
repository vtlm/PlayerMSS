package com.example.playermss

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import com.example.playermss.data.QueryTextField

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun SearchField(textValue: QueryTextField, onDone:  () -> Unit? = {}){
    val text = textValue.text.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val textField = FocusRequester()

    OutlinedTextField(
        value = text.value,
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
            .focusRequester(textField),
        onValueChange = {str -> textValue.setText(str)},
        label = { Text(textValue.description) },
        trailingIcon = {
            if (text.value != "") {
                IconButton(onClick = {
                    textValue.setText("")
                    textField.requestFocus()
                }) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "Clear"
                    )
                }
            }
        },
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
fun SearchFields(searchFields: Array<QueryTextField>, query: () -> Unit? ){
    LazyColumn {
        searchFields.forEachIndexed { ind, it ->
            item (key = ind){
                SearchField(it) {
                    query()
                }
            }
        }
    }
}

