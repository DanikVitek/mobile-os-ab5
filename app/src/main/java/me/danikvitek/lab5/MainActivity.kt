package me.danikvitek.lab5

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.booleanResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import dagger.hilt.android.AndroidEntryPoint
import me.danikvitek.lab5.screen.FileManagement
import me.danikvitek.lab5.ui.theme.Lab5Theme
import me.danikvitek.lab5.viewmodel.FileManagementViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Lab5Theme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    val viewModel: FileManagementViewModel by viewModels()
                    FileManagement(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding),
                    )
                }

                DescriptionPopup()
            }
        }
    }
}

@Composable
private fun DescriptionPopup(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val sharedPref: SharedPreferences = with(context) {
        getSharedPreferences(
            getString(R.string.preference_file_key, packageName),
            Context.MODE_PRIVATE
        )
    }

    val showDescriptionPopup = sharedPref.getBoolean(
        context.getString(R.string.preference_show_description_popup),
        booleanResource(R.bool.preference_default_show_description_popup),
    )
    var showPopupState by remember { mutableStateOf(showDescriptionPopup) }

    if (showPopupState) {
        val onDismissRequest = { showPopupState = false }
        Popup(
            alignment = Alignment.Center,
            properties = PopupProperties(),
            onDismissRequest = onDismissRequest,
        ) {
            DescriptionPopupContent(
                onDismissRequest = onDismissRequest,
                onChangeShowPopup = {
                    with(sharedPref.edit()) {
                        putBoolean(
                            context.getString(R.string.preference_show_description_popup),
                            it
                        )
                        apply()
                    }
                },
                modifier = Modifier.fillMaxWidth(0.8f),
            )
        }
    }
}

@Composable
fun DescriptionPopupContent(
    onDismissRequest: () -> Unit,
    onChangeShowPopup: (showPopup: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        modifier = modifier,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
    ) {
        Text(
            text = stringResource(R.string.description_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 12.dp, start = 12.dp, end = 12.dp),
        )
        Text(
            text = stringResource(
                R.string.description_text,
                stringResource(R.string.download),
                stringResource(R.string.open_file),
                stringResource(R.string.delete_file),
            ),
            modifier = Modifier.padding(start = 12.dp, end = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(
            modifier = Modifier
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                    var checked by remember { mutableStateOf(false) }
                    Checkbox(
                        checked = checked,
                        onCheckedChange = {
                            checked = it
                            onChangeShowPopup(!it)
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.secondary,
                        )
                    )
                }
                Text(
                    text = stringResource(R.string.description_dont_show_again),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Button(
                onClick = onDismissRequest,
            ) {
                Text(text = stringResource(R.string.description_ok))
            }
        }
    }
}

@Preview(locale = "uk")
@Composable
private fun PreferencesPopupPreview() {
    Lab5Theme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            DescriptionPopupContent(
                onDismissRequest = {},
                onChangeShowPopup = {},
                modifier = Modifier.fillMaxWidth(0.8f),
            )
        }
    }
}