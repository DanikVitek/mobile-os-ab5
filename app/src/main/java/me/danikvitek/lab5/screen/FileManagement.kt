package me.danikvitek.lab5.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.ComponentActivity
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import me.danikvitek.lab5.ui.theme.Lab5Theme
import me.danikvitek.lab5.viewmodel.FileManagementViewModel
import me.danikvitek.lab5.viewmodel.State

@Composable
fun FileManagement(
    originActivity: ComponentActivity,
    viewModel: FileManagementViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()

    FileManagement(
        state = state,
        onClickOpen = { viewModel.openFile(originActivity) },
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
//    val launcher = rememberLauncherForActivityResult(OpenPdf()) {}

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
    ) {
        Button(
            onClick = onClickOpen,
            /*{
                launcher.launch(
                    (state as State.FileReady).file.toUri(),
                    options = ActivityOptionsCompat.makeBasic(),
                )
            }*/
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

private class OpenPdf : ActivityResultContract<Uri, Uri?>() {
    override fun createIntent(context: Context, input: Uri) =
        Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, input)
        }

    override fun getSynchronousResult(context: Context, input: Uri) = null

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        TODO("Not yet implemented")
    }
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
        State.Downloading(0f),
        State.Downloading(0.5f),
        State.FileReady,
        State.Error("Error"),
    )
}
