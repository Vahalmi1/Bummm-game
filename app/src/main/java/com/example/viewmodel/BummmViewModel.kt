package com.example.viewmodel

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundEngine
import com.example.data.PreferencesRepository
import com.example.model.AppPrefs
import com.example.model.BurstBanner
import com.example.model.CellKind
import com.example.model.CountdownBanner
import com.example.model.Difficulty
import com.example.model.FloatingCallout
import com.example.model.GameMessage
import com.example.model.GamePhase
import com.example.model.GameUiState
import com.example.model.MessageTone
import com.example.model.PadButtonState
import com.example.model.PadFeedback
import com.example.model.ParticleRequest
import com.example.model.ParticleTarget
import com.example.model.PlayerState
import com.example.model.ScreenRoute
import com.example.model.SlotType
import com.example.model.WallCell
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

sealed interface GameAnswer {
    data object None : GameAnswer
    data object Boom : GameAnswer
    data class Num(val n: Int) : GameAnswer
}

class BummmViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = PreferencesRepository(application)
    val sound = SoundEngine()

    private val _prefs = MutableStateFlow(repo.load())
    val prefs: StateFlow<AppPrefs> = _prefs.asStateFlow()

    private val _screen = MutableStateFlow(ScreenRoute.TITLE)
    val screen: StateFlow<ScreenRoute> = _screen.asStateFlow()

    private val _showHowToPlay = MutableStateFlow(false)
    val showHowToPlay: StateFlow<Boolean> = _showHowToPlay.asStateFlow()

    private val _showPause = MutableStateFlow(false)
    val showPause: StateFlow<Boolean> = _showPause.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _game = MutableStateFlow(GameUiState())
    val game: StateFlow<GameUiState> = _game.asStateFlow()

    private val _particles = MutableSharedFlow<ParticleRequest>(extraBufferCapacity = 32)
    val particles = _particles.asSharedFlow()

    private val _vibrateEvents = MutableSharedFlow<LongArray>(extraBufferCapacity = 16)
    val vibrateEvents = _vibrateEvents.asSharedFlow()

    private var timerJob: Job? = null
    private var scheduledJob: Job? = null
    private var botJob: Job? = null
    private var countdownJob: Job? = null
    private var toastJob: Job? = null

    private var turnStartMs = 0L
    private var turnDurationMs = 3000L
    private var turnElapsedOnPause = 0L
    private var prePausePhase: GamePhase = GamePhase.IDLE
    private var pendingAfterResume: (() -> Unit)? = null
    private var botAnswer: GameAnswer = GameAnswer.None
    private var botFireAtMs = 0L
    private val outOrder = mutableListOf<PlayerState>()

    private var calloutIdSeq = 0
    private var bannerIdSeq = 0
    private var particleIdSeq = 0
    private var gameSessionId = 0

    init {
        sound.sfxEnabled = _prefs.value.sfx
        sound.musicEnabled = _prefs.value.music
    }

    fun onAppForegroundChanged(isForeground: Boolean) {
        sound.setForeground(isForeground)
        if (!isForeground && _screen.value == ScreenRoute.GAME) {
            pauseGame()
        }
    }

    override fun onCleared() {
        super.onCleared()
        sound.release()
    }

    /* ============ Math & Rules (exact match to HTML) ============ */
    fun magicFor(level: Int): Int = 2 * level + 1
    private fun levelLenFor(level: Int): Int = if (level <= 3) 15 else min(30, 15 + 2 * (level - 3))
    private fun isBoom(n: Int, k: Int): Boolean = n % k == 0 || n.toString().contains(k.toString())

    private fun levelStart(level: Int, k: Int, len: Int): Int {
        if (level <= 3) return 1
        val anchor = k * Random.nextInt(2, 7)
        val upper = (len - 4).coerceAtLeast(3)
        val sub = Random.nextInt(3, upper + 1)
        return max(1, anchor - sub)
    }

    private fun limitSecondsFor(level: Int): Double = when {
        level <= 3 -> 3.0
        level <= 7 -> 2.5
        level <= 15 -> 2.0
        level <= 30 -> 1.5
        level <= 50 -> 1.2
        level <= 100 -> 0.9
        else -> max(0.6, 0.9 - (level - 100) * 0.004)
    }

    private fun why(n: Int, k: Int): String {
        val d = n % k == 0
        val h = n.toString().contains(k.toString())
        return when {
            d && h -> "$n is a multiple of $k AND contains $k"
            d -> "$n is in the $k-times table"
            h -> "$n contains a $k"
            else -> "$n has no $k and isn't a multiple of $k"
        }
    }

    /* ============ Navigation & Preferences ============ */
    fun goToLobby() {
        sound.click()
        _screen.value = ScreenRoute.LOBBY
    }

    fun goToTitle() {
        sound.click()
        cancelAllCoroutines()
        _showPause.value = false
        _showHowToPlay.value = false
        _screen.value = ScreenRoute.TITLE
    }

    fun openHowToPlay() {
        sound.click()
        _showHowToPlay.value = true
    }

    fun closeHowToPlay() {
        sound.click()
        _showHowToPlay.value = false
    }

    fun toggleSfx() {
        val next = !_prefs.value.sfx
        updatePrefs { it.copy(sfx = next) }
        sound.sfxEnabled = next
        sound.click()
    }

    fun toggleMusic() {
        val next = !_prefs.value.music
        updatePrefs { it.copy(music = next) }
        sound.musicEnabled = next
        sound.click()
    }

    fun setSlotType(index: Int, type: SlotType) {
        val currentSlots = _prefs.value.slots
        val after = currentSlots.mapIndexed { j, s -> if (j == index) type else s.type }
        if (!after.contains(SlotType.HUMAN)) {
            showToast("You need at least one human player")
            return
        }
        val botDefaults = listOf("Bot Bella", "Bot Max", "Bot Zoe", "Bot Leo", "Bot Kai", "Bot Mia")
        val updated = currentSlots.mapIndexed { j, s ->
            if (j != index) s else {
                var newName = s.name
                if (type == SlotType.BOT && Regex("^Player \\d$").matches(newName)) {
                    newName = botDefaults[j]
                } else if (type == SlotType.HUMAN && newName.startsWith("Bot ")) {
                    newName = "Player ${j + 1}"
                }
                s.copy(type = type, name = newName)
            }
        }
        updatePrefs { it.copy(slots = updated) }
        sound.click()
    }

    fun cycleSlotAvatar(index: Int) {
        val slots = _prefs.value.slots
        val curAv = slots[index].av
        var next = curAv
        var attempts = 0
        do {
            next = (next + 1) % 6
            attempts++
        } while (
            slots.withIndex().any { (j, s) -> j != index && s.type != SlotType.OFF && s.av == next } &&
            next != curAv &&
            attempts < 10
        )
        val updated = slots.mapIndexed { j, s -> if (j == index) s.copy(av = next) else s }
        updatePrefs { it.copy(slots = updated) }
        sound.pop()
    }

    fun updateSlotName(index: Int, name: String) {
        val trimmed = name.take(10)
        val updated = _prefs.value.slots.mapIndexed { j, s -> if (j == index) s.copy(name = trimmed) else s }
        updatePrefs { it.copy(slots = updated) }
    }

    fun setDifficulty(diff: Difficulty) {
        updatePrefs { it.copy(diff = diff) }
        sound.click()
    }

    fun stepStartLevel(delta: Int) {
        val next = (_prefs.value.startLevel + delta).coerceIn(1, 99)
        updatePrefs { it.copy(startLevel = next) }
        sound.click()
    }

    fun showToast(text: String) {
        _toastMessage.value = text
        toastJob?.cancel()
        toastJob = viewModelScope.launch {
            delay(2200)
            _toastMessage.value = null
        }
    }

    private fun updatePrefs(transform: (AppPrefs) -> AppPrefs) {
        val next = transform(_prefs.value)
        _prefs.value = next
        repo.save(next)
    }

    private fun vibrate(pattern: LongArray) {
        if (_prefs.value.vibrate) {
            _vibrateEvents.tryEmit(pattern)
        }
    }

    private fun emitParticles(target: ParticleTarget, buttonNumber: Int? = null) {
        if (_prefs.value.fx) {
            _particles.tryEmit(ParticleRequest(++particleIdSeq, target, buttonNumber))
        }
    }

    /* ============ Game Lifecycle ============ */
    fun startGame() {
        sound.click()
        cancelAllCoroutines()
        _showPause.value = false
        _showHowToPlay.value = false
        gameSessionId++
        val session = gameSessionId

        val activeSlots = _prefs.value.slots.filter { it.type != SlotType.OFF }
        if (activeSlots.isEmpty()) return

        updatePrefs { it.copy(games = it.games + 1) }
        outOrder.clear()

        val players = activeSlots.mapIndexed { idx, slot ->
            PlayerState(
                id = idx,
                name = slot.name.trim().ifEmpty { "Player" },
                type = slot.type,
                av = slot.av
            )
        }

        val startLv = _prefs.value.startLevel
        val k = magicFor(startLv)
        val len = levelLenFor(startLv)
        val from = levelStart(startLv, k, len)
        val (lastStack, nextStack) = computeStacks(from, 0, len)

        _game.value = GameUiState(
            players = players,
            turn = -1,
            level = startLv,
            magic = k,
            levelLen = len,
            from = from,
            levelCount = 0,
            totalCount = 0,
            currentNumber = from,
            isBoom = isBoom(from, k),
            phase = GamePhase.IDLE,
            timerFraction = 1f,
            timeText = "--",
            wallCells = List(42) { WallCell() },
            lastStack = lastStack,
            nextStack = nextStack,
            padCols = _prefs.value.diff.cols,
            padButtons = emptyList(),
            padLocked = true,
            message = GameMessage(
                suffix = "${if (players.size > 1) "${players.size} players" else "Solo"}. Count together!",
                tone = MessageTone.NEUTRAL
            ),
            badgeNowPulseCounter = 1,
            badgeMagicPulseCounter = 1
        )
        _screen.value = ScreenRoute.GAME

        runCountdown(
            session = session,
            seq = listOf("3", "2", "1", "GO!"),
            stepMs = 700,
            sub = "Level $startLv: magic number $k!"
        ) {
            nextTurn(session)
        }
    }

    private fun computeStacks(from: Int, levelCount: Int, len: Int): Pair<List<String>, List<String>> {
        val last = mutableListOf<String>()
        val next = mutableListOf<String>()
        for (i in 1..3) {
            val idx = levelCount - i
            last.add(if (idx >= 0) (from + idx).toString() else "-")
            val nx = levelCount + i
            next.add(if (nx < len) (from + nx).toString() else "-")
        }
        return last to next
    }

    private fun runCountdown(
        session: Int,
        seq: List<String>,
        stepMs: Int,
        sub: String,
        onComplete: () -> Unit
    ) {
        countdownJob?.cancel()
        _game.update { it.copy(phase = GamePhase.COUNTDOWN) }
        countdownJob = viewModelScope.launch {
            for (item in seq) {
                if (session != gameSessionId) return@launch
                _game.update {
                    it.copy(
                        countdownBanner = CountdownBanner(
                            id = ++bannerIdSeq,
                            bigText = item,
                            smallText = sub,
                            durationMs = (stepMs * 0.95f).toInt(),
                            isLevelBanner = false
                        )
                    )
                }
                if (item.length == 1 && item[0].isDigit()) sound.tick() else sound.go()
                delay(stepMs.toLong())
            }
            if (session != gameSessionId) return@launch
            _game.update { it.copy(countdownBanner = null) }
            onComplete()
        }
    }

    private fun nextTurn(session: Int) {
        if (session != gameSessionId) return
        val g = _game.value
        if (g.phase == GamePhase.OVER) return

        var i = g.turn
        do {
            i = (i + 1) % g.players.size
        } while (!g.players[i].alive)

        val p = g.players[i]
        val cur = g.from + g.levelCount
        val boom = isBoom(cur, g.magic)
        val (lastS, nextS) = computeStacks(g.from, g.levelCount, g.levelLen)
        val board = buildBoard(cur, _prefs.value.diff)

        _game.update {
            it.copy(
                turn = i,
                currentNumber = cur,
                isBoom = boom,
                lastStack = lastS,
                nextStack = nextS,
                padCols = _prefs.value.diff.cols,
                padButtons = board,
                padLocked = true,
                bumFeedback = PadFeedback.NONE,
                bumHint = false,
                showHandHint = false,
                handTargetNumber = null,
                phase = GamePhase.READY,
                timerFraction = 1f,
                timeText = "--",
                badgeNowPulseCounter = it.badgeNowPulseCounter + 1,
                message = GameMessage(
                    boldPart = p.name,
                    suffix = "${if (p.type == SlotType.BOT) " (bot)" else ""}, your turn!",
                    tone = MessageTone.NEUTRAL
                )
            )
        }

        val aliveHumans = g.players.count { it.alive && it.type == SlotType.HUMAN }
        val waitMs = if (p.type == SlotType.HUMAN && aliveHumans > 1) 750L else 450L
        scheduleAction(session, waitMs) {
            startTurn(session)
        }
    }

    private fun buildBoard(cur: Int, diff: Difficulty): List<PadButtonState> {
        val nums = LinkedHashSet<Int>()
        nums.add(cur)
        val pool = mutableListOf<Int>()
        for (n in max(1, cur - 16)..(cur + 16)) {
            if (n != cur) pool.add(n)
        }
        pool.shuffle()
        while (nums.size < diff.cells && pool.isNotEmpty()) {
            nums.add(pool.removeAt(pool.lastIndex))
        }
        val upper = max(60, cur + 30)
        while (nums.size < diff.cells) {
            nums.add(Random.nextInt(1, upper + 1))
        }
        return nums.toList().shuffled().map { n ->
            PadButtonState(number = n, colorIndex = Random.nextInt(0, 8))
        }
    }

    private fun startTurn(session: Int) {
        if (session != gameSessionId) return
        val g = _game.value
        val p = g.players.getOrNull(g.turn) ?: return
        val diff = _prefs.value.diff
        val lenExtra = max(0, g.currentNumber.toString().length - 2) * 0.25
        val ms = max(
            900L,
            ((limitSecondsFor(g.level) * diff.mult + lenExtra) * 1000.0).toLong()
        )
        turnDurationMs = ms
        turnStartMs = SystemClock.elapsedRealtime()
        _game.update { it.copy(phase = GamePhase.TURN, padLocked = false) }
        startTimerLoop(session)

        if (p.type == SlotType.BOT) {
            planBot(session)
        } else {
            showHintIfNeeded()
        }
    }

    private fun startTimerLoop(session: Int) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (session == gameSessionId && _game.value.phase == GamePhase.TURN) {
                val elapsed = SystemClock.elapsedRealtime() - turnStartMs
                val left = (1f - elapsed.toFloat() / turnDurationMs.toFloat()).coerceIn(0f, 1f)
                val remSec = max(0L, turnDurationMs - elapsed) / 1000.0
                val formatted = String.format(java.util.Locale.US, "%.1f", remSec)
                _game.update {
                    if (it.timerFraction != left || it.timeText != formatted) {
                        it.copy(timerFraction = left, timeText = formatted)
                    } else it
                }
                if (elapsed >= turnDurationMs) {
                    resolveAnswer(session, GameAnswer.None)
                    return@launch
                }
                delay(16L)
            }
        }
    }

    private fun showHintIfNeeded() {
        if (_prefs.value.hints >= 8) return
        updatePrefs { it.copy(hints = it.hints + 1) }
        val g = _game.value
        if (g.isBoom) {
            _game.update {
                it.copy(
                    bumHint = true,
                    showHandHint = true,
                    handTargetIsBum = true,
                    handTargetNumber = null
                )
            }
        } else {
            _game.update { state ->
                state.copy(
                    padButtons = state.padButtons.map { btn ->
                        if (btn.number == state.currentNumber) btn.copy(isHint = true) else btn
                    },
                    showHandHint = true,
                    handTargetIsBum = false,
                    handTargetNumber = state.currentNumber
                )
            }
        }
    }

    private fun planBot(session: Int) {
        val g = _game.value
        val diff = _prefs.value.diff
        val acc = diff.acc - min(0.10f, g.level * 0.003f)
        val ok = Random.nextFloat() < acc
        botFireAtMs = (turnDurationMs * (0.30f + Random.nextFloat() * 0.40f)).toLong()
        botAnswer = if (ok) {
            if (g.isBoom) GameAnswer.Boom else GameAnswer.Num(g.currentNumber)
        } else {
            val r = Random.nextFloat()
            when {
                r < 0.30f -> GameAnswer.None
                !g.isBoom && r < 0.60f -> GameAnswer.Boom
                else -> {
                    val wrongNums = g.padButtons.map { it.number }.filter { it != g.currentNumber }
                    if (wrongNums.isNotEmpty()) GameAnswer.Num(wrongNums.random()) else GameAnswer.None
                }
            }
        }
        botJob?.cancel()
        if (botAnswer != GameAnswer.None) {
            botJob = viewModelScope.launch {
                delay(botFireAtMs)
                if (session == gameSessionId && _game.value.phase == GamePhase.TURN) {
                    sound.pop()
                    resolveAnswer(session, botAnswer)
                }
            }
        }
    }

    fun onHumanTapNumber(number: Int) {
        val g = _game.value
        if (_screen.value != ScreenRoute.GAME || g.phase != GamePhase.TURN) return
        val p = g.players.getOrNull(g.turn) ?: return
        if (p.type != SlotType.HUMAN) return
        if (SystemClock.elapsedRealtime() - turnStartMs < 60L) return
        resolveAnswer(gameSessionId, GameAnswer.Num(number))
    }

    fun onHumanTapBum() {
        val g = _game.value
        if (_screen.value != ScreenRoute.GAME || g.phase != GamePhase.TURN) return
        val p = g.players.getOrNull(g.turn) ?: return
        if (p.type != SlotType.HUMAN) return
        if (SystemClock.elapsedRealtime() - turnStartMs < 60L) return
        resolveAnswer(gameSessionId, GameAnswer.Boom)
    }

    private fun resolveAnswer(session: Int, ans: GameAnswer) {
        val g = _game.value
        if (session != gameSessionId || g.phase != GamePhase.TURN) return
        val p = g.players.getOrNull(g.turn) ?: return
        val rt = (SystemClock.elapsedRealtime() - turnStartMs).toFloat()

        timerJob?.cancel()
        botJob?.cancel()

        val ok = if (g.isBoom) {
            ans is GameAnswer.Boom
        } else {
            ans is GameAnswer.Num && ans.n == g.currentNumber
        }

        if (ok) {
            onOk(session, p, rt)
        } else {
            onFail(session, p, ans)
        }
    }

    private fun onOk(session: Int, p: PlayerState, rt: Float) {
        val g = _game.value
        val speed = max(0, ((1f - rt / turnDurationMs.toFloat()) * 10f).roundToInt())
        val pts = 10 + min(p.combo, 10) * 2 + speed + (if (g.isBoom) 5 else 0)
        val newCombo = p.combo + 1
        val newBestCombo = max(p.bestCombo, newCombo)
        val newScore = p.score + pts
        val newCorrect = p.correct + 1

        val updatedPlayers = g.players.mapIndexed { idx, q ->
            if (idx == g.turn) q.copy(combo = newCombo, bestCombo = newBestCombo, score = newScore, correct = newCorrect)
            else q
        }

        val updatedWall = g.wallCells.toMutableList()
        if (g.levelCount in updatedWall.indices) {
            updatedWall[g.levelCount] = WallCell(
                kind = if (g.isBoom) CellKind.BOOM else CellKind.NUMBER,
                number = g.currentNumber,
                colorIndex = Random.nextInt(0, 8)
            )
        }

        val updatedPad = if (!g.isBoom) {
            g.padButtons.map { btn ->
                val cleared = btn.copy(isHint = false)
                if (btn.number == g.currentNumber) cleared.copy(feedback = PadFeedback.OK) else cleared
            }
        } else {
            g.padButtons.map { it.copy(isHint = false) }
        }

        if (g.isBoom) {
            sound.boom()
            vibrate(longArrayOf(0, 30))
            emitParticles(ParticleTarget.WALL_GOOD)
        } else {
            sound.ok(newCombo)
            emitParticles(ParticleTarget.PAD_BUTTON, g.currentNumber)
        }

        val newLevelCount = g.levelCount + 1
        val newTotalCount = g.totalCount + 1
        val (lastS, nextS) = computeStacks(g.from, newLevelCount, g.levelLen)

        val callouts = mutableListOf<FloatingCallout>()
        callouts.add(FloatingCallout(++calloutIdSeq, g.turn, "+$pts", false))
        if (newCombo == 5 || newCombo == 10 || newCombo == 20) {
            callouts.add(FloatingCallout(++calloutIdSeq, g.turn, "COMBO ×$newCombo!", false))
        }

        val reason = why(g.currentNumber, g.magic)
        val newMsg = if (g.isBoom) {
            GameMessage(prefix = "💥 ", boldPart = "BUMMM!", suffix = " $reason. +$pts", tone = MessageTone.GOOD)
        } else {
            GameMessage(prefix = "✔ ", boldPart = "${g.currentNumber}", suffix = " is safe. +$pts", tone = MessageTone.GOOD)
        }

        _game.update {
            it.copy(
                phase = GamePhase.RESOLVE,
                players = updatedPlayers,
                wallCells = updatedWall,
                padButtons = updatedPad,
                padLocked = true,
                bumFeedback = if (g.isBoom) PadFeedback.OK else PadFeedback.NONE,
                bumHint = false,
                showHandHint = false,
                burstBanner = if (g.isBoom) BurstBanner(++bannerIdSeq, false, "BUMMM!") else it.burstBanner,
                levelCount = newLevelCount,
                totalCount = newTotalCount,
                lastStack = lastS,
                nextStack = nextS,
                ptsPulseCounter = it.ptsPulseCounter + 1,
                comboPulseCounter = it.comboPulseCounter + 1,
                message = newMsg,
                floatingCallouts = (it.floatingCallouts + callouts).takeLast(10)
            )
        }

        scheduleAction(session, 700L) {
            val curState = _game.value
            if (curState.levelCount >= curState.levelLen) {
                levelUp(session)
            } else {
                nextTurn(session)
            }
        }
    }

    private fun onFail(session: Int, p: PlayerState, ans: GameAnswer) {
        val g = _game.value
        val isLate = ans is GameAnswer.None
        val wrongTappedNum = (ans as? GameAnswer.Num)?.n
        val newLives = p.lives - 1
        val newWrong = p.wrong + 1

        val updatedPlayers = g.players.mapIndexed { idx, q ->
            if (idx == g.turn) q.copy(
                lives = newLives,
                combo = 0,
                wrong = newWrong,
                shakeKey = q.shakeKey + 1,
                brokenHeartIndex = max(0, newLives)
            ) else q
        }

        val updatedWall = g.wallCells.toMutableList()
        if (g.levelCount in updatedWall.indices) {
            updatedWall[g.levelCount] = WallCell(kind = CellKind.FAIL, number = g.currentNumber)
        }

        val updatedPad = g.padButtons.map { btn ->
            var b = btn.copy(isHint = false)
            if (wrongTappedNum != null && btn.number == wrongTappedNum) {
                b = b.copy(feedback = PadFeedback.NO)
            }
            if (!g.isBoom && btn.number == g.currentNumber) {
                b = b.copy(isHint = true)
            }
            b
        }

        sound.wrong()
        vibrate(longArrayOf(0, 60, 40, 100))
        emitParticles(ParticleTarget.WALL_BAD)

        val newLevelCount = g.levelCount + 1
        val newTotalCount = g.totalCount + 1
        val (lastS, nextS) = computeStacks(g.from, newLevelCount, g.levelLen)
        val reason = why(g.currentNumber, g.magic)
        val boomSuffix = if (g.isBoom) ": that is BUMMM!" else ": that is safe."
        val newMsg = if (isLate) {
            GameMessage(
                prefix = "⏰ ",
                boldPart = p.name,
                suffix = " was too slow! $reason$boomSuffix",
                tone = MessageTone.BAD
            )
        } else {
            GameMessage(
                prefix = "😵 Oops, ",
                boldPart = p.name,
                suffix = "! $reason$boomSuffix",
                tone = MessageTone.BAD
            )
        }

        _game.update {
            it.copy(
                phase = GamePhase.RESOLVE,
                players = updatedPlayers,
                wallCells = updatedWall,
                padButtons = updatedPad,
                padLocked = true,
                bumFeedback = if (ans is GameAnswer.Boom) PadFeedback.NO else PadFeedback.NONE,
                bumHint = g.isBoom,
                showHandHint = false,
                burstBanner = BurstBanner(++bannerIdSeq, true, if (isLate) "TOO SLOW!" else "OOPS!"),
                flashRedCounter = it.flashRedCounter + 1,
                shakeCounter = it.shakeCounter + 1,
                levelCount = newLevelCount,
                totalCount = newTotalCount,
                lastStack = lastS,
                nextStack = nextS,
                message = newMsg,
                floatingCallouts = (it.floatingCallouts + FloatingCallout(++calloutIdSeq, g.turn, "−❤️", true)).takeLast(10)
            )
        }

        val playerOut = newLives <= 0
        val outPlayerIdx = g.turn

        scheduleAction(session, 1500L) {
            val cur = _game.value
            if (playerOut) {
                sound.out()
                val afterOutPlayers = cur.players.mapIndexed { idx, q ->
                    if (idx == outPlayerIdx) {
                        val elim = q.copy(alive = false)
                        outOrder.add(elim)
                        elim
                    } else q
                }
                _game.update {
                    it.copy(
                        players = afterOutPlayers,
                        turn = -1,
                        floatingCallouts = (it.floatingCallouts + FloatingCallout(++calloutIdSeq, outPlayerIdx, "OUT!", true)).takeLast(10)
                    )
                }
            }

            val latest = _game.value
            val alivePlayers = latest.players.filter { it.alive }
            val aliveHumans = alivePlayers.count { it.type == SlotType.HUMAN }
            val isOver = if (latest.players.size == 1) {
                !latest.players[0].alive
            } else {
                alivePlayers.size <= 1 || aliveHumans == 0
            }

            if (isOver) {
                scheduleAction(session, 900L) { endGame(session) }
            } else if (latest.levelCount >= latest.levelLen) {
                levelUp(session)
            } else {
                scheduleAction(session, if (playerOut) 800L else 200L) {
                    nextTurn(session)
                }
            }
        }
    }

    private fun levelUp(session: Int) {
        if (session != gameSessionId) return
        val g = _game.value
        val newLevel = g.level + 1
        val k = magicFor(newLevel)
        val len = levelLenFor(newLevel)
        val from = levelStart(newLevel, k, len)
        val (lastS, nextS) = computeStacks(from, 0, len)

        val bonusPlayers = g.players.map { q ->
            if (q.alive) q.copy(score = q.score + 20) else q
        }

        if (newLevel > _prefs.value.bestLevel) {
            updatePrefs { it.copy(bestLevel = newLevel) }
        }
        sound.level()
        emitParticles(ParticleTarget.CONFETTI)

        _game.update {
            it.copy(
                phase = GamePhase.LEVEL_UP,
                players = bonusPlayers,
                turn = -1,
                level = newLevel,
                magic = k,
                levelLen = len,
                from = from,
                levelCount = 0,
                currentNumber = from,
                wallCells = List(42) { WallCell() },
                lastStack = lastS,
                nextStack = nextS,
                badgeNowPulseCounter = it.badgeNowPulseCounter + 1,
                badgeMagicPulseCounter = it.badgeMagicPulseCounter + 1,
                countdownBanner = CountdownBanner(
                    id = ++bannerIdSeq,
                    bigText = "LEVEL $newLevel",
                    smallText = "New magic number: $k! BUMMM = multiple of $k or contains $k. Start at $from. +20 each!",
                    durationMs = 2600,
                    isLevelBanner = true
                ),
                message = GameMessage(
                    prefix = "🪜 ",
                    boldPart = "Level $newLevel",
                    suffix = ": magic number ",
                    boldSecond = "$k",
                    tone = MessageTone.GOOD
                )
            )
        }

        scheduleAction(session, 2700L) {
            _game.update { it.copy(countdownBanner = null) }
            nextTurn(session)
        }
    }

    private fun endGame(session: Int) {
        if (session != gameSessionId) return
        val g = _game.value
        if (g.phase == GamePhase.OVER) return

        timerJob?.cancel()
        scheduledJob?.cancel()
        botJob?.cancel()
        countdownJob?.cancel()

        val aliveSorted = g.players.filter { it.alive }.sortedByDescending { it.score }
        val ranks = aliveSorted + outOrder.reversed()
        val topScore = g.players.maxOfOrNull { it.score } ?: 0
        var isNewBest = false
        updatePrefs { pref ->
            val bestLv = max(pref.bestLevel, g.level)
            val bestSc = if (topScore > pref.bestScore) {
                isNewBest = true
                topScore
            } else pref.bestScore
            pref.copy(bestLevel = bestLv, bestScore = bestSc)
        }

        sound.boom()
        emitParticles(ParticleTarget.WALL_END)

        _game.update {
            it.copy(
                phase = GamePhase.OVER,
                turn = -1,
                showHandHint = false,
                shakeCounter = it.shakeCounter + 1,
                rankedResults = ranks,
                newBest = isNewBest
            )
        }

        scheduleAction(session, 1100L) {
            _screen.value = ScreenRoute.RESULT
            sound.win()
            emitParticles(ParticleTarget.CONFETTI)
        }
    }

    /* ============ Pause & Resume ============ */
    fun pauseGame() {
        val g = _game.value
        if (_screen.value != ScreenRoute.GAME) return
        val pausable = g.phase == GamePhase.TURN || g.phase == GamePhase.READY || g.phase == GamePhase.RESOLVE
        if (!pausable) return

        prePausePhase = g.phase
        if (g.phase == GamePhase.TURN) {
            turnElapsedOnPause = SystemClock.elapsedRealtime() - turnStartMs
            timerJob?.cancel()
            botJob?.cancel()
        }
        scheduledJob?.cancel()
        _game.update { it.copy(phase = GamePhase.PAUSED) }
        _showPause.value = true
    }

    fun resumeGame() {
        val g = _game.value
        if (g.phase != GamePhase.PAUSED) return
        sound.click()
        _showPause.value = false
        val session = gameSessionId

        runCountdown(session, listOf("3", "2", "1"), 500, "") {
            _game.update { it.copy(phase = prePausePhase) }
            if (prePausePhase == GamePhase.TURN) {
                turnStartMs = SystemClock.elapsedRealtime() - turnElapsedOnPause
                startTimerLoop(session)
                val p = _game.value.players.getOrNull(_game.value.turn)
                if (p?.type == SlotType.BOT && botAnswer != GameAnswer.None) {
                    val remainingBotDelay = max(60L, botFireAtMs - turnElapsedOnPause)
                    botJob?.cancel()
                    botJob = viewModelScope.launch {
                        delay(remainingBotDelay)
                        if (session == gameSessionId && _game.value.phase == GamePhase.TURN) {
                            sound.pop()
                            resolveAnswer(session, botAnswer)
                        }
                    }
                }
            } else {
                val pending = pendingAfterResume
                pendingAfterResume = null
                pending?.invoke()
            }
        }
    }

    private fun scheduleAction(session: Int, delayMs: Long, action: () -> Unit) {
        scheduledJob?.cancel()
        pendingAfterResume = action
        scheduledJob = viewModelScope.launch {
            delay(delayMs)
            val fn = pendingAfterResume
            pendingAfterResume = null
            if (session == gameSessionId) {
                fn?.invoke()
            }
        }
    }

    private fun cancelAllCoroutines() {
        timerJob?.cancel()
        scheduledJob?.cancel()
        botJob?.cancel()
        countdownJob?.cancel()
        pendingAfterResume = null
    }
}
