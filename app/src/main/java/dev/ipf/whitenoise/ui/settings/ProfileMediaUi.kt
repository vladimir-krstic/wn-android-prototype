@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.ipf.whitenoise.ui.settings

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.ProfileAvatar
import dev.ipf.whitenoise.model.ProfileImageZoom
import dev.ipf.whitenoise.ui.components.AvatarPhotoButton
import dev.ipf.whitenoise.ui.components.WhiteNoiseDropdownMenu
import dev.ipf.whitenoise.ui.components.WhiteNoiseMenuItem
import dev.ipf.whitenoise.ui.components.drawableResource
import dev.ipf.whitenoise.ui.onboarding.AvatarImageProcessor
import dev.ipf.whitenoise.ui.onboarding.AvatarWebImagePicker
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private data class ProfileBitmap(val loading: Boolean = true, val bitmap: Bitmap? = null)

@Composable
private fun profileBitmap(image: ProfileAvatar): ProfileBitmap {
    val resources = LocalResources.current
    val result by produceState(ProfileBitmap(), image, resources) {
        value = ProfileBitmap()
        val bitmap = withContext(Dispatchers.Default) { runCatching {
            when (image) {
                is ProfileAvatar.DeviceImage -> BitmapFactory.decodeByteArray(image.bytes, 0, image.bytes.size)
                is ProfileAvatar.Asset -> BitmapFactory.decodeResource(resources, image.asset.drawableResource)
                is ProfileAvatar.WebImage -> BitmapFactory.decodeResource(resources, image.asset.drawableResource)
                ProfileAvatar.Monogram -> null
            }
        }.getOrNull() }
        value = ProfileBitmap(loading = false, bitmap = bitmap)
    }
    return result
}

@Composable
fun ProfileBanner(image: ProfileAvatar, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val decoded = profileBitmap(image)
    Surface(onClick = onOpen, modifier = modifier.fillMaxWidth().aspectRatio(2f).testTag("profile.banner"), shape = MaterialTheme.shapes.large) {
        Box(contentAlignment = Alignment.Center) {
            when {
                decoded.loading -> CircularProgressIndicator()
                decoded.bitmap != null -> Image(decoded.bitmap.asImageBitmap(), stringResource(R.string.profile_banner), Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else -> Text(stringResource(R.string.profile_image_failed))
            }
        }
    }
}

private sealed interface ProfileImageSource {
    data class Device(val uri: Uri) : ProfileImageSource
    data class Bundled(val image: ProfileAvatar) : ProfileImageSource
}

@Composable
internal fun ProfileImageActions(
    ownerId: String, image: ProfileAvatar?, isBanner: Boolean,
    enabled: Boolean, onChange: (ProfileAvatar?) -> Unit,
    onBusyChanged: (Boolean) -> Unit, consumeFailure: () -> Boolean,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var menu by remember(ownerId) { mutableStateOf(false) }
    var web by remember(ownerId) { mutableStateOf(false) }
    var source by remember(ownerId) { mutableStateOf<ProfileImageSource?>(null) }
    var failure by remember(ownerId) { mutableStateOf(false) }
    var busy by remember(ownerId) { mutableStateOf(false) }
    var job by remember(ownerId) { mutableStateOf<Job?>(null) }
    var pickerOwner by remember(ownerId) { mutableStateOf<String?>(null) }
    var generation by remember(ownerId) { mutableIntStateOf(0) }
    fun prepare(selected: ProfileImageSource) {
        val id = ++generation
        job?.cancel()
        source = selected
        failure = false
        val fails = consumeFailure()
        busy = true
        onBusyChanged(true)
        job = scope.launch {
            try {
                val result = when (selected) {
                    is ProfileImageSource.Bundled -> { delay(500); selected.image }
                    is ProfileImageSource.Device -> AvatarImageProcessor.prepare(context.contentResolver, selected.uri,
                        maximumDimension = if (isBanner) 1600 else 512)?.let(ProfileAvatar::DeviceImage)
                }
                if (id != generation) return@launch
                if (result == null || fails) failure = true else { source = null; onChange(result) }
            } finally {
                if (id == generation) { busy = false; onBusyChanged(false) }
            }
        }
    }
    val photos = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val valid = pickerOwner == ownerId
        pickerOwner = null
        if (valid && uri != null) prepare(ProfileImageSource.Device(uri))
    }
    val files = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val valid = pickerOwner == ownerId
        pickerOwner = null
        if (valid && uri != null) prepare(ProfileImageSource.Device(uri))
    }
    DisposableEffect(ownerId) { onDispose { generation++; job?.cancel(); onBusyChanged(false) } }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            if (isBanner) FilledTonalButton(onClick = { menu = true }, enabled = enabled && !busy) {
                Text(stringResource(if (image == null) R.string.profile_add_banner else R.string.profile_change_banner))
            } else AvatarPhotoButton(hasPhoto = image != null && image != ProfileAvatar.Monogram, onClick = { menu = true }, enabled = enabled && !busy)
            WhiteNoiseDropdownMenu(expanded = menu, onDismissRequest = { menu = false }, items = buildList {
                add(WhiteNoiseMenuItem(stringResource(R.string.profile_choose_photos), icon = R.drawable.ic_image, onClick = { pickerOwner = ownerId; if (runCatching { photos.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }.isFailure) { pickerOwner = null; source = null; failure = true } }))
                add(WhiteNoiseMenuItem(stringResource(R.string.profile_choose_files), icon = R.drawable.ic_description, onClick = { pickerOwner = ownerId; if (runCatching { files.launch(arrayOf("image/*")) }.isFailure) { pickerOwner = null; source = null; failure = true } }))
                add(WhiteNoiseMenuItem(stringResource(R.string.find_web_image), icon = R.drawable.ic_search, onClick = { web = true }))
                if (image != null && image != ProfileAvatar.Monogram) add(WhiteNoiseMenuItem(
                    stringResource(if (isBanner) R.string.profile_remove_banner else R.string.remove_photo), icon = R.drawable.ic_delete,
                    destructive = true, onClick = { source = null; failure = false; onChange(null) },
                ))
            })
        }
        if (busy) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
            CircularProgressIndicator(Modifier.size(20.dp))
            Text(stringResource(if (isBanner) R.string.profile_preparing_banner else R.string.profile_preparing_photo))
        }
        if (failure) {
            Text(stringResource(R.string.profile_image_failed), color = MaterialTheme.colorScheme.error)
            if (source != null) TextButton(onClick = { source?.let(::prepare) }, enabled = enabled) { Text(stringResource(R.string.people_retry)) }
        }
    }
    if (web) AvatarWebImagePicker(currentChoiceId = (image as? ProfileAvatar.WebImage)?.choiceId,
        onDismiss = { web = false }, onUseImage = { web = false; prepare(ProfileImageSource.Bundled(ProfileAvatar.WebImage(it.asset, it.id))) })
}

@Composable
fun ProfileImageViewer(ownerKey: String, title: String, image: ProfileAvatar, onDismiss: () -> Unit, onEdit: (() -> Unit)? = null) {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(ownerKey) { focus.clearFocus(); keyboard?.hide() }
    val decoded = profileBitmap(image)
    val bitmap = decoded.bitmap
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var scale by remember(ownerKey, image) { mutableFloatStateOf(1f) }
    var offset by remember(ownerKey, image) { mutableStateOf(Offset.Zero) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    var pendingSave by remember(ownerKey, image) { mutableStateOf<Bitmap?>(null) }
    val savedCopy = stringResource(R.string.media_saved)
    val failedCopy = stringResource(R.string.media_save_error)
    fun clamp(value: Offset, zoom: Float): Offset {
        val bounds = ProfileImageZoom.maxPan(viewport.width, viewport.height, bitmap?.width ?: 0, bitmap?.height ?: 0, zoom)
        return Offset(value.x.coerceIn(-bounds.first, bounds.first), value.y.coerceIn(-bounds.second, bounds.second))
    }
    fun zoom(next: Float) { scale = next.coerceIn(1f, 4f); offset = clamp(offset, scale) }
    fun reset() { scale = 1f; offset = Offset.Zero }
    val transform = rememberTransformableState { amount, pan, _ -> zoom(scale * amount); offset = clamp(offset + pan, scale) }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/jpeg")) { uri ->
        val captured = pendingSave
        pendingSave = null
        if (uri != null && captured == null) scope.launch { snackbar.showSnackbar(failedCopy) }
        if (uri != null && captured != null) scope.launch {
            val success = withContext(Dispatchers.IO) { runCatching {
                checkNotNull(context.contentResolver.openOutputStream(uri)).use { check(captured.compress(Bitmap.CompressFormat.JPEG, 92, it)) }
            }.isSuccess }
            snackbar.showSnackbar(if (success) savedCopy else failedCopy)
        }
    }
    Dialog(onDismissRequest = { if (scale > 1f) reset() else onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        SettingsScaffold(title = title, onBack = onDismiss, modifier = Modifier.fillMaxSize().testTag("profile.image_viewer"), topBarActions = {
            IconButton(onClick = { pendingSave = bitmap; if (runCatching { save.launch("white-noise-profile-image.jpg") }.isFailure) { pendingSave = null; scope.launch { snackbar.showSnackbar(failedCopy) } } }, enabled = bitmap != null) {
                Icon(painterResource(R.drawable.ic_download), stringResource(R.string.save))
            }
        }) {
            Column(Modifier.fillMaxSize()) {
                val zoomLabel = stringResource(R.string.zoom_level, (scale * 100).roundToInt())
                Box(Modifier.fillMaxWidth().weight(1f).clipToBounds().onSizeChanged { viewport = it; offset = clamp(offset, scale) }
                    .transformable(transform, canPan = { scale > 1f }).semantics { stateDescription = zoomLabel }.testTag("profile.image_viewer.image"), contentAlignment = Alignment.Center) {
                    when {
                        decoded.loading -> CircularProgressIndicator()
                        bitmap != null -> Image(bitmap.asImageBitmap(), title, Modifier.fillMaxSize().graphicsLayer { scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y }, contentScale = ContentScale.Fit)
                        else -> Text(stringResource(R.string.profile_image_failed))
                    }
                }
                SnackbarHost(snackbar)
                FlowRow(Modifier.fillMaxWidth().padding(WhiteNoiseSpacing.Related), horizontalArrangement = Arrangement.Center) {
                    TextButton(onClick = { zoom(scale + .5f) }, enabled = bitmap != null && scale < 4f) { Text(stringResource(R.string.zoom_in)) }
                    TextButton(onClick = { zoom(scale - .5f) }, enabled = bitmap != null && scale > 1f) { Text(stringResource(R.string.zoom_out)) }
                    TextButton(onClick = ::reset, enabled = scale > 1f) { Text(stringResource(R.string.reset_zoom)) }
                    onEdit?.let { TextButton(onClick = it) { Text(stringResource(R.string.profile_edit)) } }
                }
            }
        }
    }
}
