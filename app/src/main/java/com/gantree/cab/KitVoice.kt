package com.gantree.cab

import android.media.AudioAttributes
import android.media.MediaDataSource
import android.media.MediaPlayer
import com.gantree.cab.mailbox.SpeakFail
import com.gantree.cab.mailbox.SpeakPhase
import com.gantree.cab.mailbox.TtsApi
import com.gantree.cab.mailbox.TtsResult
import com.gantree.cab.mailbox.WireFrame
import com.gantree.cab.mailbox.clipForSpeech
import com.gantree.cab.mailbox.disarmsVoice
import com.gantree.cab.mailbox.speakFailHint
import com.gantree.cab.mailbox.speakable
import com.gantree.cab.mailbox.speaksReply
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Kit's pocket voice. The live `reply` to a hold this phone armed goes
 * `speakable()` → `POST /api/tts` (Bearer) → MP3 → [MediaPlayer]. Android Auto
 * keeps the host engine on the HUN; nothing here runs for the car. Pendant
 * `app/lib/tts.ts` plus the `awaitingVoice` gate in `PhoneShell`.
 *
 * Player calls stay on the main thread ([scope]); [heard] arrives on the
 * socket thread and only flips the gate before hopping over.
 */
class KitVoice(
  private val tts: TtsApi,
  private val origin: () -> String,
  private val bearer: () -> String,
  private val hint: (String) -> Unit,
  private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) {
  private val _phase = MutableStateFlow(SpeakPhase.IDLE)
  val phase: StateFlow<SpeakPhase> = _phase
  @Volatile
  private var armed = false
  private var player: MediaPlayer? = null
  private var job: Job? = null

  /** A hold just sent: the next live reply is read aloud, then this disarms. */
  fun arm() {
    armed = true
  }

  /** Every mailbox frame passes here after `Mouth.ingest`; `fresh` is its answer. */
  fun heard(frame: WireFrame, fresh: Boolean) {
    if (disarmsVoice(frame.kind)) {
      armed = false
    }
    if (!speaksReply(frame.kind, frame.replay, fresh, armed)) {
      return
    }
    armed = false
    speak(frame.text.orEmpty())
  }

  /** Hold pressed while Kit is mid-sentence: quiet the speaker so the mic does not hear Kit. */
  fun hush() {
    scope.launch {
      job?.cancel()
      job = null
      stopPlayer()
      _phase.value = SpeakPhase.IDLE
    }
  }

  private fun speak(markdown: String) {
    val text = clipForSpeech(speakable(markdown))
    scope.launch {
      job?.cancel()
      job = coroutineContext.job
      stopPlayer()
      if (text.isEmpty()) {
        // A markdown-only reply has nothing to say; that is not worth a line.
        _phase.value = SpeakPhase.IDLE
        return@launch
      }
      _phase.value = SpeakPhase.FETCHING
      when (val got = withContext(Dispatchers.IO) { tts.synthesize(origin(), bearer(), text) }) {
        is TtsResult.Err -> fail(got.reason)
        is TtsResult.Ok -> play(got.mp3)
      }
    }
  }

  private fun fail(reason: SpeakFail) {
    _phase.value = SpeakPhase.IDLE
    val why = speakFailHint(reason)
    if (why.isNotEmpty()) {
      hint(why)
    }
  }

  private fun play(mp3: ByteArray) {
    val mp = MediaPlayer()
    player = mp
    try {
      mp.setAudioAttributes(
        AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_ASSISTANT)
          .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
          .build(),
      )
      mp.setDataSource(ClipSource(mp3))
      mp.setOnPreparedListener {
        if (player === mp) {
          it.start()
          _phase.value = SpeakPhase.PLAYING
        }
      }
      mp.setOnCompletionListener {
        if (player === mp) {
          stopPlayer()
          _phase.value = SpeakPhase.IDLE
        }
      }
      mp.setOnErrorListener { _, _, _ ->
        if (player === mp) {
          stopPlayer()
          fail(SpeakFail.PLAY)
        }
        true
      }
      mp.prepareAsync()
    } catch (_: Exception) {
      stopPlayer()
      fail(SpeakFail.PLAY)
    }
  }

  private fun stopPlayer() {
    val mp = player ?: return
    player = null
    runCatching { mp.stop() }
    mp.release()
  }
}

/** The MP3 straight from memory. No temp file; nothing of Kit's voice touches disk. */
private class ClipSource(private val bytes: ByteArray) : MediaDataSource() {
  override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
    if (position >= bytes.size) {
      return -1
    }
    val n = minOf(size, (bytes.size - position).toInt())
    System.arraycopy(bytes, position.toInt(), buffer, offset, n)
    return n
  }

  override fun getSize(): Long = bytes.size.toLong()

  override fun close() {}
}
