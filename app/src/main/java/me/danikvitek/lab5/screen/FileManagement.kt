package me.danikvitek.lab5.screen

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.FileProvider
import me.danikvitek.lab5.ui.theme.Lab5Theme
import me.danikvitek.lab5.viewmodel.FileManagementViewModel
import me.danikvitek.lab5.viewmodel.State

private const val TAG = "FileManagement#onClickOpen"

@Composable
fun FileManagement(
    viewModel: FileManagementViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val launcher = rememberLauncherForActivityResult(ViewPdf()) {}

    val context = LocalContext.current

    FileManagement(
        state = state,
        onClickOpen = {
//            (context as ComponentActivity).registerForActivityResult(Act)
            runCatching {
                launcher.launch(
                    input = viewModel.file().run {
                        FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.provider",
                            this
                        ).also { Log.d(TAG, "File URI: $it") }
                    },
                    options = ActivityOptionsCompat.makeBasic(),
                )
            }.onFailure { th ->
                when (th) {
                    is ActivityNotFoundException -> {
                        Toast.makeText(
                            context,
                            "No application found which can open the file",
                            Toast.LENGTH_LONG
                        ).show()
                        Log.i(
                            "FileManagement#onClickOpen",
                            "No application found which can open the file",
                            th
                        )
                    }

                    else -> {
                        Toast.makeText(
                            context,
                            "Error opening file (${th::class.simpleName})",
                            Toast.LENGTH_LONG
                        ).show()
                        Log.e("FileManagement#onClickOpen", "Error opening file", th)
                    }
                }
            }
        },
        onClickDownload = { viewModel.downloadFile() },
        onClickDelete = { viewModel.deleteFile() },
        modifier = modifier,
    )
}

@Composable
private fun FileManagement(
    state: State,
    onClickOpen: () -> Unit,
    onClickDownload: () -> Unit,
    onClickDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
    ) {
        Button(
            onClick = onClickOpen,
            enabled = state is State.FileReady,
        ) {
            Text(text = "Open file")
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = onClickDownload,
                enabled = state is State.NoFile || state is State.Error,
            ) {
                Text(
                    text = if (state is State.Downloading) "Downloading..." else "Download",
                )
            }
            if (state is State.Downloading) {
                state.progress
                    ?.let { CircularProgressIndicator(progress = { it }) }
                ?: CircularProgressIndicator()
            }
        }
        Button(
            onClick = onClickDelete,
            enabled = state is State.FileReady,
        ) {
            Text(text = "Delete file")
        }
    }
}

private class ViewPdf : ActivityResultContract<Uri, Unit>() {
    override fun createIntent(context: Context, input: Uri): Intent =
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(input, "application/pdf")
            setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }.let { Intent.createChooser(it, "Open PDF") }

    override fun getSynchronousResult(context: Context, input: Uri): SynchronousResult<Unit>? = null

    override fun parseResult(resultCode: Int, intent: Intent?) = Unit
}

@Preview
@Composable
private fun PreviewFileManagement(
    @PreviewParameter(StatePreviewProvider::class) state: State,
) {
    Lab5Theme {
        Scaffold { innerPadding ->
            FileManagement(
                state = state,
                onClickOpen = {},
                onClickDownload = {},
                onClickDelete = {},
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

private class StatePreviewProvider : PreviewParameterProvider<State> {
    override val values: Sequence<State> = sequenceOf(
        State.NoFile,
        State.Downloading(null),
        State.Downloading(0f),
        State.Downloading(0.5f),
        State.FileReady,
        State.Error("Error"),
    )
}
