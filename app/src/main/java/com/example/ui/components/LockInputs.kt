package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DangerRed
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.OverlayDotIdle
import com.example.ui.theme.OverlayLineActive
import kotlin.math.hypot

/**
 * Interactive 3x3 pattern grid supporting both light setup mode and dark overlay mode.
 * Automatically connects skipped intermediate collinear nodes (e.g., 0 -> 2 connects 1).
 */
@Composable
fun PatternLockPad(
    modifier: Modifier = Modifier,
    darkMode: Boolean = false,
    enabled: Boolean = true,
    isError: Boolean = false,
    onPatternComplete: (List<Int>) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val selectedNodes = remember { mutableStateListOf<Int>() }
    var pointerOffset by remember { mutableStateOf<Offset?>(null) }

    val lineColor = when {
        isError -> DangerRed
        darkMode -> OverlayLineActive
        else -> EmeraldPrimary
    }
    val idleDotColor = if (darkMode) OverlayDotIdle.copy(alpha = 0.9f) else Color(0xFF94A198)
    val activeDotColor = when {
        isError -> DangerRed
        darkMode -> OverlayLineActive
        else -> EmeraldPrimary
    }

    Canvas(
        modifier = modifier
            .widthIn(max = 310.dp)
            .fillMaxWidth()
            .aspectRatio(1f)
            .testTag("pattern_lock_pad")
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput

                fun nodeCenter(index: Int): Offset {
                    val col = index % 3
                    val row = index / 3
                    val padding = size.width * 0.16f
                    val step = (size.width - padding * 2f) / 2f
                    return Offset(padding + col * step, padding + row * step)
                }

                val hitRadius = size.width * 0.125f

                fun hitTest(pos: Offset): Int? {
                    for (i in 0..8) {
                        val c = nodeCenter(i)
                        if (hypot(pos.x - c.x, pos.y - c.y) <= hitRadius) {
                            return i
                        }
                    }
                    return null
                }

                fun addNodeWithIntermediate(next: Int) {
                    if (next in selectedNodes) return
                    val prev = selectedNodes.lastOrNull()
                    if (prev != null) {
                        val mid = intermediateNode(prev, next)
                        if (mid != null && mid !in selectedNodes) {
                            selectedNodes.add(mid)
                        }
                    }
                    selectedNodes.add(next)
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }

                detectDragGestures(
                    onDragStart = { startOffset ->
                        selectedNodes.clear()
                        pointerOffset = startOffset
                        hitTest(startOffset)?.let(::addNodeWithIntermediate)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val pos = change.position
                        pointerOffset = pos
                        hitTest(pos)?.let(::addNodeWithIntermediate)
                    },
                    onDragEnd = {
                        pointerOffset = null
                        if (selectedNodes.isNotEmpty()) {
                            val result = selectedNodes.toList()
                            selectedNodes.clear()
                            onPatternComplete(result)
                        }
                    },
                    onDragCancel = {
                        pointerOffset = null
                        selectedNodes.clear()
                    }
                )
            }
    ) {
        val padding = size.width * 0.16f
        val step = (size.width - padding * 2f) / 2f

        fun centerOf(index: Int): Offset {
            val col = index % 3
            val row = index / 3
            return Offset(padding + col * step, padding + row * step)
        }

        // Draw connected segments
        for (i in 0 until selectedNodes.size - 1) {
            drawLine(
                color = lineColor,
                start = centerOf(selectedNodes[i]),
                end = centerOf(selectedNodes[i + 1]),
                strokeWidth = size.width * 0.024f,
                cap = StrokeCap.Round
            )
        }

        // Draw trailing line to finger position
        val last = selectedNodes.lastOrNull()
        val pointer = pointerOffset
        if (last != null && pointer != null) {
            drawLine(
                color = lineColor.copy(alpha = 0.72f),
                start = centerOf(last),
                end = pointer,
                strokeWidth = size.width * 0.022f,
                cap = StrokeCap.Round
            )
        }

        // Draw 9 dots
        val idleRadius = if (darkMode) size.width * 0.022f else size.width * 0.026f
        val activeRadius = size.width * 0.042f
        val haloRadius = size.width * 0.085f

        for (i in 0..8) {
            val center = centerOf(i)
            val isSelected = i in selectedNodes
            if (isSelected) {
                drawCircle(
                    color = activeDotColor.copy(alpha = 0.22f),
                    radius = haloRadius,
                    center = center
                )
                drawCircle(
                    color = activeDotColor,
                    radius = activeRadius,
                    center = center
                )
            } else {
                drawCircle(
                    color = idleDotColor,
                    radius = idleRadius,
                    center = center
                )
            }
        }
    }
}

private fun intermediateNode(a: Int, b: Int): Int? {
    val r1 = a / 3
    val c1 = a % 3
    val r2 = b / 3
    val c2 = b % 3
    val dr = r2 - r1
    val dc = c2 - c1
    return if (dr % 2 == 0 && dc % 2 == 0 && (dr != 0 || dc != 0)) {
        val mr = r1 + dr / 2
        val mc = c1 + dc / 2
        mr * 3 + mc
    } else {
        null
    }
}

@Composable
fun PinLockPad(
    pin: String,
    maxLength: Int = 6,
    minLength: Int = 4,
    darkMode: Boolean = false,
    enabled: Boolean = true,
    isError: Boolean = false,
    showBiometricKey: Boolean = false,
    onDigitPress: (Char) -> Unit,
    onBackspace: () -> Unit,
    onBiometricClick: (() -> Unit)? = null,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val activeDotColor = when {
        isError -> DangerRed
        darkMode -> EmeraldLight
        else -> EmeraldPrimary
    }
    val emptyDotBorder = if (darkMode) Color.White.copy(alpha = 0.45f) else Color(0xFFBCC6BF)
    val keyBg = if (darkMode) Color.White.copy(alpha = 0.12f) else Color(0xFFF2F6F3)
    val keyBorder = if (darkMode) Color.White.copy(alpha = 0.18f) else Color(0xFFE1E8E3)
    val keyText = if (darkMode) Color.White else Color(0xFF141E17)

    Column(
        modifier = modifier.widthIn(max = 320.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // PIN Dot indicators
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            repeat(maxLength) { index ->
                val filled = index < pin.length
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (filled) activeDotColor else Color.Transparent)
                        .border(
                            width = 2.dp,
                            color = if (filled) activeDotColor else emptyDotBorder,
                            shape = CircleShape
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9")
        )

        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { digit ->
                    PinCircleButton(
                        enabled = enabled,
                        background = keyBg,
                        border = keyBorder,
                        testTag = "pin_key_$digit",
                        onClick = {
                            if (pin.length < maxLength) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDigitPress(digit[0])
                            }
                        }
                    ) {
                        Text(
                            text = digit,
                            style = MaterialTheme.typography.headlineSmall,
                            color = keyText,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Bottom row: Clear | 0 | Backspace
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PinCircleButton(
                enabled = enabled && pin.isNotEmpty(),
                background = Color.Transparent,
                border = Color.Transparent,
                testTag = "pin_clear_key",
                onClick = {
                    repeat(pin.length) { onBackspace() }
                }
            ) {
                Text(
                    text = "Clear",
                    style = MaterialTheme.typography.labelLarge,
                    color = keyText.copy(alpha = if (pin.isNotEmpty()) 0.85f else 0.35f)
                )
            }

            PinCircleButton(
                enabled = enabled,
                background = keyBg,
                border = keyBorder,
                testTag = "pin_key_0",
                onClick = {
                    if (pin.length < maxLength) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDigitPress('0')
                    }
                }
            ) {
                Text(
                    text = "0",
                    style = MaterialTheme.typography.headlineSmall,
                    color = keyText,
                    fontWeight = FontWeight.SemiBold
                )
            }

            PinCircleButton(
                enabled = enabled && pin.isNotEmpty(),
                background = Color.Transparent,
                border = Color.Transparent,
                testTag = "pin_backspace_key",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onBackspace()
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Backspace,
                    contentDescription = "Backspace",
                    tint = keyText.copy(alpha = if (pin.isNotEmpty()) 0.9f else 0.35f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        androidx.compose.material3.Button(
            onClick = onSubmit,
            enabled = enabled && pin.length >= minLength,
            shape = CircleShape,
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = EmeraldPrimary,
                contentColor = Color.White,
                disabledContainerColor = if (darkMode) Color.White.copy(alpha = 0.14f) else Color(0xFFE1E8E3),
                disabledContentColor = if (darkMode) Color.White.copy(alpha = 0.45f) else Color(0xFF8C9990)
            ),
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .height(48.dp)
                .testTag("pin_submit_button")
        ) {
            Text(
                text = if (showBiometricKey) "Unlock" else "Continue",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun PinCircleButton(
    enabled: Boolean,
    background: Color,
    border: Color,
    testTag: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(background)
            .then(
                if (border != Color.Transparent) {
                    Modifier.border(1.dp, border, CircleShape)
                } else {
                    Modifier
                }
            )
            .testTag(testTag)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
