package dev.ipf.whitenoise.screenshots

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun ScenePicker(onSceneSelected: (ScreenshotScene) -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(24.dp),
    ) {
        Spacer(Modifier.height(28.dp))
        Text(
            text = "Store screenshot scenes",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Choose a deterministic scene to inspect. Automated capture opens these routes directly.",
            style = MaterialTheme.typography.bodyLarge,
            color = MutedInk,
            modifier = Modifier.padding(top = 10.dp, bottom = 28.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            ScreenshotScene.entries.forEach { scene ->
                Button(
                    onClick = { onSceneSelected(scene) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = SurfaceWhite,
                            contentColor = Ink,
                        ),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(scene.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            scene.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedInk,
                            modifier = Modifier.padding(top = 3.dp),
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
                    }
                }
            }
        }
    }
}

