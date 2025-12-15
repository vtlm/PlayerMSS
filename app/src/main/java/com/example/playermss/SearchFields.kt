package com.example.playermss

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import com.example.playermss.data.QueryTextField

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun SearchField(queryTextField: QueryTextField, onDone:  () -> Unit? = {}){
    var expanded by remember { mutableStateOf(false) }

    val text = queryTextField.text.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val textField = FocusRequester()

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = text.value,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(textField)
                .menuAnchor(MenuAnchorType.PrimaryEditable)
                //Set this modifier to dismiss the menu when the text field is not in focus,
                //e.g. moving to another text field by using keyboard actions.
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused) {
                            expanded = false
                    }
                },

            onValueChange = { str ->
                queryTextField.setText(str)
                expanded = queryTextField.userInputDictIsNotEmpty()
            },
            label = { Text(queryTextField.description) },
            trailingIcon = {
                if (text.value != "") {
                    IconButton(onClick = {
                        queryTextField.setText("")
                        textField.requestFocus()
                        if (queryTextField.userInputDictIsNotEmpty()) {
                            expanded = true
                        }
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
                    queryTextField.updateUserInputDict()
                    onDone()
                }
            ))

        if(queryTextField.userInputDictFilteredByCurrentText().isNotEmpty()) {

            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                queryTextField.userInputDictFilteredByCurrentText().forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option, style = MaterialTheme.typography.bodyLarge) },
                        onClick = {
                            queryTextField.setText(option)
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }
    }
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

