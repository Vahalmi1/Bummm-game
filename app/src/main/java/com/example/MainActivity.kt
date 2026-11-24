package com.example

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.GamePhase
import com.example.model.ScreenRoute
import com.example.ui.components.BummmBackground
import com.example.ui.components.ParticleOverlay
import com.example.ui.screens.GameScreen
import com.example.ui.screens.HowToPlayOverlay
import com.example.ui.screens.LobbyScreen
import com.example.ui.screens.PauseOverlay
import com.example.ui.screens.ResultScreen
import com.example.ui.screens.TitleScreen
import com.example.ui.screens.ToastBanner
import com.example.ui.theme.BummmTheme
import com.example.ui.theme.OuterBg
import com.example.viewmodel.BummmViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: BummmViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BummmTheme {
                BummmApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun BummmApp(viewModel: BummmViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val screen by viewModel.screen.collectAsStateWithLifecycle()
    val gameState by viewModel.game.collectAsStateWithLifecycle()
    val showHow by viewModel.showHowToPlay.collectAsStateWithLifecycle()
    val showPause by viewModel.showPause.collectAsStateWithLifecycle()
    val toastMsg by viewModel.toastMessage.collectAsStateWithLifecycle()

    // Lifecycle foreground/background sync for music + auto-pause
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.onAppForegroundChanged(true)
                Lifecycle.Event.ON_PAUSE -> viewModel.onAppForegroundChanged(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Vibration listener
    LaunchedEffect(Unit) {
        viewModel.vibrateEvents.collect { pattern ->
            triggerVibration(context, pattern)
        }
    }

    // Back button handling for overlays and non-root screens
    BackHandler(enabled = showHow || showPause || screen != ScreenRoute.TITLE) {
        when {
            showHow -> viewModel.closeHowToPlay()
            showPause -> viewModel.resumeGame()
            screen == ScreenRoute.LOBBY -> viewModel.goToTitle()
            screen == ScreenRoute.GAME -> {
                if (gameState.phase == GamePhase.PAUSED) viewModel.resumeGame()
                else viewModel.pauseGame()
            }
            screen == ScreenRoute.RESULT -> viewModel.goToTitle()
        }
    }

    var wallCenter by remember { mutableStateOf<Offset?>(null) }
    val padCenters = remember { mutableStateMapOf<Int, Offset>() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OuterBg)
    ) {
        BummmBackground()

        Crossfade(
            targetState = screen,
            animationSpec = tween(250),
            label = "screenTransition"
        ) { target ->
            when (target) {
                ScreenRoute.TITLE -> {
                    TitleScreen(
                        prefs = prefs,
                        onPlayClick = { viewModel.goToLobby() },
                        onHowToPlayClick = { viewModel.openHowToPlay() },
                        onToggleSfx = { viewModel.toggleSfx() },
                        onToggleMusic = { viewModel.toggleMusic() }
                    )
                }
                ScreenRoute.LOBBY -> {
                    LobbyScreen(
                        prefs = prefs,
                        magicForLevel = viewModel::magicFor,
                        onBack = { viewModel.goToTitle() },
                        onCycleAvatar = { viewModel.cycleSlotAvatar(it) },
                        onUpdateName = { idx, name -> viewModel.updateSlotName(idx, name) },
                        onSetSlotType = { idx, type -> viewModel.setSlotType(idx, type) },
                        onSetDifficulty = { viewModel.setDifficulty(it) },
                        onStepLevel = { viewModel.stepStartLevel(it) },
                        onStartGame = { viewModel.startGame() }
                    )
                }
                ScreenRoute.GAME -> {
                    GameScreen(
                        state = gameState,
                        onTapNumber = { viewModel.onHumanTapNumber(it) },
                        onTapBum = { viewModel.onHumanTapBum() },
                        onPauseClick = { viewModel.pauseGame() },
                        onWallBoundsChanged = { wallCenter = it },
                        onPadButtonBoundsChanged = { num, center -> padCenters[num] = center }
                    )
                }
                ScreenRoute.RESULT -> {
                    ResultScreen(
                        state = gameState,
                        onPlayAgain = { viewModel.startGame() },
                        onShare = { shareText ->
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            val chooser = Intent.createChooser(sendIntent, "BUMMM!")
                            context.startActivity(chooser)
                        },
                        onChangePlayers = { viewModel.goToLobby() },
                        onTitleScreen = { viewModel.goToTitle() }
                    )
                }
            }
        }

        PauseOverlay(
            visible = showPause,
            onResume = { viewModel.resumeGame() },
            onRestart = { viewModel.startGame() },
            onQuit = { viewModel.goToTitle() }
        )

        HowToPlayOverlay(
            visible = showHow,
            onClose = { viewModel.closeHowToPlay() }
        )

        ParticleOverlay(
            requests = viewModel.particles,
            wallCenterProvider = { wallCenter },
            padButtonCenterProvider = { padCenters[it] }
        )

        ToastBanner(message = toastMsg)
    }
}

@Suppress("DEPRECATION")
private fun triggerVibration(context: Context, pattern: LongArray) {
    try {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return
        if (!vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            vibrator.vibrate(pattern, -1)
        }
    } catch (_: Throwable) {
    }
}
