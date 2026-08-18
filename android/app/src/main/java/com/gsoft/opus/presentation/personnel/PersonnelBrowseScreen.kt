package com.gsoft.opus.presentation.personnel

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun PersonnelBrowseScreen(
    onPersonnelClick: (Int) -> Unit,
    viewModel: PersonnelViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        PersonnelListContent(
            state = state,
            onSearch = viewModel::setSearchQuery,
            onServiceFilter = viewModel::setServiceFilter,
            onRefresh = viewModel::refresh,
            onPersonnelClick = onPersonnelClick,
            onRequestDelete = null
        )
    }
}
