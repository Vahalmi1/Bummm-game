package com.example.model

enum class ScreenRoute {
    TITLE, LOBBY, GAME, RESULT
}

enum class SlotType {
    HUMAN, BOT, OFF
}

enum class Difficulty(
    val label: String,
    val mult: Float,
    val cells: Int,
    val cols: Int,
    val acc: Float
) {
    RELAXED("Relaxed", 1.9f, 24, 6, 0.80f),
    NORMAL("Normal", 1.5f, 40, 8, 0.90f),
    HARD("Hard", 1.2f, 48, 8, 0.96f)
}

data class PlayerSlot(
    val type: SlotType,
    val name: String,
    val av: Int
)

data class AppPrefs(
    val sfx: Boolean = true,
    val music: Boolean = true,
    val vibrate: Boolean = true,
    val fx: Boolean = true,
    val diff: Difficulty = Difficulty.NORMAL,
    val startLevel: Int = 1,
    val hints: Int = 0,
    val bestLevel: Int = 1,
    val bestScore: Int = 0,
    val games: Int = 0,
    val slots: List<PlayerSlot> = defaultSlots()
) {
    companion object {
        fun defaultSlots(): List<PlayerSlot> = listOf(
            PlayerSlot(SlotType.HUMAN, "Player 1", 0),
            PlayerSlot(SlotType.HUMAN, "Player 2", 1),
            PlayerSlot(SlotType.BOT, "Bot Bella", 2),
            PlayerSlot(SlotType.BOT, "Bot Max", 3),
            PlayerSlot(SlotType.OFF, "Bot Zoe", 4),
            PlayerSlot(SlotType.OFF, "Bot Leo", 5)
        )
    }
}

data class PlayerState(
    val id: Int,
    val name: String,
    val type: SlotType,
    val av: Int,
    val lives: Int = 3,
    val score: Int = 0,
    val combo: Int = 0,
    val bestCombo: Int = 0,
    val alive: Boolean = true,
    val correct: Int = 0,
    val wrong: Int = 0,
    val shakeKey: Int = 0,
    val brokenHeartIndex: Int = -1
)

enum class CellKind {
    EMPTY, NUMBER, BOOM, FAIL
}

data class WallCell(
    val kind: CellKind = CellKind.EMPTY,
    val number: Int = 0,
    val colorIndex: Int = 0
)

data class PadButtonState(
    val number: Int,
    val colorIndex: Int,
    val feedback: PadFeedback = PadFeedback.NONE,
    val isHint: Boolean = false
)

enum class PadFeedback {
    NONE, OK, NO
}

enum class MessageTone {
    NEUTRAL, GOOD, BAD
}

data class GameMessage(
    val prefix: String = "",
    val boldPart: String = "",
    val suffix: String = "Get ready!",
    val boldSecond: String = "",
    val tail: String = "",
    val tone: MessageTone = MessageTone.NEUTRAL
)

enum class GamePhase {
    IDLE, COUNTDOWN, READY, TURN, RESOLVE, LEVEL_UP, PAUSED, OVER
}

data class BurstBanner(
    val id: Int = 0,
    val isBad: Boolean = false,
    val text: String = "BUMMM!"
)

data class CountdownBanner(
    val id: Int = 0,
    val bigText: String = "",
    val smallText: String = "",
    val durationMs: Int = 700,
    val isLevelBanner: Boolean = false
)

data class FloatingCallout(
    val id: Int,
    val seatIndex: Int,
    val text: String,
    val isBad: Boolean
)

data class ParticleRequest(
    val id: Int,
    val target: ParticleTarget,
    val buttonNumber: Int? = null
)

enum class ParticleTarget {
    WALL_GOOD, WALL_BAD, WALL_END, PAD_BUTTON, BUM_BUTTON, CONFETTI
}

data class GameUiState(
    val players: List<PlayerState> = emptyList(),
    val turn: Int = -1,
    val level: Int = 1,
    val magic: Int = 3,
    val levelLen: Int = 15,
    val from: Int = 1,
    val levelCount: Int = 0,
    val totalCount: Int = 0,
    val currentNumber: Int = 1,
    val isBoom: Boolean = false,
    val phase: GamePhase = GamePhase.IDLE,
    val timerFraction: Float = 1f,
    val timeText: String = "--",
    val wallCells: List<WallCell> = List(42) { WallCell() },
    val lastStack: List<String> = listOf("-", "-", "-"),
    val nextStack: List<String> = listOf("-", "-", "-"),
    val padCols: Int = 8,
    val padButtons: List<PadButtonState> = emptyList(),
    val padLocked: Boolean = true,
    val bumFeedback: PadFeedback = PadFeedback.NONE,
    val bumHint: Boolean = false,
    val showHandHint: Boolean = false,
    val handTargetIsBum: Boolean = false,
    val handTargetNumber: Int? = null,
    val message: GameMessage = GameMessage(),
    val burstBanner: BurstBanner? = null,
    val countdownBanner: CountdownBanner? = null,
    val floatingCallouts: List<FloatingCallout> = emptyList(),
    val shakeCounter: Int = 0,
    val flashRedCounter: Int = 0,
    val ptsPulseCounter: Int = 0,
    val comboPulseCounter: Int = 0,
    val badgeNowPulseCounter: Int = 0,
    val badgeMagicPulseCounter: Int = 0,
    val rankedResults: List<PlayerState> = emptyList(),
    val newBest: Boolean = false
)
