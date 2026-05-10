package com.daniel.silo.ui.share

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.daniel.silo.R
import com.daniel.silo.ui.links.AddLinkScreen
import com.daniel.silo.ui.links.AddLinkViewModel
import com.daniel.silo.ui.theme.SiloTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ShareActivity : ComponentActivity() {

    private val viewModel: AddLinkViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Extract shared URL
        val sharedUrl = when (intent?.action) {
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
            else -> ""
        }
        val sharedTitle = intent?.getStringExtra(Intent.EXTRA_SUBJECT)

        if (sharedUrl.isNotBlank()) {
            viewModel.prefill(sharedUrl, sharedTitle)
        }

        setContent {
            SiloTheme {
                val state by viewModel.state.collectAsState()

                AddLinkScreen(
                    state = state,
                    onUrlChange = viewModel::setUrl,
                    onTitleChange = viewModel::setTitle,
                    onDescriptionChange = viewModel::setDescription,
                    onCollectionChange = viewModel::setCollection,
                    onSave = viewModel::save,
                    onBack = { finish() }
                )

                // Snackbar for offline save
                if (state.savedOffline) {
                    LaunchedEffect(Unit) {
                        // Give user feedback before closing
                        kotlinx.coroutines.delay(1500)
                        finish()
                    }
                }
            }
        }
    }
}
