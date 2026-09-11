package com.gantree.cab.ui

import com.gantree.cab.ChatLine

/** Newest-first for a reverseLayout thread — index 0 sits on the bottom edge. */
fun threadNewestFirst(lines: List<ChatLine>): List<ChatLine> = lines.asReversed()

/** reverseLayout: index 0 + a small offset means the newest turn is pinned to the bottom. */
fun pinnedToNewest(firstVisibleIndex: Int, firstVisibleOffset: Int, slopPx: Int = 80): Boolean =
  firstVisibleIndex == 0 && firstVisibleOffset <= slopPx

/** Stay on the newest turn while already there, or when the operator just sent. */
fun shouldFollowNewest(pinnedToNewest: Boolean, newestFromYou: Boolean): Boolean =
  pinnedToNewest || newestFromYou
