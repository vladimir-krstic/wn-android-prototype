@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.ipf.whitenoise.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.text.input.TextFieldState
import dev.ipf.whitenoise.ui.components.whiteNoiseVerticalScroll
import dev.ipf.whitenoise.ui.components.WhiteNoiseAlertDialog as AlertDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import dev.ipf.whitenoise.ui.components.WhiteNoiseScaffold as Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.LoginPrototypeData
import dev.ipf.whitenoise.model.PrivateKeyState
import dev.ipf.whitenoise.model.PrivateKeyValidator
import dev.ipf.whitenoise.ui.components.AdaptiveContent
import dev.ipf.whitenoise.ui.components.WhiteNoiseButton
import dev.ipf.whitenoise.ui.components.WhiteNoiseButtonDefaults
import dev.ipf.whitenoise.ui.components.WhiteNoiseTopBar
import dev.ipf.whitenoise.ui.components.WhiteNoiseSecureTextField
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing
import kotlinx.coroutines.delay

private enum class ScannerDialog {
    WrongContent,
    Unavailable,
}

@Composable
fun SignInScreen(
    onBack: () -> Unit,
    onScan: () -> Unit,
    privateKey: TextFieldState,
    scannedPrivateKey: String?,
    scannerUnavailable: Boolean,
    onScannedPrivateKeyConsumed: () -> Unit,
    onScannerUnavailableConsumed: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val normalizedKey = PrivateKeyValidator.normalize(privateKey.text.toString())
    val keyState = PrivateKeyValidator.state(normalizedKey)
    var isSigningIn by remember { mutableStateOf(false) }
    var scannerDialog by remember { mutableStateOf<ScannerDialog?>(null) }

    fun beginSignIn() {
        if (keyState == PrivateKeyState.Valid && !isSigningIn) isSigningIn = true
    }

    fun beginScan() {
        scannerDialog = null
        onScan()
    }

    LaunchedEffect(isSigningIn) {
        if (!isSigningIn) return@LaunchedEffect
        delay(2_000)
        isSigningIn = false
        onSignIn()
    }

    LaunchedEffect(scannedPrivateKey, scannerUnavailable) {
        scannedPrivateKey?.let { rawPayload ->
            val payload = rawPayload.trim()
            if (PrivateKeyValidator.state(payload) == PrivateKeyState.Valid) {
                privateKey.edit { replace(0, length, payload) }
            } else {
                scannerDialog = ScannerDialog.WrongContent
            }
            onScannedPrivateKeyConsumed()
        }
        if (scannerUnavailable) {
            scannerDialog = ScannerDialog.Unavailable
            onScannerUnavailableConsumed()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            WhiteNoiseTopBar(
                title = stringResource(R.string.sign_in),
                onBack = onBack,
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
                    onClick = ::beginSignIn,
                    enabled = keyState == PrivateKeyState.Valid,
                    loading = isSigningIn,
                    loadingLabel = stringResource(R.string.signing_in),
                    modifier = Modifier
                        .widthIn(max = 520.dp)
                        .fillMaxWidth()
                        .testTag("onboarding.sign_in.action"),
                ) {
                    Text(stringResource(R.string.sign_in))
                }
            }
        },
    ) { contentPadding ->
        AdaptiveContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = 520.dp)
                    .fillMaxSize()
                    .whiteNoiseVerticalScroll(rememberScrollState())
                    .padding(
                        horizontal = WhiteNoiseSpacing.CompactScreenMargin,
                        vertical = WhiteNoiseSpacing.Section,
                    ),
                verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WhiteNoiseSecureTextField(
                        state = privateKey,
                        enabled = !isSigningIn,
                        modifier = Modifier.weight(1f),
                        label = { Text(stringResource(R.string.private_key)) },
                        placeholder = { Text(stringResource(R.string.enter_private_key)) },
                        trailingIcon = {
                            if (!isSigningIn) {
                                val isEmpty = normalizedKey.isEmpty()
                                IconButton(
                                    onClick = {
                                        val replacement = if (isEmpty) {
                                            LoginPrototypeData.privateKey
                                        } else {
                                            ""
                                        }
                                        privateKey.edit { replace(0, length, replacement) }
                                    },
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                ) {
                                    Icon(
                                        painter = painterResource(
                                            if (isEmpty) {
                                                R.drawable.ic_content_paste
                                            } else {
                                                R.drawable.ic_close
                                            },
                                        ),
                                        contentDescription = stringResource(
                                            if (isEmpty) {
                                                R.string.paste_private_key
                                            } else {
                                                R.string.clear_private_key
                                            },
                                        ),
                                    )
                                }
                            }
                        },
                        supportingText = {
                            Text(
                                text = stringResource(
                                    if (keyState == PrivateKeyState.Invalid) {
                                        R.string.private_key_invalid
                                    } else {
                                        R.string.private_key_help
                                    },
                                ),
                            )
                        },
                        isError = keyState == PrivateKeyState.Invalid,
                        errorMessage = stringResource(R.string.private_key_invalid),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Go,
                        ),
                    )

                    FilledTonalIconButton(
                        onClick = ::beginScan,
                        modifier = Modifier.size(WhiteNoiseButtonDefaults.TaskHeight),
                        enabled = !isSigningIn,
                        shape = MaterialTheme.shapes.extraLarge,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_qr_code_scanner),
                            contentDescription = stringResource(R.string.scan_qr_code),
                        )
                    }
                }
            }
        }
    }

    when (scannerDialog) {
        ScannerDialog.WrongContent -> ScannerErrorDialog(
            title = stringResource(R.string.wrong_qr_title),
            detail = stringResource(R.string.wrong_qr_detail),
            onDismiss = { scannerDialog = null },
            onRetry = ::beginScan,
        )

        ScannerDialog.Unavailable -> ScannerErrorDialog(
            title = stringResource(R.string.scanner_unavailable_title),
            detail = stringResource(R.string.scanner_unavailable_detail),
            onDismiss = { scannerDialog = null },
            onRetry = ::beginScan,
        )

        null -> Unit
    }
}

@Composable
private fun ScannerErrorDialog(
    title: String,
    detail: String,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(detail) },
        confirmButton = {
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.try_again))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
    )
}
