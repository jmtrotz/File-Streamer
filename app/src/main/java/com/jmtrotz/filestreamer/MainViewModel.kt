package com.jmtrotz.filestreamer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.ViewModel

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel for the Main Screen.
 */
@HiltViewModel
class MainViewModel @Inject constructor(): ViewModel(), DefaultLifecycleObserver {
    var selectedFilePath by mutableStateOf("")
    var destinationUrl by mutableStateOf("")
    var toastMessage by mutableStateOf("")
    var isProgressVisible by mutableStateOf(false)
    var isRationaleVisible by mutableStateOf(false)
    var isUrlError by mutableStateOf(false)
}