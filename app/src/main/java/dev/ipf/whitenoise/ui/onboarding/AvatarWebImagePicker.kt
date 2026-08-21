package dev.ipf.whitenoise.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldLabelPosition
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
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
import dev.ipf.whitenoise.ui.components.whiteNoiseOutlinedTextFieldColors
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing

private enum class WebImageMode {
    Search,
    Url,
}

private const val NearFullSheetHeightFraction = 0.94f
// BottomSheetDefaults.DragHandle currently measures 4 dp with 22 dp vertical padding per side.
private val MaterialSheetDragHandleHeight = 48.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarWebImagePicker(
    currentChoiceId: String?,
    onDismiss: () -> Unit,
    onUseImage: (AvatarWebImageChoice) -> Unit,
) {
    var mode by remember { mutableStateOf(WebImageMode.Search) }
    val query = rememberTextFieldState()
    var selectedChoice by remember(currentChoiceId) {
        mutableStateOf(currentChoiceId?.let(AvatarWebImageCatalog::choice))
    }
    val imageUrl = rememberTextFieldState(
        initialText = selectedChoice?.let(AvatarWebImageCatalog::displayUrl).orEmpty(),
    )
    val urlChoice = AvatarWebImageCatalog.choiceMatchingUrl(imageUrl.text.toString())
    val activeChoice = if (mode == WebImageMode.Search) selectedChoice else urlChoice
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sheetContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
    val density = LocalDensity.current
    val windowHeightPx = LocalWindowInfo.current.containerSize.height
    val sheetContentHeight = with(density) {
        (windowHeightPx.toDp() * NearFullSheetHeightFraction - MaterialSheetDragHandleHeight)
            .coerceAtLeast(0.dp)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetContainerColor,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(sheetContentHeight)
                .navigationBarsPadding()
                .imePadding(),
        ) {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.find_web_image)) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.close),
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            activeChoice?.let(onUseImage)
                        },
                        enabled = activeChoice != null,
                    ) {
                        Text(stringResource(R.string.done))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = sheetContainerColor,
                ),
            )

            SecondaryTabRow(
                selectedTabIndex = mode.ordinal,
                modifier = Modifier.fillMaxWidth(),
                containerColor = sheetContainerColor,
            ) {
                Tab(
                    selected = mode == WebImageMode.Search,
                    onClick = { mode = WebImageMode.Search },
                    text = { Text(stringResource(R.string.search)) },
                )
                Tab(
                    selected = mode == WebImageMode.Url,
                    onClick = {
                        mode = WebImageMode.Url
                        if (imageUrl.text.isEmpty()) {
                            val selectedUrl = selectedChoice
                                ?.let(AvatarWebImageCatalog::displayUrl)
                                .orEmpty()
                            imageUrl.edit { replace(0, length, selectedUrl) }
                        }
                    },
                    text = { Text(stringResource(R.string.url)) },
                )
            }

            when (mode) {
                WebImageMode.Search -> SearchWebImages(
                    query = query,
                    selectedChoice = selectedChoice,
                    onSelect = { choice ->
                        selectedChoice = choice
                        val selectedUrl = AvatarWebImageCatalog.displayUrl(choice)
                        imageUrl.edit { replace(0, length, selectedUrl) }
                    },
                )

                WebImageMode.Url -> UrlWebImage(
                    imageUrl = imageUrl,
                    choice = urlChoice,
                )
            }
        }
    }
}

@Composable
private fun SearchWebImages(
    query: TextFieldState,
    selectedChoice: AvatarWebImageChoice?,
    onSelect: (AvatarWebImageChoice) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        PrivacyDisclosure(
            title = stringResource(R.string.search_privacy),
            detail = stringResource(R.string.search_privacy_detail),
        )
        OutlinedTextField(
            state = query,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin),
            label = { Text(stringResource(R.string.search_images)) },
            labelPosition = TextFieldLabelPosition.Above(),
            lineLimits = TextFieldLineLimits.SingleLine,
            colors = whiteNoiseOutlinedTextFieldColors(),
        )
        if (query.text.toString().trim().isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
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
                    items = AvatarWebImageCatalog.results(query.text.toString()),
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
    imageUrl: TextFieldState,
    choice: AvatarWebImageChoice?,
) {
    val imageUrlText = imageUrl.text.toString()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 16.dp),
    ) {
        PrivacyDisclosure(
            title = stringResource(R.string.image_privacy),
            detail = stringResource(R.string.image_privacy_detail),
        )
        OutlinedTextField(
            state = imageUrl,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin),
            label = { Text(stringResource(R.string.image_url)) },
            labelPosition = TextFieldLabelPosition.Above(),
            placeholder = { Text(stringResource(R.string.image_url_prompt)) },
            supportingText = {
                Text(
                    stringResource(
                        if (imageUrlText.isNotEmpty() && choice == null) {
                            R.string.image_url_invalid
                        } else {
                            R.string.image_url_helper
                        },
                    ),
                )
            },
            isError = imageUrlText.isNotEmpty() && choice == null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            lineLimits = TextFieldLineLimits.SingleLine,
            colors = whiteNoiseOutlinedTextFieldColors(),
        )
        if (choice != null) {
            Text(
                text = stringResource(R.string.preview),
                modifier = Modifier.padding(
                    horizontal = WhiteNoiseSpacing.CompactScreenMargin,
                    vertical = WhiteNoiseSpacing.Related,
                ),
                style = MaterialTheme.typography.titleMedium,
            )
            WebImageTile(
                choice = choice,
                selected = false,
                onSelect = null,
                modifier = Modifier
                    .padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin)
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
        modifier = Modifier.padding(
            horizontal = WhiteNoiseSpacing.CompactScreenMargin,
            vertical = WhiteNoiseSpacing.FormField,
        ),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(WhiteNoiseSpacing.CompactScreenMargin),
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
