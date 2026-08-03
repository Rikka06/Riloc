package com.riloc.app.ui.component.bottombar

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.riloc.app.Natives
import com.riloc.app.R
import com.riloc.app.ui.LocalMainPagerState
import com.riloc.app.ui.util.rootAvailable

@Composable
fun NavigationRailMaterial(
    modifier: Modifier = Modifier,
) {
    val isManager = Natives.isManager
    val fullFeatured = isManager && !Natives.requireNewKernel() && rootAvailable()
    val mainPagerState = LocalMainPagerState.current

    if (!fullFeatured) return

    val items = listOf(
        Triple(R.string.home, Icons.Filled.Map, Icons.Outlined.Map),
        Triple(R.string.module, Icons.Filled.Apps, Icons.Outlined.Apps),
        Triple(R.string.settings, Icons.Filled.Settings, Icons.Outlined.Settings)
    )

    NavigationRail(
        modifier = modifier.fillMaxHeight(),
        windowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout).only(
            WindowInsetsSides.Start + WindowInsetsSides.Vertical
        )
    ) {
        Spacer(Modifier.weight(1f))
        items.forEachIndexed { index, (label, selectedIcon, unselectedIcon) ->
            val selected = mainPagerState.selectedPage == index
            NavigationRailItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        mainPagerState.animateToPage(index)
                    }
                },
                icon = {
                    Icon(
                        if (selected) selectedIcon else unselectedIcon,
                        stringResource(label)
                    )
                },
                label = { Text(stringResource(label)) }
            )
        }
        Spacer(Modifier.weight(1f))
    }
}

