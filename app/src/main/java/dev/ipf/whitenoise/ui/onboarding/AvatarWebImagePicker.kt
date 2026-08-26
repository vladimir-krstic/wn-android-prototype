package dev.ipf.whitenoise.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import dev.ipf.whitenoise.ui.components.WhiteNoiseModalBottomSheet as ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.AvatarWebImageCatalog
import dev.ipf.whitenoise.model.AvatarWebImageChoice
import dev.ipf.whitenoise.ui.components.WhiteNoiseEmptyState
import dev.ipf.whitenoise.ui.components.drawableResource
import dev.ipf.whitenoise.ui.components.WhiteNoiseTextField
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing

private enum class WebImageMode {
    Search,
    Url,
}

private const val NearFullSheetHeightFraction = 0.94f
// BottomSheetDefaults.DragHandle currently measures 4 dp with 22 dp vertical padding per side.
private val MaterialSheetDragHandleHeight = 48.dp
// The native app bar reserves 4 dp at its action edge, which is intended for icon buttons.
private val MaterialTopAppBarEndInset = 4.dp
private val WebImageGridGutter = 2.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarWebImagePicker(
    currentChoiceId: String?,
    onDismiss: () -> Unit,
    onUseImage: (AvatarWebImageChoice) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val density = LocalDensity.current
    val windowHeightPx = LocalWindowInfo.current.containerSize.height
    val systemInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
    val sheetContentHeight = with(density) {
        val windowHeight = windowHeightPx.toDp()
        val sheetHeight = minOf(
            windowHeight * NearFullSheetHeightFraction,
            windowHeight - systemInsets.getTop(this).toDp(),
        )
        // The sheet owns the handle and safe-area padding outside this content's height.
        (sheetHeight - MaterialSheetDragHandleHeight - systemInsets.getBottom(this).toDp())
            .coerceAtLeast(0.dp)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        // Keep the handle and content safe when the IME expands the sheet to the top.
        // ModalBottomSheet already applies/consumes the IME; do not add imePadding here.
        contentWindowInsets = { WindowInsets.safeDrawing },
    ) {
        AvatarWebImagePickerContent(
            currentChoiceId = currentChoiceId,
            onDismiss = onDismiss,
            onUseImage = onUseImage,
            modifier = Modifier.height(sheetContentHeight),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AvatarWebImagePickerContent(
    currentChoiceId: String?,
    onDismiss: () -> Unit,
    onUseImage: (AvatarWebImageChoice) -> Unit,
    modifier: Modifier = Modifier,
) {
    var mode by rememberSaveable { mutableStateOf(WebImageMode.Search) }
    val query = rememberTextFieldState()
    var selectedChoiceId by rememberSaveable(currentChoiceId) {
        mutableStateOf(currentChoiceId)
    }
    val selectedChoice = selectedChoiceId?.let(AvatarWebImageCatalog::choice)
    val imageUrl = rememberTextFieldState(
        initialText = selectedChoice?.let(AvatarWebImageCatalog::displayUrl).orEmpty(),
    )
    val urlChoice = AvatarWebImageCatalog.choiceMatchingUrl(imageUrl.text.toString())
    val activeChoice = if (mode == WebImageMode.Search) selectedChoice else urlChoice
    val sheetContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
    val searchGridState = rememberLazyGridState()
    val urlScrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val finishEditing: () -> Unit = {
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    Column(modifier = modifier.fillMaxWidth()) {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    stringResource(R.string.find_web_image),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            navigationIcon = {
                IconButton(onClick = {
                    finishEditing()
                    onDismiss()
                }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.close),
                    )
                }
            },
            actions = {
                Button(
                    onClick = {
                        activeChoice?.let { choice ->
                            finishEditing()
                            onUseImage(choice)
                        }
                    },
                    enabled = activeChoice != null,
                    modifier = Modifier.padding(
                        end = WhiteNoiseSpacing.CompactScreenMargin - MaterialTopAppBarEndInset,
                    ),
                ) {
                    Text(stringResource(R.string.done), maxLines = 1)
                }
            },
            // The containing sheet has already applied and consumed the safe area.
            windowInsets = WindowInsets(0, 0, 0, 0),
            colors = TopAppBarDefaults.topAppBarColors(containerColor = sheetContainerColor),
        )

        WebImageModeButtons(
            mode = mode,
            onModeChange = { nextMode ->
                if (nextMode != mode) {
                    finishEditing()
                    mode = nextMode
                    if (nextMode == WebImageMode.Url && imageUrl.text.isEmpty()) {
                        val selectedUrl = selectedChoice
                            ?.let(AvatarWebImageCatalog::displayUrl)
                            .orEmpty()
                        imageUrl.edit { replace(0, length, selectedUrl) }
                    }
                }
            },
        )

        when (mode) {
            WebImageMode.Search -> SearchWebImages(
                query = query,
                selectedChoice = selectedChoice,
                gridState = searchGridState,
                onSearch = finishEditing,
                onSelect = { choice ->
                    selectedChoiceId = choice.id
                    val selectedUrl = AvatarWebImageCatalog.displayUrl(choice)
                    imageUrl.edit { replace(0, length, selectedUrl) }
                    finishEditing()
                },
            )

            WebImageMode.Url -> UrlWebImage(
                imageUrl = imageUrl,
                choice = urlChoice,
                onPreview = finishEditing,
                modifier = Modifier.verticalScroll(urlScrollState),
            )
        }
    }
}

@Composable
private fun WebImageModeButtons(mode: WebImageMode, onModeChange: (WebImageMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = WhiteNoiseSpacing.CompactScreenMargin,
                vertical = WhiteNoiseSpacing.Related,
            )
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
    ) {
        WebImageMode.entries.forEach { option ->
            val isSelected = mode == option
            val isSearch = option == WebImageMode.Search
            // Same public Material component/shape pattern as the Android Photo Picker.
            FilledTonalButton(
                onClick = { onModeChange(option) },
                modifier = Modifier
                    .weight(1f)
                    .semantics {
                        role = Role.Tab
                        selected = isSelected
                    },
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    },
                    contentColor = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                ),
            ) {
                Icon(
                    painter = painterResource(if (isSearch) R.drawable.ic_search else R.drawable.ic_link),
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(
                    stringResource(if (isSearch) R.string.search else R.string.url),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SearchWebImages(
    query: TextFieldState,
    selectedChoice: AvatarWebImageChoice?,
    gridState: LazyGridState,
    onSearch: () -> Unit,
    onSelect: (AvatarWebImageChoice) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        state = gridState,
        modifier = Modifier.fillMaxSize().selectableGroup(),
        contentPadding = PaddingValues(bottom = WhiteNoiseSpacing.CompactScreenMargin),
        horizontalArrangement = Arrangement.spacedBy(WebImageGridGutter),
        verticalArrangement = Arrangement.spacedBy(WebImageGridGutter),
    ) {
        item(key = "search-controls", span = { GridItemSpan(maxLineSpan) }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin)
                    .padding(
                        top = WhiteNoiseSpacing.Related,
                        // The grid adds its own gutter after this full-span item.
                        bottom = WhiteNoiseSpacing.Section - WebImageGridGutter,
                    ),
                verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.FormField),
            ) {
                PrivacyDisclosure(
                    title = stringResource(R.string.search_privacy),
                    detail = stringResource(R.string.search_privacy_detail),
                )
                WhiteNoiseTextField(
                    state = query,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.search_images)) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_search),
                            contentDescription = null,
                        )
                    },
                    trailingIcon = if (query.text.isNotEmpty()) {
                        {
                            IconButton(onClick = { query.edit { replace(0, length, "") } }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_close),
                                    contentDescription = stringResource(R.string.clear_search),
                                )
                            }
                        }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    onKeyboardAction = { onSearch() },
                    lineLimits = TextFieldLineLimits.SingleLine,
                )
            }
        }
        if (query.text.toString().trim().isEmpty()) {
            item(key = "search-empty", span = { GridItemSpan(maxLineSpan) }) {
                WhiteNoiseEmptyState(
                    title = stringResource(R.string.search_images),
                    detail = stringResource(R.string.search_images_empty),
                )
            }
        } else {
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

@Composable
private fun UrlWebImage(
    imageUrl: TextFieldState,
    choice: AvatarWebImageChoice?,
    onPreview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val imageUrlText = imageUrl.text.toString()
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin)
            .padding(top = WhiteNoiseSpacing.Related, bottom = WhiteNoiseSpacing.CompactScreenMargin),
        verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.FormField),
    ) {
        PrivacyDisclosure(
            title = stringResource(R.string.image_privacy),
            detail = stringResource(R.string.image_privacy_detail),
        )
        WhiteNoiseTextField(
            state = imageUrl,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.image_url)) },
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
            errorMessage = stringResource(R.string.image_url_invalid),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
            onKeyboardAction = { onPreview() },
            lineLimits = TextFieldLineLimits.SingleLine,
        )
        if (choice != null) {
            Column(
                // Add to the column's peer gap to make a full section gap before the preview.
                modifier = Modifier.padding(top = WhiteNoiseSpacing.Section - WhiteNoiseSpacing.FormField),
                verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
            ) {
                Text(
                    text = stringResource(R.string.preview),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleMedium,
                )
                WebImageTile(
                    choice = choice,
                    selected = false,
                    onSelect = null,
                    modifier = Modifier.fillMaxWidth(0.6f),
                )
            }
        }
    }
}

@Composable
private fun PrivacyDisclosure(
    title: String,
    detail: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(WhiteNoiseSpacing.CompactScreenMargin),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                title,
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleSmall,
            )
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
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(32.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    modifier = Modifier.padding(6.dp),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}
