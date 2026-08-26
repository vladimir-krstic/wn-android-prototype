package dev.ipf.whitenoise.ui.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.ProfileAvatar
import dev.ipf.whitenoise.ui.components.AdaptiveContent
import dev.ipf.whitenoise.ui.components.ProfileAvatar
import dev.ipf.whitenoise.ui.components.WhiteNoiseButton
import dev.ipf.whitenoise.ui.components.WhiteNoiseDropdownMenu
import dev.ipf.whitenoise.ui.components.WhiteNoiseMenuItem
import dev.ipf.whitenoise.ui.components.WhiteNoiseScaffold as Scaffold
import dev.ipf.whitenoise.ui.components.WhiteNoiseTextField
import dev.ipf.whitenoise.ui.components.whiteNoiseVerticalScroll
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    initialName: String,
    onBack: () -> Unit,
    onSignUp: (name: String, about: String, avatar: ProfileAvatar?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val photoErrorText = stringResource(R.string.photo_error)
    val name = rememberSaveable(initialName, saver = TextFieldState.Saver) {
        TextFieldState(initialText = initialName)
    }
    val about = rememberSaveable(saver = TextFieldState.Saver) { TextFieldState() }
    var avatar by remember { mutableStateOf<ProfileAvatar?>(null) }
    var webChoiceId by remember { mutableStateOf<String?>(null) }
    var isPhotoMenuOpen by remember { mutableStateOf(false) }
    var isWebPickerOpen by remember { mutableStateOf(false) }
    var isPreparingPhoto by remember { mutableStateOf(false) }
    var photoError by remember { mutableStateOf<String?>(null) }
    var isSigningUp by remember { mutableStateOf(false) }
    var preparationJob by remember { mutableStateOf<Job?>(null) }

    fun prepare(uri: android.net.Uri) {
        preparationJob?.cancel()
        preparationJob = scope.launch {
            isPreparingPhoto = true
            photoError = null
            val prepared = runCatching {
                AvatarImageProcessor.prepare(context.contentResolver, uri)
            }.getOrNull()
            if (prepared == null) {
                photoError = photoErrorText
            } else {
                avatar = ProfileAvatar.DeviceImage(prepared)
                webChoiceId = null
            }
            isPreparingPhoto = false
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri?.let(::prepare)
    }
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let(::prepare)
    }

    DisposableEffect(Unit) {
        onDispose { preparationJob?.cancel() }
    }

    LaunchedEffect(isSigningUp) {
        if (!isSigningUp) return@LaunchedEffect
        delay(2_000)
        isSigningUp = false
        onSignUp(name.text.toString().trim(), about.text.toString(), avatar)
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.sign_up),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                scrollBehavior = dev.ipf.whitenoise.ui.components.LocalWhiteNoiseHeaderScroll.current,
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(WhiteNoiseSpacing.PinnedActionInset),
                contentAlignment = Alignment.Center,
            ) {
                WhiteNoiseButton(
                    onClick = { isSigningUp = true },
                    enabled = !isPreparingPhoto,
                    loading = isSigningUp,
                    loadingLabel = stringResource(R.string.creating_profile),
                    modifier = Modifier
                        .widthIn(max = 520.dp)
                        .fillMaxWidth()
                        .testTag("onboarding.sign_up.action"),
                ) {
                    Text(stringResource(R.string.sign_up))
                }
            }
        },
    ) { contentPadding ->
        AdaptiveContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .consumeWindowInsets(contentPadding),
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = 520.dp)
                    .fillMaxSize()
                    .whiteNoiseVerticalScroll(rememberScrollState())
                    .padding(
                        horizontal = WhiteNoiseSpacing.CompactScreenMargin,
                        vertical = WhiteNoiseSpacing.CompactScreenMargin,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Section),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
                ) {
                    ProfileAvatar(
                        name = name.text.toString(),
                        avatar = avatar ?: ProfileAvatar.Monogram,
                        modifier = Modifier.size(120.dp),
                    )
                    Box {
                        FilledTonalButton(
                            onClick = { isPhotoMenuOpen = true },
                            enabled = !isPreparingPhoto && !isSigningUp,
                        ) {
                            Text(
                                stringResource(
                                    if (avatar == null) R.string.add_photo else R.string.change_photo,
                                ),
                            )
                        }
                        WhiteNoiseDropdownMenu(
                            expanded = isPhotoMenuOpen,
                            onDismissRequest = { isPhotoMenuOpen = false },
                            items = buildList {
                                add(
                                    WhiteNoiseMenuItem(
                                        label = stringResource(R.string.choose_photos),
                                        icon = R.drawable.ic_image,
                                        onClick = {
                                            photoPicker.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                            )
                                        },
                                    ),
                                )
                                add(
                                    WhiteNoiseMenuItem(
                                        label = stringResource(R.string.choose_files),
                                        icon = R.drawable.ic_description,
                                        onClick = { filePicker.launch(arrayOf("image/*")) },
                                    ),
                                )
                                add(
                                    WhiteNoiseMenuItem(
                                        label = stringResource(R.string.find_web_image),
                                        icon = R.drawable.ic_search,
                                        onClick = { isWebPickerOpen = true },
                                    ),
                                )
                                if (avatar != null) {
                                    add(
                                        WhiteNoiseMenuItem(
                                            label = stringResource(R.string.remove_photo),
                                            icon = R.drawable.ic_delete,
                                            destructive = true,
                                            onClick = {
                                                preparationJob?.cancel()
                                                isPreparingPhoto = false
                                                avatar = null
                                                webChoiceId = null
                                                photoError = null
                                            },
                                        ),
                                    )
                                }
                            },
                        )
                    }
                    if (isPreparingPhoto) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                            Text(stringResource(R.string.preparing_photo))
                        }
                    }
                    photoError?.let { error ->
                        Text(
                            text = error,
                            modifier = Modifier.semantics {
                                liveRegion = LiveRegionMode.Polite
                            },
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.FormField),
                ) {
                    WhiteNoiseTextField(
                        state = name,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSigningUp,
                        label = { Text(stringResource(R.string.name)) },
                        lineLimits = TextFieldLineLimits.SingleLine,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next,
                        ),
                    )
                    WhiteNoiseTextField(
                        state = about,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSigningUp,
                        label = { Text(stringResource(R.string.about)) },
                        placeholder = { Text(stringResource(R.string.about_prompt)) },
                        lineLimits = TextFieldLineLimits.MultiLine(
                            minHeightInLines = 3,
                            maxHeightInLines = 6,
                        ),
                    )
                }

            }
        }
    }

    if (isWebPickerOpen) {
        AvatarWebImagePicker(
            currentChoiceId = webChoiceId,
            onDismiss = { isWebPickerOpen = false },
            onUseImage = { choice ->
                preparationJob?.cancel()
                isPreparingPhoto = false
                avatar = ProfileAvatar.WebImage(choice.asset, choice.id)
                webChoiceId = choice.id
                photoError = null
                isWebPickerOpen = false
            },
        )
    }
}
