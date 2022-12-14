package com.jmtrotz.filestreamer

import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Log

import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Various utilities.
 */
object Util {
    private const val TAG = "Util"

    /**
     * Checks if all permissions have been granted. If so, onAllPermissionsGranted()
     * is invoked, else onAllPermissionNotGranted() is invoked.
     *
     * @param permissionsState Permissions state for Android 12 and below.
     * @param permissionsStateApi33 Permissions state for Android 13 and up.
     * @param onAllPermissionsGranted Callback that is invoked if all permissions
     * have been granted.
     * @param onAllPermissionNotGranted Callback that is invoked if all permissions
     * have been not been granted.
     */
    @OptIn(ExperimentalPermissionsApi::class)
    fun isPermissionGranted(
        permissionsStateApi33: MultiplePermissionsState,
        permissionsState: MultiplePermissionsState,
        onAllPermissionsGranted: () -> Unit,
        onAllPermissionsNotGranted: (isApi33: Boolean) -> Unit
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Log.d(TAG, "Checking permissions for API 33+")
            if (permissionsStateApi33.allPermissionsGranted) {
                Log.d(TAG, "Invoking onAllPermissionsGranted()")
                onAllPermissionsGranted.invoke()
            } else {
                Log.d(TAG, "Invoking onAllPermissionsNotGranted()")
                onAllPermissionsNotGranted.invoke(true)
            }
        } else {
            Log.d(TAG, "Checking permissions")
            if (permissionsState.allPermissionsGranted) {
                Log.d(TAG, "Invoking onAllPermissionsGranted()")
                onAllPermissionsGranted.invoke()
            } else {
                Log.d(TAG, "Invoking onAllPermissionsNotGranted()")
                onAllPermissionsNotGranted.invoke(false)
            }
        }
    }

    /**
     * Verifies that the user's input is valid. If so, onValidationSuccess() is invoked,
     * else onValidationSuccess() is invoked.
     *
     * @param filePath File path to be verified.
     * @param url To be verified.
     * @param onValidationSuccess Callback that is invoked if the input is valid.
     * @param onValidationSuccess Callback that is invoked if the input is not valid.
     */
    fun isInputValid(
        filePath: String,
        url: String,
        onValidationSuccess: () -> Unit,
        onValidationFailed: (errorMessage: String, isUrlError: Boolean) -> Unit
    ) {
        Log.d(TAG, "Validating input")
        if (filePath.isBlank() && url.isBlank()) {
            Log.e(TAG, "Selected file path and destination URL are both blank. Invoking onValidationFailed()")
            onValidationFailed.invoke("Please select a file and enter a destination URL", true)
        } else if (filePath.isBlank()) {
            Log.e(TAG, "Selected file path is blank. Invoking onValidationFailed()")
            onValidationFailed.invoke("Please select a file", false)
        } else if (url.isBlank()) {
            Log.e(TAG, "Destination URL is blank. Invoking onValidationFailed()")
            onValidationFailed.invoke("Please enter a destination URL", true)
        } else {
            Log.d(TAG, "Invoking onValidationSuccess()")
            onValidationSuccess.invoke()
        }
    }

    /**
     * Copies a file from the given input stream to the given output stream.
     *
     * @param inputStream FileInputStream to read the file from.
     * @param outputStream FileOutputStream to write the file to.
     * @param onCopyStarted Callback that is invoked when the copy has started.
     * @param onCopyComplete Callback that is invoked when the copy is complete.
     */
    fun copyFile(
        inputStream: FileInputStream,
        outputStream: FileOutputStream,
        onCopyStarted: () -> Unit,
        onCopyComplete: () -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.IO) {
                Log.d(TAG, "File copy started")
                onCopyStarted.invoke()
                val buffer = ByteArray(1024)
                var length = inputStream.read(buffer)

                while (length > 0) {
                    outputStream.write(buffer, 0, length)
                    length = inputStream.read(buffer)
                }

                Log.d(TAG, "File copy complete")
                inputStream.close()
                outputStream.close()
                onCopyComplete.invoke()
            }
        }
    }
}

/**
 * Extension function for the ContentResolver class to get the filename from a Uri.
 * @param uri Uri that will be used to find the name of the file.
 * @return The name of the file associated with the given Uri as a String.
 */
fun ContentResolver.getFilename(uri: Uri): String {
    var filename = ""
    val cursor = this.query(uri, null, null, null, null)

    if (cursor != null) {
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        cursor.moveToFirst()
        filename = cursor.getString(nameIndex)
        cursor.close()
    }

    return filename
}