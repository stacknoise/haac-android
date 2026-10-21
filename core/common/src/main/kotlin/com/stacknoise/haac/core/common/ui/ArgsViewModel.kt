package com.stacknoise.haac.core.common.ui

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.DEFAULT_ARGS_KEY
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * A view model of [modelClass] for a pane inside a screen (concept 15.7): [args] reach its `SavedStateHandle` as
 * navigation arguments would, so the view model reads them the same way in a pane and on its own screen. [key]
 * must differ for each use, otherwise the owner hands out the earlier instance.
 */
@Composable
fun <VM : ViewModel> argsViewModel(modelClass: Class<VM>, key: String, args: Bundle): VM {
    val owner = checkNotNull(LocalViewModelStoreOwner.current)
    val extras = remember(owner, key) { owner.extrasWith(args) }
    return viewModel(modelClass = modelClass, viewModelStoreOwner = owner, key = key, factory = null, extras = extras)
}

/** The creation extras of this owner with [args] as the default arguments of a view model. */
private fun ViewModelStoreOwner.extrasWith(args: Bundle): CreationExtras {
    val base = (this as? HasDefaultViewModelProviderFactory)?.defaultViewModelCreationExtras ?: CreationExtras.Empty
    return MutableCreationExtras(base).apply { set(DEFAULT_ARGS_KEY, args) }
}
