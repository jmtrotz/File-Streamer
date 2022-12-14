package com.jmtrotz.filestreamer

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast

import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel

import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

private const val TAG = "MainScreen"

/**
 * Main Screen for this application.
 * 
 * @param viewModel ViewModel for this screen.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalPermissionsApi::class)
@ExperimentalMaterial3Api
@Composable
fun MainScreen(viewModel: MainViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarState = remember { SnackbarHostState() }

    // Launches the file chooser
    val getContent = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            // Copy the file to a location that is accessible to this application (scoped storage)
            val contentResolver = context.contentResolver
            val parcelFileDescriptor = contentResolver.openFileDescriptor(uri, "r", null)
            val inputStream = FileInputStream(parcelFileDescriptor?.fileDescriptor)
            val file = File(context.cacheDir, contentResolver.getFilename(uri))
            val outputStream = FileOutputStream(file)

            Log.d(TAG, "Copying file")
            Util.copyFile(
                inputStream = inputStream,
                outputStream = outputStream,
                onCopyStarted = {
                    viewModel.isProgressVisible = true
                },
                onCopyComplete = {
                    viewModel.isProgressVisible = false
                    viewModel.selectedFilePath = file.path
                    parcelFileDescriptor?.close()
                }
            )
        }
    }

    // Permissions state for Android 13 and up
    val permissionsStateApi33 = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.INTERNET
        )
    ) { results ->
        onPermissionsResult(viewModel, results, getContent)
    }

    // Permissions state for Android 12 and below
    val permissionsState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.INTERNET
        )
    ) { results ->
        onPermissionsResult(viewModel, results, getContent)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(text = stringResource(id = R.string.app_name)) }) },
        snackbarHost = { SnackbarHost(hostState = snackbarState) }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
            color = MaterialTheme.colorScheme.background
        ) {
            InputForm(
                context = context,
                viewModel = viewModel,
                coroutineScope = coroutineScope,
                snackbarState = snackbarState,
                activityResultLauncher = getContent,
                permissionsStateApi33 = permissionsStateApi33,
                permissionsState = permissionsState
            )
        }
    }
}

/**
 * User input form for this screen.
 *
 * @param viewModel ViewModel for this Screen.
 * @param coroutineScope Coroutine scope for the Snackbar.
 * @param snackbarState State for the Snackbar.
 * @param activityResultLauncher Activity to launch when the top TextField is pressed.
 * @param permissionsState Permissions state for Android 12 and below.
 * @param permissionsStateApi33 Permissions state for Android 13 and up.
 */
@OptIn(ExperimentalPermissionsApi::class)
@ExperimentalMaterial3Api
@Composable
private fun InputForm(
    context: Context,
    viewModel: MainViewModel,
    coroutineScope: CoroutineScope,
    snackbarState: SnackbarHostState,
    activityResultLauncher: ManagedActivityResultLauncher<String, Uri?>,
    permissionsState: MultiplePermissionsState,
    permissionsStateApi33: MultiplePermissionsState
) {
    Column(
        modifier = Modifier.fillMaxWidth(0.8f),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ToastAndDialogs(
            context = context,
            viewModel = viewModel
        )

        TextFields(
            viewModel = viewModel,
            activityResultLauncher = activityResultLauncher,
            permissionsStateApi33 = permissionsStateApi33,
            permissionsState = permissionsState
        )

        Buttons(
            viewModel = viewModel,
            coroutineScope = coroutineScope,
            snackbarState = snackbarState,
            permissionsStateApi33 = permissionsStateApi33,
            permissionsState = permissionsState
        )
    }
}

/**
 * Contains the Toast message that is shown when the FileSteamer has an update (i.e. when
 * a connection is made/lost, stream started/stopped, etc). Also contains the progress
 * dialog that is shown when a file stream is started and the permissions rationale dialog.
 * 
 * @param context Android application Context.
 * @param viewModel ViewModel for this screen.
 */
@ExperimentalMaterial3Api
@Composable
private fun ToastAndDialogs(context: Context, viewModel: MainViewModel) {
    // TODO: Seems hacky... Find a better solution?
    // Toast message that is shown when the FileStreamer has an update to share
    if (viewModel.toastMessage != FileStreamer.message) {
        Log.d(TAG, "Showing toast")
        Toast.makeText(context, FileStreamer.message, Toast.LENGTH_SHORT).show()
        FileStreamer.message = ""
    }

    // Progress dialog that is shown when a file is being copied or prepared for streaming
    if (viewModel.isProgressVisible || FileStreamer.isPreparingFile) {
        Log.d(TAG, "Showing progress dialog")
        AlertDialog(
            onDismissRequest = { Log.d(TAG, "Dismissing progress dialog") },
            title = {
                if (FileStreamer.isPreparingFile) {
                    Text(text = stringResource(id = R.string.prepare_file))
                } else {
                    Text(text = stringResource(id = R.string.copy_file))
                }
            },
            text = { CircularProgressIndicator() },
            confirmButton = {  },
            dismissButton = {  },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        )
    }

    // TODO: Fix progress indicator not centered
    // Progress dialog that is shown when a file is streaming
    if (FileStreamer.isStreaming) {
        Log.d(TAG, "Showing stream progress dialog")
        AlertDialog(
            onDismissRequest = { Log.d(TAG, "Dismissing stream progress dialog") },
            title = { Text(text = stringResource(id = R.string.dialog_title_steaming_file)) },
            text = { CircularProgressIndicator() },
            confirmButton = {  },
            dismissButton = {
                Button(onClick = {
                    Log.d(TAG, "Stopping stream")
                    FileStreamer.stopStream()
                    Log.d(TAG, "Closing progress dialog")
                }
            ) {
                Text(text = stringResource(id = R.string.btn_stop)) }
            },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        )
    }

    // TODO: Fix progress indicator not centered
    // Rationale dialog that is shown if a permission is not granted
    if (viewModel.isRationaleVisible) {
        Log.d(TAG, "Showing permission rationale")
        AlertDialog(
            onDismissRequest = { Log.d(TAG, "Dismissing permission rationale") },
            title = { Text(text = stringResource(id = R.string.permission_required)) },
            text = { Text(text = stringResource(id = R.string.permission_rationale)) },
            confirmButton = {
                Button(onClick = {
                    Log.d(TAG, "Closing permission rationale & opening settings")
                    viewModel.isRationaleVisible = false
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        )
                    )
                }) {
                    Text(text = stringResource(id = R.string.btn_ok))
                }
            },
            dismissButton = {
                Button(onClick = {
                    Log.d(TAG, "Closing permission rationale")
                    viewModel.isRationaleVisible = false
                }) {
                    Text(text = stringResource(id = R.string.btn_cancel))
                }
            },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        )
    }
}

/**
 * TextFields for the input form. The TextField at the top of the form opens the file
 * chooser if all permissions have been granted. The TextField in the middle of the
 * form is used to enter the destination URL.
 *
 * @param viewModel ViewModel for this screen
 * @param activityResultLauncher Activity to launch when the top TextField is pressed.
 * @param permissionsStateApi33 Permissions state for Android 13 and up.
 * @param permissionsState Permissions state for Android 12 and below.
 */
@OptIn(ExperimentalPermissionsApi::class)
@ExperimentalMaterial3Api
@Composable
private fun TextFields(
    viewModel: MainViewModel,
    activityResultLauncher: ManagedActivityResultLauncher<String, Uri?>,
    permissionsStateApi33: MultiplePermissionsState,
    permissionsState: MultiplePermissionsState
) {
   /* 
    * TextField at the top of the form that opens the file chooser.
    * NOTE: There is no error state for this TextField because it doesn't work
    * when the TextField is disabled. It had to be disabled in order for
    * Modifier.clickable{} to work (known bug according to my research)
    */
    OutlinedTextField(
        value = viewModel.selectedFilePath,
        onValueChange = { filePath: String ->
            viewModel.selectedFilePath = filePath
        },
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .clickable {
                Util.isPermissionGranted(
                    permissionsState = permissionsState,
                    permissionsStateApi33 = permissionsStateApi33,
                    onAllPermissionsGranted = {
                        Log.d(TAG, "Launching file chooser")
                        activityResultLauncher.launch("*/*")
                    }
                ) { isApi33 ->
                    if (isApi33) {
                        Log.d(TAG, "Requesting permissions for API 33+")
                        permissionsStateApi33.launchMultiplePermissionRequest()
                    } else {
                        Log.d(TAG, "Requesting permissions")
                        permissionsState.launchMultiplePermissionRequest()
                    }
                }
            },
        enabled = false,
        label = { Text(text = stringResource(R.string.hint_select_file)) },
        singleLine = true,
        maxLines = 1,
        shape = RoundedCornerShape(16.dp)
    )

    // TextField in the middle of the form where the user enters the destination URL
    OutlinedTextField(
        value = viewModel.destinationUrl,
        onValueChange = { url: String ->
            viewModel.isUrlError = false
            viewModel.destinationUrl = url
        },
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .padding(top = 8.dp, bottom = 8.dp),
        label = {
            Text(text = stringResource(R.string.hint_enter_url))
        },
        isError = viewModel.isUrlError,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrect = false,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.None
        ),
        keyboardActions = KeyboardActions.Default,
        singleLine = true,
        maxLines = 1,
        shape = RoundedCornerShape(16.dp)
    )
}

/**
 * Buttons at the bottom of the form below the TextFields. These
 * buttons are used to clear the form or to start the file stream
 * if the user's input is valid.
 *
 * @param viewModel ViewModel for this screen
 * @param coroutineScope Coroutine scope for the Snackbar.
 * @param snackbarState Snackbar host state.
 * @param permissionsStateApi33 Permissions state for Android 13 and up.
 * @param permissionsState Permissions state for Android 12 and below.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun Buttons(
    viewModel: MainViewModel,
    coroutineScope: CoroutineScope,
    snackbarState: SnackbarHostState,
    permissionsStateApi33: MultiplePermissionsState,
    permissionsState: MultiplePermissionsState
) {
    Row (
        modifier = Modifier.fillMaxWidth(0.8f),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Button to clear the input form
        Button(onClick = {
            Log.d(TAG, "Clearing input")
            viewModel.selectedFilePath = ""
            viewModel.destinationUrl = ""
            viewModel.isUrlError = false
        }) {
            Text(text = stringResource(id = R.string.btn_clear))
        }

        // Button to start the file stream
        Button(onClick = {
            Util.isInputValid(
                filePath = viewModel.selectedFilePath,
                url = viewModel.destinationUrl,
                onValidationSuccess = {
                    Util.isPermissionGranted(
                        permissionsState = permissionsState,
                        permissionsStateApi33 = permissionsStateApi33,
                        onAllPermissionsGranted = {
                            Log.d(TAG, "Starting file stream")
                            FileStreamer.startStream(viewModel.selectedFilePath,
                                viewModel.destinationUrl)
                        },
                        onAllPermissionsNotGranted = {
                            viewModel.isRationaleVisible = true
                        }
                    )
                },
                onValidationFailed = { errorMessage, isUrlError ->
                    Log.e(TAG, "Input was not valid. Showing Snackbar")
                    viewModel.isUrlError = isUrlError
                    coroutineScope.launch {
                        snackbarState.showSnackbar(errorMessage)
                    }
                }
            )
        }) {
            Text(text = stringResource(id = R.string.btn_stream))
        }
    }
}

/**
 * Verifies that all permissions were granted. If all permissions were granted,
 * the file chooser is opened. If all permissions were not granted, the rationale
 * dialog is shown.
 *
 * @param viewModel ViewModel for this screen.
 * @param results Map containing the names of permissions that were requested
 * and a Boolean indicating if that permission was granted or not.
 * @param activityResultLauncher Activity to launch if all permissions were granted.
 */
private fun onPermissionsResult(
    viewModel: MainViewModel,
    results: Map<String, Boolean>,
    activityResultLauncher: ManagedActivityResultLauncher<String, Uri?>
) {
    val isGranted: (Boolean) -> Boolean = { it } // Fancy way of saying "true"...
    if (results.values.all(isGranted)) {
        Log.d(TAG, "All permissions granted. Opening file chooser")
        activityResultLauncher.launch("media/*")
    } else {
        Log.d(TAG, "One or more permission was not granted. Showing permission rationale")
        viewModel.isRationaleVisible = true
    }
}