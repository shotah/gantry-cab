package com.gantree.cab.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.gantree.cab.mailbox.LISTEN_END_MS
import com.gantree.cab.mailbox.Utterance

/**
 * One handheld hold on Android's [SpeechRecognizer]. Press → [begin]; release →
 * [release] (`stopListening`, then the final lands or a 2 s watchdog commits
 * what was heard so the `…` cannot hang); slide off → [abort]. Google's engine
 * ends an utterance on silence, so while the button is still down it is started
 * again and a pause is not the end of the turn. The words go to `onDone` once.
 *
 * Never used on the Auto template: the host already did STT there
 * (pendant docs/voice.md). Main thread only, like the recognizer itself.
 */
class HoldListener(
  private val ctx: Context,
  private val onDone: (String) -> Unit,
  private val onBlocked: () -> Unit,
) {
  private val main = Handler(Looper.getMainLooper())
  private var rec: SpeechRecognizer? = null
  private var utterance: Utterance? = null
  private var held = false
  private val watchdog = Runnable { utterance?.finish() }

  val listening: Boolean
    get() = utterance != null

  /** @return false when this phone has no recognizer at all (nothing to hold). */
  fun begin(): Boolean {
    if (utterance != null) {
      return true
    }
    if (!SpeechRecognizer.isRecognitionAvailable(ctx)) {
      return false
    }
    val u = Utterance { words ->
      teardown()
      onDone(words)
    }
    utterance = u
    held = true
    val r = SpeechRecognizer.createSpeechRecognizer(ctx)
    r.setRecognitionListener(
      object : RecognitionListener {
        override fun onPartialResults(partialResults: Bundle?) {
          partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let(u::partial)
        }

        override fun onResults(results: Bundle?) {
          results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let(u::final)
          if (held) {
            r.startListening(intent())
          } else {
            u.finish()
          }
        }

        override fun onError(error: Int) {
          when {
            error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
              onBlocked()
              u.abort()
            }
            held && (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) ->
              r.startListening(intent())
            else -> u.finish()
          }
        }

        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
      },
    )
    rec = r
    r.startListening(intent())
    return true
  }

  /** Release: let the engine finish the utterance, then commit — by result, or by the watchdog. */
  fun release() {
    if (utterance == null) {
      return
    }
    held = false
    rec?.stopListening()
    main.postDelayed(watchdog, LISTEN_END_MS)
  }

  /** Slide-off cancel ("that was the radio"): drop what was heard. */
  fun abort() {
    held = false
    utterance?.abort()
  }

  private fun teardown() {
    main.removeCallbacks(watchdog)
    rec?.destroy()
    rec = null
    utterance = null
    held = false
  }

  private fun intent(): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
    .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
    .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
    .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    .putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, ctx.packageName)
}
