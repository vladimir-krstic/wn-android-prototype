@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.ipf.whitenoise.ui.settings

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.AvatarWebImageCatalog
import dev.ipf.whitenoise.model.Profile
import dev.ipf.whitenoise.model.ProfileAvatar as ProfileAvatarModel
import dev.ipf.whitenoise.ui.components.AdaptiveContent
import dev.ipf.whitenoise.ui.components.LocalWhiteNoiseHeaderScroll
import dev.ipf.whitenoise.ui.components.ProfileAvatar
import dev.ipf.whitenoise.ui.components.WhiteNoiseButton
import dev.ipf.whitenoise.ui.components.WhiteNoiseScaffold
import dev.ipf.whitenoise.ui.components.whiteNoiseVerticalScroll
import dev.ipf.whitenoise.ui.onboarding.ProfileQrScannerSheet
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing
import kotlinx.coroutines.delay

private enum class ProfileScannerError {
    InvalidCode,
    Unavailable,
}

@Composable
fun ShareConnectScreen(
    profile: Profile,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val shareProfileLabel = stringResource(R.string.share_profile)
    var copied by rememberSaveable(profile.id) { mutableStateOf(false) }
    var scannerOpen by rememberSaveable { mutableStateOf(false) }
    var scannerError by rememberSaveable { mutableStateOf<ProfileScannerError?>(null) }
    var profileFound by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(2_000)
            copied = false
        }
    }

    fun shareProfile() {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, profile.name)
            putExtra(
                Intent.EXTRA_TEXT,
                "Connect with ${profile.name} on White Noise:\n${profile.publicKey}",
            )
        }
        context.startActivity(Intent.createChooser(sendIntent, shareProfileLabel))
    }

    WhiteNoiseScaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.share_and_connect)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = ::shareProfile,
                        modifier = Modifier.testTag("share_connect.share"),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_share),
                            contentDescription = stringResource(R.string.share_profile),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                scrollBehavior = LocalWhiteNoiseHeaderScroll.current,
            )
        },
        bottomBar = {
            if (!profileFound) {
                ShareConnectBottomAction(
                    onClick = {
                        scannerError = null
                        scannerOpen = true
                    },
                )
            }
        },
    ) { innerPadding ->
        AdaptiveContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (profileFound) {
                ProfileFoundContent(
                    foundProfile = demoFoundProfile(),
                    onOpenScanner = {
                        scannerError = null
                        scannerOpen = true
                    },
                    onDone = { profileFound = false },
                )
            } else {
                ShareProfileContent(
                    profile = profile,
                    copied = copied,
                    scannerError = scannerError,
                    onCopy = {
                        copyToClipboard(context, "Public key", profile.publicKey)
                        copied = true
                    },
                )
            }
        }
    }

    if (scannerOpen) {
        ProfileQrScannerSheet(
            onDismiss = { scannerOpen = false },
            onCodeScanned = { payload ->
                scannerOpen = false
                if (payload.trim().startsWith("npub")) {
                    scannerError = null
                    profileFound = true
                } else {
                    scannerError = ProfileScannerError.InvalidCode
                }
            },
            onUnavailable = {
                scannerOpen = false
                scannerError = ProfileScannerError.Unavailable
            },
        )
    }
}

@Composable
private fun ShareProfileContent(
    profile: Profile,
    copied: Boolean,
    scannerError: ProfileScannerError?,
    onCopy: () -> Unit,
) {
    val copyState = stringResource(if (copied) R.string.copied else R.string.not_copied)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .whiteNoiseVerticalScroll(rememberScrollState()),
        contentAlignment = Alignment.TopCenter,
    ) {
        val avatarSize = (maxWidth * 0.32f).coerceIn(104.dp, 152.dp)
        val qrCodeSize = (maxWidth * 0.81f).coerceIn(248.dp, 376.dp) - 16.dp
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = WhiteNoiseSpacing.CompactScreenMargin,
                    end = WhiteNoiseSpacing.CompactScreenMargin,
                    top = WhiteNoiseSpacing.Section,
                    bottom = WhiteNoiseSpacing.Section,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ProfileAvatar(
                name = profile.name,
                avatar = profile.avatar,
                modifier = Modifier.size(avatarSize),
                contentDescription = stringResource(R.string.profile_photo_for, profile.name),
            )
            Text(
                text = profile.name,
                modifier = Modifier.padding(top = WhiteNoiseSpacing.FormField),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            if (profile.nostrAddress.isNotBlank()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = profile.nostrAddress,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (profile.isNostrAddressVerified) {
                        Icon(
                            painter = painterResource(R.drawable.ic_verified_filled),
                            contentDescription = stringResource(R.string.verified),
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            val copyInteractionSource = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .widthIn(max = 360.dp)
                    .heightIn(min = 48.dp)
                    .clickable(
                        interactionSource = copyInteractionSource,
                        indication = null,
                        role = Role.Button,
                        onClick = onCopy,
                    )
                    .testTag("share_connect.copy_public_key")
                    .semantics {
                        stateDescription = copyState
                    },
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .indication(copyInteractionSource, ripple())
                        .padding(horizontal = 25.dp, vertical = 8.dp)
                        .testTag("share_connect.copy_public_key.visual"),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = profile.shortPublicKey,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.MiddleEllipsis,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Icon(
                        painter = painterResource(
                            if (copied) R.drawable.ic_check else R.drawable.ic_content_copy,
                        ),
                        contentDescription = stringResource(
                            if (copied) R.string.copied else R.string.copy_public_key,
                        ),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .size(qrCodeSize + 24.dp)
                    .testTag("share_connect.qr_surface"),
                shape = MaterialTheme.shapes.large,
                color = Color.White,
            ) {
                ProfileCode(
                    value = profile.publicKey,
                    modifier = Modifier.padding(12.dp),
                    contentDescription = stringResource(R.string.profile_qr_code),
                    marginModules = 0,
                )
            }
            Text(
                text = stringResource(R.string.scan_to_connect),
                modifier = Modifier.padding(top = 1.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
            scannerError?.let { error ->
                Text(
                    text = stringResource(
                        when (error) {
                            ProfileScannerError.InvalidCode -> R.string.profile_qr_invalid
                            ProfileScannerError.Unavailable -> R.string.profile_scanner_unavailable
                        },
                    ),
                    modifier = Modifier
                        .padding(top = WhiteNoiseSpacing.Related)
                        .widthIn(max = 440.dp),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun ShareConnectBottomAction(onClick: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
        AdaptiveContent {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(WhiteNoiseSpacing.PinnedActionInset),
                contentAlignment = Alignment.Center,
            ) {
                WhiteNoiseButton(
                    onClick = onClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("share_connect.open_scanner"),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_qr_code_scanner),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(WhiteNoiseSpacing.Related))
                    Text(stringResource(R.string.scan_qr_code))
                }
            }
        }
    }
}

@Composable
private fun ProfileFoundContent(
    foundProfile: Profile,
    onOpenScanner: () -> Unit,
    onDone: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(WhiteNoiseSpacing.Section),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        ProfileAvatar(
            name = foundProfile.name,
            avatar = foundProfile.avatar,
            modifier = Modifier.size(112.dp),
            contentDescription = null,
        )
        Text(
            text = stringResource(R.string.profile_found),
            modifier = Modifier.padding(top = WhiteNoiseSpacing.Section),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = foundProfile.name,
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = foundProfile.nostrAddress,
            modifier = Modifier.padding(top = 4.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = foundProfile.shortPublicKey,
            modifier = Modifier.padding(top = WhiteNoiseSpacing.Related),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(
            onClick = onDone,
            modifier = Modifier
                .padding(top = WhiteNoiseSpacing.Section)
                .widthIn(min = 200.dp),
        ) {
            Text(stringResource(R.string.done))
        }
        OutlinedButton(
            onClick = onOpenScanner,
            modifier = Modifier
                .padding(top = WhiteNoiseSpacing.Related)
                .widthIn(min = 200.dp),
        ) {
            Text(stringResource(R.string.scan_another))
        }
    }
}

private fun demoFoundProfile(): Profile {
    val choice = AvatarWebImageCatalog.choices.first()
    return Profile(
        id = "open-quill-found",
        name = "Open Quill",
        publicKey = "npub1q2v9n6t4r7c3x8m5k2w9p6s4y7h3d8f5j2a9e6u4z7n1m2d9",
        nostrAddress = "open-quill@whitenoise.example",
        isNostrAddressVerified = true,
        avatar = ProfileAvatarModel.WebImage(choice.asset, choice.id),
    )
}
