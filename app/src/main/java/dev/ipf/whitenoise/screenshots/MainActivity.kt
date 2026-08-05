package dev.ipf.whitenoise.screenshots

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {
    private var currentScene by mutableStateOf<ScreenshotScene?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentScene = sceneFromIntent(intent)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.WHITE, Color.WHITE),
        )
        setContent {
            ScreenshotTheme {
                BackHandler(enabled = currentScene != null) {
                    currentScene = null
                }
                Box(Modifier.fillMaxSize()) {
                    when (currentScene) {
                        ScreenshotScene.Relays -> RelaysScene(onBack = { currentScene = null })
                        ScreenshotScene.ProfileSwitcher -> ProfileSwitcherScene(onBack = { currentScene = null })
                        ScreenshotScene.Chats -> ChatsScene()
                        ScreenshotScene.Conversation -> ConversationScene(onBack = { currentScene = null })
                        ScreenshotScene.ShareConnect ->
                            ShareConnectScene(onBack = { currentScene = null })
                        null -> ScenePicker(onSceneSelected = { currentScene = it })
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        currentScene = sceneFromIntent(intent)
    }

    private fun sceneFromIntent(intent: Intent?): ScreenshotScene? {
        val data = intent?.data ?: return null
        if (data.scheme != "whitenoise-screenshots" || data.host != "scene") return null
        return ScreenshotScene.fromRoute(data.pathSegments.firstOrNull())
    }
}
