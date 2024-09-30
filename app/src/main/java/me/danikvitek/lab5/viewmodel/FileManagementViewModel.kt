package me.danikvitek.lab5.viewmodel

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Environment
import android.util.Log
import androidx.annotation.FloatRange
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
import me.danikvitek.lab5.R
import me.danikvitek.lab5.service.ChemEngineService
import java.io.File
import java.io.FileNotFoundException
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

sealed interface State {
    data object NoFile : State
    data object FileReady : State
    data class Downloading(@FloatRange(from = 0.0, to = 1.0) val progress: Float?) : State
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
                    _state.value =
                        file().takeIf { it.exists() }?.let { State.FileReady } ?: State.NoFile
                }
                delay(RETRY_RATE)
            }
        }
    }

    /**
     * @return the file path of the file or null if the external storage is not available
     */
    fun file(): File =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            .resolve(appContext.packageName)
            .resolve(FILE_NAME)

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
                _state.value =
                    State.Error(appContext.getString(R.string.error_no_internet_connection))
                return@launch
            }

            _state.value = State.Downloading(null)

            val response = chemEngineService.downloadPdf()
            Log.d(TAG, "Status: ${response.code()}")
            if (!response.isSuccessful) {
                _state.value =
                    State.Error("${appContext.getString(R.string.error_server_response_error)}: ${response.code()}")
                return@launch
            }

            val body = response.body() ?: run {
                _state.value =
                    State.Error(appContext.getString(R.string.error_no_server_response_body))
                return@launch
            }
            val contentLength = body.contentLength()
            file().run {
                try {
                    parentFile?.takeIf { !it.isDirectory }?.mkdirs()
                    outputStream()
                } catch (e: FileNotFoundException) {
                    _state.value = State.Error(appContext.getString(R.string.error_creating_file))
                    Log.e(TAG, "Error creating file", e)
                    return@launch
                } catch (e: SecurityException) {
                    _state.value =
                        State.Error(appContext.getString(R.string.error_no_rights_to_create_file))
                    Log.e(TAG, "Error creating file", e)
                    return@launch
                }
            }.use { outputStream ->
                body.byteStream().use { inputStream ->
                    val buffer = ByteArray(4096)
                    var total = 0L
                    while (true) {
                        val len = inputStream.read(buffer)
                        if (len == -1) break

                        outputStream.write(buffer, 0, len)

                        if (contentLength > -1) {
                            total += len
                            _state.value = State.Downloading(
                                if (contentLength == 0L) 1f else (total.toFloat() / contentLength)
                            )
                        }
                    }
                }
            }

            _state.value = State.FileReady
        }
    }

    fun deleteFile() {
        file().takeIf { it.isFile }?.delete()
        _state.value = State.NoFile
    }

    companion object {
        private const val TAG = "FileManagementViewModel"
        private const val FILE_NAME = "file.pdf"

        private val RETRY_RATE = 1.seconds
    }
}
