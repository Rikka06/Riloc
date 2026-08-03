package com.riloc.app.ui.screen.about

import androidx.compose.runtime.Composable
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.dropUnlessResumed
import com.riloc.app.BuildConfig
import com.riloc.app.R
import com.riloc.app.ui.LocalUiMode
import com.riloc.app.ui.UiMode
import com.riloc.app.ui.navigation3.LocalNavigator

@Composable
fun AboutScreen() {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val htmlString = stringResource(
        id = R.string.riloc_about_source_code,
        "<b><a href=\"https://github.com/Rikka06/Android-UI-Template\">Android-UI-Template</a></b>",
        "<b><a href=\"https://github.com/noobexon1/XposedFakeLocation\">XposedFakeLocation</a></b>",
        "<b><a href=\"https://github.com/auag0/HideMockLocation\">HideMockLocation</a></b>",
        "<b><a href=\"https://github.com/Mikotwa/FuckLocation\">FuckLocation</a></b>",
    )
    val state = AboutUiState(
        title = stringResource(R.string.about),
        appName = stringResource(R.string.app_name),
        versionName = BuildConfig.VERSION_NAME,
        links = extractLinks(htmlString),
    )
    val actions = AboutScreenActions(
        onBack = dropUnlessResumed { navigator.pop() },
        onOpenLink = {
            Toast.makeText(context, "UI template: external links are disabled.", Toast.LENGTH_SHORT).show()
        },
    )

    when (LocalUiMode.current) {
        UiMode.Miuix -> AboutScreenMiuix(state, actions)
        UiMode.Material -> AboutScreenMaterial(state, actions)
    }
}
