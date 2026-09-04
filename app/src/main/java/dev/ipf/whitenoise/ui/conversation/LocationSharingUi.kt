package dev.ipf.whitenoise.ui.conversation

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.ui.components.*
import dev.ipf.whitenoise.ui.components.WhiteNoiseAlertDialog as AlertDialog
import dev.ipf.whitenoise.ui.components.WhiteNoiseScaffold as Scaffold
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged

internal enum class MapOpenResult { Opened, InvalidPoint, NoHandler, Unavailable }
internal fun locationMapIntent(point: SharedLocation): Intent? = point.takeIf { it.valid }?.let { Intent(Intent.ACTION_VIEW, it.geoUri.toUri()) }
internal fun openLocationMap(context: Context, point: SharedLocation): MapOpenResult {
    val intent = locationMapIntent(point) ?: return MapOpenResult.InvalidPoint
    return try { context.startActivity(intent); MapOpenResult.Opened }
    catch (_: ActivityNotFoundException) { MapOpenResult.NoHandler }
    catch (_: SecurityException) { MapOpenResult.Unavailable }
    catch (_: IllegalArgumentException) { MapOpenResult.Unavailable }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LocationPickerDialog(session: LocationSession, onEvent: (LocationEvent) -> Unit) {
    val owner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val activity = remember(context) { generateSequence(context) { (it as? ContextWrapper)?.baseContext }.filterIsInstance<Activity>().firstOrNull() }
    DisposableEffect(session.id) {
        onDispose { if (activity?.isChangingConfigurations != true) onEvent(LocationEvent.Close) }
    }
    LaunchedEffect(session.id, session.revision, session.phase, owner) {
        if (session.phase == LocationPhase.Locating || session.phase == LocationPhase.Sending) owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            delay(350)
            onEvent(if (session.phase == LocationPhase.Locating) LocationEvent.Located(session.revision) else LocationEvent.Sent(session.revision))
            awaitCancellation()
        }
    }
    val reviewing = session.phase in setOf(LocationPhase.Review, LocationPhase.Sending)
    val locating = session.phase == LocationPhase.Locating
    val sending = session.phase == LocationPhase.Sending
    Dialog(onDismissRequest = { onEvent(LocationEvent.Close) }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false, dismissOnBackPress = false)) {
        BackHandler { onEvent(LocationEvent.Back) }
        Scaffold(Modifier.fillMaxSize().testTag("location.picker"), containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            topBar = { TopAppBar(title = { Text(stringResource(if (reviewing) R.string.location_review else R.string.location_title)) }, navigationIcon = {
                IconButton({ onEvent(LocationEvent.Back) }) { Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.back)) }
            }) }, bottomBar = {
                AdaptiveContent(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(WhiteNoiseSpacing.PinnedActionInset)) {
                    WhiteNoiseButton({ onEvent(if (reviewing) LocationEvent.Send else LocationEvent.Review) },
                        Modifier.fillMaxWidth().testTag("location.confirm"), enabled = session.point != null && !locating && session.failure != LocationFailure.SourceChanged,
                        loading = sending, loadingLabel = stringResource(R.string.location_sending)) {
                        Text(stringResource(if (session.failure == LocationFailure.SendFailed) R.string.attachment_retry else if (reviewing) R.string.location_send else R.string.location_review))
                    }
                }
            }) { padding ->
            AdaptiveContent(Modifier.fillMaxSize().padding(padding)) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(WhiteNoiseSpacing.CompactScreenMargin), verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.FormField)) {
                    if (!reviewing) {
                        Text(stringResource(R.string.location_coordinate_help), style = MaterialTheme.typography.bodyMedium)
                        LocationCoordinateField(session.latitude, true, !locating, { onEvent(LocationEvent.Latitude(it)) })
                        LocationCoordinateField(session.longitude, false, !locating, { onEvent(LocationEvent.Longitude(it)) })
                        FilledTonalButton({ onEvent(LocationEvent.Locate) }, enabled = !locating, modifier = Modifier.fillMaxWidth().testTag("location.current")) {
                            Icon(painterResource(R.drawable.ic_location_on), null)
                            Spacer(Modifier.width(WhiteNoiseSpacing.Related))
                            Text(stringResource(if (locating) R.string.location_finding else R.string.location_use_current))
                        }
                    }
                    if (locating) LinearProgressIndicator(Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite })
                    session.point?.let { LocationPointSummary(it) }
                    if (reviewing) {
                        Text(stringResource(R.string.location_fixed_point), style = MaterialTheme.typography.bodyMedium)
                        session.expectedReply?.let {
                            Text(stringResource(R.string.location_reply), style = MaterialTheme.typography.labelLarge)
                            Text(it.text.ifBlank { it.attachments.firstOrNull()?.label.orEmpty() }, maxLines = 3, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        }
                        TextButton({ onEvent(LocationEvent.Edit) }, enabled = !sending, modifier = Modifier.testTag("location.edit")) { Text(stringResource(R.string.location_change)) }
                    }
                    session.failure?.let { failure ->
                        Text(stringResource(locationFailureLabel(failure)), color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag("location.error").semantics { liveRegion = LiveRegionMode.Polite })
                        if (failure in setOf(LocationFailure.RequestFailed, LocationFailure.Unavailable, LocationFailure.PermissionDenied, LocationFailure.ServicesOff))
                            TextButton({ onEvent(LocationEvent.Locate) }, Modifier.testTag("location.retry")) { Text(stringResource(R.string.attachment_retry)) }
                    }
                    TextButton({ onEvent(LocationEvent.Close) }, Modifier.testTag("location.cancel")) { Text(stringResource(R.string.cancel)) }
                }
            }
        }
    }
}

@Composable
private fun LocationCoordinateField(value: String, latitude: Boolean, enabled: Boolean, onValue: (String) -> Unit) {
    val field = rememberTextFieldState(value)
    val callback = rememberUpdatedState(onValue)
    LaunchedEffect(value) { if (field.text.toString() != value) field.setTextAndPlaceCursorAtEnd(value) }
    LaunchedEffect(field) { snapshotFlow { field.text.toString() }.distinctUntilChanged().collect { callback.value(it) } }
    val signLabel = stringResource(if (latitude) R.string.location_latitude_sign else R.string.location_longitude_sign)
    val invalid = value.isNotBlank() && LocationSharing.number(value, latitude) == null
    val error = stringResource(if (latitude) R.string.location_latitude_error else R.string.location_longitude_error)
    WhiteNoiseTextField(field, Modifier.fillMaxWidth().testTag(if (latitude) "location.latitude" else "location.longitude"), enabled = enabled,
        label = { Text(stringResource(if (latitude) R.string.location_latitude else R.string.location_longitude)) },
        trailingIcon = {
            IconButton({
                val input = field.text.toString().trim()
                field.setTextAndPlaceCursorAtEnd(if (input.startsWith("-")) input.drop(1) else "-" + input.removePrefix("+"))
            }, enabled = enabled && value.isNotBlank() && (value.trim().startsWith("-") || value.trim().length < 32),
                modifier = Modifier.semantics { contentDescription = signLabel }.testTag(if (latitude) "location.latitude.sign" else "location.longitude.sign")) {
                Text("±", style = MaterialTheme.typography.titleLarge)
            }
        },
        textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
        isError = invalid, errorMessage = error, supportingText = if (invalid) ({ Text(error) }) else null,
        inputTransformation = InputTransformation.maxLength(32),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), lineLimits = TextFieldLineLimits.SingleLine)
}

@Composable
internal fun LocationPointSummary(point: SharedLocation) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLowest) {
        Row(Modifier.fillMaxWidth().padding(WhiteNoiseSpacing.CompactScreenMargin), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
            Icon(painterResource(R.drawable.ic_location_on), null)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
                Text(stringResource(R.string.location_title), style = MaterialTheme.typography.titleSmall)
                SelectionContainer { Text(point.coordinates, style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Ltr), modifier = Modifier.testTag("location.coordinates")) }
                point.accuracyMeters?.let { Text(stringResource(R.string.location_accuracy, it), style = MaterialTheme.typography.labelMedium) }
            }
        }
    }
}

@Composable
internal fun LocationMessageCard(point: SharedLocation, modifier: Modifier = Modifier, onOpenMap: ((SharedLocation) -> MapOpenResult)? = null) {
    val context = LocalContext.current
    var result by remember(point) { mutableStateOf<MapOpenResult?>(null) }
    var copied by remember(point) { mutableStateOf(false) }
    val clipboardLabel = stringResource(R.string.location_title)
    fun open() { result = onOpenMap?.invoke(point) ?: openLocationMap(context, point) }
    fun copy(text: String) { (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText(clipboardLabel, text)); copied = true }
    Surface(modifier.fillMaxWidth().testTag("location.message").clickable(role = Role.Button, onClickLabel = stringResource(R.string.location_open_maps), onClick = ::open),
        shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(Modifier.padding(WhiteNoiseSpacing.Related), horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_location_on), null)
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.location_title), style = MaterialTheme.typography.titleSmall)
                Text(point.coordinates, style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Ltr))
                Text(stringResource(R.string.location_open_maps), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
    if (result != null && result != MapOpenResult.Opened) AlertDialog(onDismissRequest = { result = null }, title = { Text(stringResource(R.string.location_open_maps)) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
            Text(stringResource(if (result == MapOpenResult.NoHandler) R.string.location_no_maps else R.string.location_maps_unavailable))
            SelectionContainer { Text(point.coordinates, style = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr)) }
            TextButton({ copy(point.coordinates) }) { Text(stringResource(R.string.location_copy_coordinates)) }
            TextButton({ copy(point.mapsLink) }) { Text(stringResource(R.string.location_copy_link)) }
            if (copied) Text(stringResource(R.string.copied), Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        } }, confirmButton = { TextButton(::open) { Text(stringResource(R.string.attachment_retry)) } },
        dismissButton = { TextButton({ result = null }) { Text(stringResource(R.string.close)) } })
}

private fun locationFailureLabel(value: LocationFailure) = when (value) {
    LocationFailure.Unavailable -> R.string.location_unavailable
    LocationFailure.PermissionDenied -> R.string.location_denied
    LocationFailure.ServicesOff -> R.string.location_services_off
    LocationFailure.RequestFailed -> R.string.location_request_failed
    LocationFailure.SendFailed -> R.string.location_send_failed
    LocationFailure.SourceChanged -> R.string.location_source_changed
}
