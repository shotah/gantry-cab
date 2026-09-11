package com.gantree.cab.mailbox

/** Mailbox order on a thread bubble. Seq first, then `at`, then id. */
interface ThreadOrder {
  val id: String
  val at: Long
  val seq: Int?
  val kind: String?
}

data class ThreadCursor(
  val id: String? = null,
  val seq: Int = 0,
)

const val THREAD_MAX = 80
const val SEEN_MAX = 200

fun <T> capThread(messages: List<T>, max: Int = THREAD_MAX): List<T> {
  return if (messages.size > max) messages.takeLast(max) else messages
}

fun isDraftBubble(kind: String?): Boolean = kind == "draft"

/** Seq first, then mailbox time, then id. Drafts stay last so catch-up cannot leapfrog typing. */
fun compareThread(a: ThreadOrder, b: ThreadOrder): Int {
  val aDraft = isDraftBubble(a.kind)
  val bDraft = isDraftBubble(b.kind)
  if (aDraft != bDraft) {
    return if (aDraft) 1 else -1
  }
  val aSeq = a.seq
  val bSeq = b.seq
  if (aSeq != null && bSeq != null && aSeq != bSeq) {
    return aSeq.compareTo(bSeq)
  }
  if (a.at != b.at) {
    return a.at.compareTo(b.at)
  }
  return a.id.compareTo(b.id)
}

fun <T : ThreadOrder> placeInThread(messages: List<T>, bubble: T): List<T> {
  val next = messages.filter { it.id != bubble.id } + bubble
  return next.sortedWith(::compareThread)
}

fun advanceCursor(current: ThreadCursor, id: String? = null, seq: Int? = null): ThreadCursor {
  if (seq != null && seq >= current.seq) {
    return ThreadCursor(id = id ?: current.id, seq = seq)
  }
  if (seq == null && id != null) {
    return ThreadCursor(id = id, seq = current.seq)
  }
  return current
}

/**
 * Only queued turns advance `since`. An `ack` echoes our own id; an `error`
 * names the frame the mailbox refused — neither is a place to resume from.
 */
fun movesCursor(kind: String?): Boolean = kind != "ack" && kind != "error"

fun ackSince(cursor: ThreadCursor): String? {
  if (cursor.seq > 0) {
    return cursor.seq.toString()
  }
  return cursor.id
}

fun rememberSeen(seen: LinkedHashSet<String>, id: String, max: Int = SEEN_MAX) {
  seen.add(id)
  if (seen.size <= max) {
    return
  }
  val extra = seen.size - max
  repeat(extra) {
    val first = seen.firstOrNull() ?: return
    seen.remove(first)
  }
}
