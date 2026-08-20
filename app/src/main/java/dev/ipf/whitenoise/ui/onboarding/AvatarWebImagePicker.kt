package dev.ipf.whitenoise.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.AvatarWebImageCatalog
import dev.ipf.whitenoise.model.AvatarWebImageChoice
import dev.ipf.whitenoise.ui.components.drawableResource

private enum class WebImageMode {
    Search,
    Url,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarWebImagePicker(
    currentChoiceId: String?,
    onDismiss: () -> Unit,
    onUseImage: (AvatarWebImageChoice) -> Unit,
) {
    var mode by remember { mutableStateOf(WebImageMode.Search) }
    var query by remember { mutableStateOf("") }
    var selectedChoice by remember(currentChoiceId) {
        mutableStateOf(currentChoiceId?.let(AvatarWebImageCatalog::choice))
    }
    var imageUrl by remember(currentChoiceId) {
        mutableStateOf(selectedChoice?.let(AvatarWebImageCatalog::displayUrl).orEmpty())
    }
    val urlChoice = AvatarWebImageCatalog.choiceMatchingUrl(imageUrl)
    val activeChoice = if (mode == WebImageMode.Search) selectedChoice else urlChoice

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        contentWindowInsets = { WindowInsets.safeDrawing },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.94f),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.close))
                }
                TextButton(
                    onClick = {
                        activeChoice?.let(onUseImage)
                    },
                    enabled = activeChoice != null,
                ) {
                    Text(stringResource(R.string.done))
                }
            }

            PrimaryTabRow(selectedTabIndex = mode.ordinal) {
                Tab(
                    selected = mode == WebImageMode.Search,
                    onClick = { mode = WebImageMode.Search },
                    text = { Text(stringResource(R.string.search)) },
                )
                Tab(
                    selected = mode == WebImageMode.Url,
                    onClick = {
                        mode = WebImageMode.Url
                        if (imageUrl.isEmpty()) {
                            imageUrl = selectedChoice
                                ?.let(AvatarWebImageCatalog::displayUrl)
                                .orEmpty()
                        }
                    },
                    text = { Text(stringResource(R.string.url)) },
                )
            }

            when (mode) {
                WebImageMode.Search -> SearchWebImages(
                    query = query,
                    onQueryChange = { query = it },
                    selectedChoice = selectedChoice,
                    onSelect = { choice ->
                        selectedChoice = choice
                        imageUrl = AvatarWebImageCatalog.displayUrl(choice)
                    },
                )

                WebImageMode.Url -> UrlWebImage(
                    imageUrl = imageUrl,
                    onUrlChange = { imageUrl = it },
                    choice = urlChoice,
                )
            }
        }
    }
}

@Composable
private fun SearchWebImages(
    query: String,
    onQueryChange: (String) -> Unit,
    selectedChoice: AvatarWebImageChoice?,
    onSelect: (AvatarWebImageChoice) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        PrivacyDisclosure(
            title = stringResource(R.string.search_privacy),
            detail = stringResource(R.string.search_privacy_detail),
        )
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            label = { Text(stringResource(R.string.search_images)) },
            singleLine = true,
        )
        if (query.trim().isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.search_images),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = stringResource(R.string.search_images_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(
                    items = AvatarWebImageCatalog.results(query),
                    key = AvatarWebImageChoice::id,
                ) { choice ->
                    WebImageTile(
                        choice = choice,
                        selected = selectedChoice == choice,
                        onSelect = { onSelect(choice) },
                    )
                }
            }
        }
    }
}

@Composable
private fun UrlWebImage(
    imageUrl: String,
    onUrlChange: (String) -> Unit,
    choice: AvatarWebImageChoice?,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 16.dp),
    ) {
        PrivacyDisclosure(
            title = stringResource(R.string.image_privacy),
            detail = stringResource(R.string.image_privacy_detail),
        )
        TextField(
            value = imageUrl,
            onValueChange = onUrlChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            label = { Text(stringResource(R.string.image_url)) },
            placeholder = { Text(stringResource(R.string.image_url_prompt)) },
            supportingText = {
                Text(
                    stringResource(
                        if (imageUrl.isNotEmpty() && choice == null) {
                            R.string.image_url_invalid
                        } else {
                            R.string.image_url_helper
                        },
                    ),
                )
            },
            isError = imageUrl.isNotEmpty() && choice == null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            singleLine = true,
        )
        if (choice != null) {
            Text(
                text = stringResource(R.string.preview),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleMedium,
            )
            WebImageTile(
                choice = choice,
                selected = false,
                onSelect = null,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(0.6f),
            )
        }
    }
}

@Composable
private fun PrivacyDisclosure(
    title: String,
    detail: String,
) {
    Surface(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = detail,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
    HorizontalDivider()
}

@Composable
private fun WebImageTile(
    choice: AvatarWebImageChoice,
    selected: Boolean,
    onSelect: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .then(
                if (onSelect == null) {
                    Modifier
                } else {
                    Modifier.selectable(
                        selected = selected,
                        role = Role.RadioButton,
                        onClick = onSelect,
                    )
                },
            ),
    ) {
        Image(
            painter = painterResource(choice.asset.drawableResource),
            contentDescription = choice.accessibilityLabel,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "✓",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
