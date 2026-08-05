package dev.ipf.whitenoise.screenshots

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private const val MarmotaPublicKey =
    "npub1m8z7q4k6v2c9r5t3y8p4s7h2d6n9w3x5j8f4u7e2a6k9q8x4k"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShareConnectScene(onBack: () -> Unit) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = Modifier.semantics { paneTitle = "Share and connect" },
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Share & connect") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (selectedTab == 0) {
                            IconButton(onClick = { shareProfile(context) }) {
                                Icon(Icons.Default.Share, contentDescription = "Share profile")
                            }
                        }
                    },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = CanvasGray,
                            scrolledContainerColor = CanvasGray,
                        ),
                )
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = CanvasGray,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Share") },
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Connect") },
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets(0.dp),
        containerColor = CanvasGray,
    ) { innerPadding ->
        if (selectedTab == 0) {
            ShareProfileContent(
                modifier = Modifier.padding(innerPadding),
                onCopy = { copyPublicKey(context) },
            )
        } else {
            ConnectProfileContent(modifier = Modifier.padding(innerPadding))
        }
    }
}

@Composable
private fun ShareProfileContent(
    modifier: Modifier,
    onCopy: () -> Unit,
) {
    var copied by remember { mutableStateOf(false) }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.spaceXl, vertical = Dimens.spaceXl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PhotoAvatar(
            resource = R.drawable.profile_avatar_marmota,
            size = 112.dp,
            contentDescription = "Marmota",
        )
        Text(
            text = "Marmota",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = Dimens.spaceMd),
        )
        FilledTonalButton(
            onClick = {
                onCopy()
                copied = true
            },
            modifier = Modifier.padding(top = Dimens.spaceMd),
        ) {
            Text(
                text = "npub1m8z7q4k6v…8x4k",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontFamily = FontFamily.Monospace,
            )
            Icon(
                imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                contentDescription = if (copied) "Public key copied" else "Copy public key",
                modifier = Modifier.padding(start = Dimens.spaceSm).size(18.dp),
            )
        }

        Spacer(Modifier.height(Dimens.spaceXxl))

        ElevatedCard(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .sizeIn(maxWidth = 352.dp)
                    .aspectRatio(1f),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.elevatedCardColors(containerColor = SurfaceWhite),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.profile_qr_marmota),
                contentDescription = "Marmota profile QR code",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(Dimens.spaceLg),
            )
        }
        Text(
            text = "Scan to connect",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Dimens.spaceMd),
        )
    }
}

@Composable
private fun ConnectProfileContent(modifier: Modifier) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = Dimens.spaceXl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        ElevatedCard(
            colors = CardDefaults.elevatedCardColors(containerColor = SurfaceWhite),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = Dimens.spaceXxl, vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                )
                Text(
                    text = "Scan a profile QR code",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = Dimens.spaceXl),
                )
                Text(
                    text = "Point your camera at another White Noise profile to connect.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = Dimens.spaceSm),
                )
            }
        }
    }
}

private fun copyPublicKey(context: Context) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Marmota public key", MarmotaPublicKey))
}

private fun shareProfile(context: Context) {
    val intent =
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                "Marmota on White Noise\n$MarmotaPublicKey",
            )
        }
    context.startActivity(Intent.createChooser(intent, "Share profile"))
}
