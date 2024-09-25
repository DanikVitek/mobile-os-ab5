package me.danikvitek.lab5.viewmodel

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.provider.DocumentsContract
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.danikvitek.lab5.service.ChemEngineService
import java.io.BufferedInputStream
import java.io.File
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

sealed interface State {
    data object NoFile : State
    data object FileReady : State
    data class Downloading(val progress: Float?) : State
    data class Error(val error: String) : State
}

@HiltViewModel
class FileManagementViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val chemEngineService: ChemEngineService,
) : ViewModel() {

    private val _state = MutableStateFlow<State>(State.NoFile)
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                if (_state.value !is State.Downloading) {
                    _state.value = file()?.let { State.FileReady } ?: State.NoFile
                }
                delay(RETRY_RATE)
            }
        }
    }

    fun openFile(originActivity: ComponentActivity) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            putExtra(
                DocumentsContract.EXTRA_INITIAL_URI,
                appContext.getFileStreamPath(FILE_NAME).toURI(),
            )
        }
        startActivityForResult(originActivity, intent, 1, null)
    }

    private fun file(): File? =
        appContext.getFileStreamPath(FILE_NAME).takeIf { it.exists() }.also {
            if (it == null) {
                _state.value = State.NoFile
            }
        }

    private fun isOnline(): Boolean {
        val connectivityManager =
            appContext.getSystemService(ConnectivityManager::class.java)
            ?: return false

        val capabilities =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            ?: return false

        val hasCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        Log.d(TAG, "hasCellular: $hasCellular")
        val hasWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        Log.d(TAG, "hasWifi: $hasWifi")
        val hasEthernet = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        Log.d(TAG, "hasEthernet: $hasEthernet")

        return (hasCellular || hasWifi || hasEthernet).also { Log.d(TAG, "=> isOnline: $it") }
    }

    fun downloadFile() {
        viewModelScope.launch(Dispatchers.IO) {
            if (!isOnline()) {
                _state.value = State.Error("No internet connection")
                return@launch
            }

            _state.value = State.Downloading(null)

            val response = chemEngineService.downloadPdf()
            if (!response.isSuccessful) {
                _state.value = State.Error("Error downloading file")
                return@launch
            }

            val body = response.body() ?: run {
                _state.value = State.Error("Error downloading file")
                return@launch
            }
            val contentLength = body.contentLength()
            appContext.openFileOutput(FILE_NAME, Context.MODE_PRIVATE).use { outputStream ->
                BufferedInputStream(body.byteStream()).use { inputStream ->
                    val buffer = ByteArray(1024)
                    var len: Int
                    var total = 0L
                    while (true) {
                        len = inputStream.read(buffer)
                        if (len == -1) break
                        outputStream.write(buffer, 0, len)
                        if (contentLength > -1) {
                            total += len
                            _state.value = State.Downloading(total.toFloat() / contentLength)
                        }
                    }
                }
            }

            _state.value = State.FileReady
        }
    }

    fun deleteFile() {
        appContext.deleteFile(FILE_NAME)
        _state.value = State.NoFile
    }

    companion object {
        private const val TAG = "FileManagementViewModel"
        private const val FILE_NAME = "file.pdf"

        private val RETRY_RATE = 1.seconds
    }
}
