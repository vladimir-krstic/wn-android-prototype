package dev.ipf.whitenoise.ui.conversation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.MessageForwarding
import dev.ipf.whitenoise.model.Profile
import dev.ipf.whitenoise.ui.components.WhiteNoiseScaffold
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing

/** A separate picker page; Back discards folder edits, Done applies the chosen chats. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ForwardFoldersPage(
    profile: Profile,
    sourceProfileId: String,
    sourceChatId: String,
    initialSelected: Set<String>,
    onBack: () -> Unit,
    onDone: (Set<String>) -> Unit,
) {
    var selected by rememberSaveable(profile.id, sourceChatId,
        stateSaver = listSaver<Set<String>, String>(save = { it.toList() }, restore = { it.toSet() }),
    ) { mutableStateOf(initialSelected) }
    WhiteNoiseScaffold(
        modifier = Modifier.fillMaxWidth().fillMaxHeight(0.88f).testTag("conversation.forward.folders"),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.batch_folders)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.back))
                    }
                },
                actions = {
                    TextButton(onClick = { onDone(selected) }, modifier = Modifier.testTag("conversation.forward.folders.done")) {
                        Text(stringResource(R.string.done))
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).testTag("conversation.forward.folderList"),
            contentPadding = PaddingValues(WhiteNoiseSpacing.CompactScreenMargin),
            verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
        ) {
            item {
                Text(stringResource(R.string.forward_folders_hint), color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium)
            }
            items(profile.chatFolders, key = { it.id }) { folder ->
                val members = MessageForwarding.folderMembers(profile, sourceProfileId, sourceChatId, folder)
                ForwardFolderChoice(folder, members, selected) {
                    selected = MessageForwarding.toggleFolder(selected, members)
                }
            }
        }
    }
}
