package dev.ipf.whitenoise.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldLabelPosition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.LoginPrototypeData
import dev.ipf.whitenoise.model.PrivateKeyState
import dev.ipf.whitenoise.model.PrivateKeyValidator
import dev.ipf.whitenoise.ui.components.AdaptiveContent
import dev.ipf.whitenoise.ui.components.WhiteNoiseButton
import dev.ipf.whitenoise.ui.components.WhiteNoiseTopBar
import dev.ipf.whitenoise.ui.components.whiteNoiseOutlinedTextFieldColors
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing
import kotlinx.coroutines.delay

private enum class ScannerDialog {
    WrongContent,
    Unavailable,
}

@Composable
fun SignInScreen(
    onBack: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val privateKey = remember { TextFieldState() }
    val normalizedKey = PrivateKeyValidator.normalize(privateKey.text.toString())
    val keyState = PrivateKeyValidator.state(normalizedKey)
    val signingInDescription = stringResource(R.string.signing_in)
    val inProgressDescription = stringResource(R.string.wn_in_progress)
    var isSigningIn by remember { mutableStateOf(false) }
    var scannerDialog by remember { mutableStateOf<ScannerDialog?>(null) }

    val scanner = remember(context) {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .enableAutoZoom()
            .build()
        GmsBarcodeScanning.getClient(context, options)
    }

    fun beginSignIn() {
        if (keyState == PrivateKeyState.Valid && !isSigningIn) isSigningIn = true
    }

    fun beginScan() {
        scannerDialog = null
        scanner.startScan()
            .addOnSuccessListener { barcode ->
                val payload = barcode.rawValue.orEmpty().trim()
                if (PrivateKeyValidator.state(payload) == PrivateKeyState.Valid) {
                    privateKey.edit { replace(0, length, payload) }
                } else {
                    scannerDialog = ScannerDialog.WrongContent
                }
            }
            .addOnCanceledListener { }
            .addOnFailureListener {
                scannerDialog = ScannerDialog.Unavailable
            }
    }

    LaunchedEffect(isSigningIn) {
        if (!isSigningIn) return@LaunchedEffect
        delay(2_000)
        isSigningIn = false
        onSignIn()
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
            WhiteNoiseButton(
                onClick = ::beginSignIn,
                enabled = keyState == PrivateKeyState.Valid && !isSigningIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(WhiteNoiseSpacing.PinnedActionInset)
                    .semantics {
                        if (isSigningIn) {
                            contentDescription = signingInDescription
                            stateDescription = inProgressDescription
                        }
                    },
            ) {
                if (isSigningIn) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(vertical = 2.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
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
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = WhiteNoiseSpacing.CompactScreenMargin,
                        vertical = WhiteNoiseSpacing.Section,
                    ),
                verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
            ) {
                OutlinedSecureTextField(
                    state = privateKey,
                    enabled = !isSigningIn,
                    modifier = Modifier.fillMaxWidth(),
                    labelPosition = TextFieldLabelPosition.Above(),
                    label = { Text(stringResource(R.string.private_key)) },
                    placeholder = { Text(stringResource(R.string.enter_private_key)) },
                    trailingIcon = {
                        if (!isSigningIn) {
                            TextButton(
                                onClick = {
                                    val replacement = if (normalizedKey.isEmpty()) {
                                        LoginPrototypeData.privateKey
                                    } else {
                                        ""
                                    }
                                    privateKey.edit { replace(0, length, replacement) }
                                },
                            ) {
                                Text(
                                    stringResource(
                                        if (normalizedKey.isEmpty()) R.string.paste else R.string.clear,
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
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Go,
                    ),
                    colors = whiteNoiseOutlinedTextFieldColors(),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = ::beginScan,
                        enabled = !isSigningIn,
                    ) {
                        Text(stringResource(R.string.scan_qr_code))
                    }
                }
                Spacer(Modifier.weight(1f))
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
