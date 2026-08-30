package ir.k1adili.projectcam.ui.theme

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable

/**
 * The default Material3 TopAppBar uses a neutral surface color, not the theme's primary color -
 * which is why the app didn't actually look "Caterpillar yellow" even though the color scheme's
 * primary was set correctly: nothing on screen was using primary prominently enough. Applying
 * this to every top bar gives the app its branded yellow-bar/black-icon look throughout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun projectCamTopAppBarColors(): TopAppBarColors = TopAppBarDefaults.topAppBarColors(
    containerColor = MaterialTheme.colorScheme.primary,
    scrolledContainerColor = MaterialTheme.colorScheme.primary,
    titleContentColor = MaterialTheme.colorScheme.onPrimary,
    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
)
