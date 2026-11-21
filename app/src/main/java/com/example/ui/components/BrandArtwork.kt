package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldPrimaryDark
import com.example.ui.theme.MintBlob
import com.example.ui.theme.MintContainer
import com.example.ui.theme.MintSurface
import com.example.ui.theme.WarningAmber

@Composable
fun SoftMintBackdrop(
    modifier: Modifier = Modifier,
    showBottomWave: Boolean = false
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        // Top-right soft organic mint blob
        drawCircle(
            color = MintBlob.copy(alpha = 0.72f),
            radius = size.width * 0.34f,
            center = Offset(size.width * 0.92f, size.height * 0.04f)
        )
        drawCircle(
            color = MintSurface.copy(alpha = 0.6f),
            radius = size.width * 0.2f,
            center = Offset(size.width * 0.72f, size.height * 0.02f)
        )

        if (showBottomWave) {
            val wave = Path().apply {
                moveTo(0f, size.height * 0.9f)
                quadraticTo(
                    size.width * 0.3f,
                    size.height * 0.84f,
                    size.width * 0.62f,
                    size.height * 0.92f
                )
                quadraticTo(
                    size.width * 0.85f,
                    size.height * 0.96f,
                    size.width,
                    size.height * 0.88f
                )
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(path = wave, color = MintBlob.copy(alpha = 0.85f))
        }
    }
}

@Composable
fun ShieldLockHero(
    modifier: Modifier = Modifier,
    size: Dp = 144.dp,
    showCheckBadge: Boolean = false,
    darkBackdrop: Boolean = false
) {
    Box(
        modifier = modifier.size(size * 1.35f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val cx = w / 2f
            val cy = h / 2f

            // Ambient organic mint blobs behind shield
            drawCircle(
                color = if (darkBackdrop) EmeraldPrimary.copy(alpha = 0.18f) else MintBlob,
                radius = w * 0.36f,
                center = Offset(cx - w * 0.14f, cy + h * 0.08f)
            )
            drawCircle(
                color = if (darkBackdrop) EmeraldLight.copy(alpha = 0.14f) else MintContainer.copy(alpha = 0.85f),
                radius = w * 0.28f,
                center = Offset(cx + w * 0.18f, cy - h * 0.08f)
            )

            // Shield dimensions
            val sw = w * 0.56f
            val sh = h * 0.66f
            val left = cx - sw / 2f
            val top = cy - sh / 2f
            val right = cx + sw / 2f
            val bottom = cy + sh / 2f

            val shieldPath = Path().apply {
                moveTo(cx, top)
                lineTo(right, top + sh * 0.16f)
                lineTo(right, top + sh * 0.54f)
                cubicTo(
                    right, top + sh * 0.78f,
                    cx + sw * 0.16f, bottom - sh * 0.05f,
                    cx, bottom
                )
                cubicTo(
                    cx - sw * 0.16f, bottom - sh * 0.05f,
                    left, top + sh * 0.78f,
                    left, top + sh * 0.54f
                )
                lineTo(left, top + sh * 0.16f)
                close()
            }

            // Soft shadow under shield
            drawPath(
                path = shieldPath,
                color = Color.Black.copy(alpha = if (darkBackdrop) 0.3f else 0.08f)
            )

            // Main glossy emerald gradient shield
            drawPath(
                path = shieldPath,
                brush = Brush.linearGradient(
                    colors = listOf(EmeraldLight, EmeraldPrimary, EmeraldPrimaryDark),
                    start = Offset(left, top),
                    end = Offset(right, bottom)
                )
            )

            // Subtle inner highlight border
            drawPath(
                path = shieldPath,
                color = Color.White.copy(alpha = 0.25f),
                style = Stroke(width = w * 0.015f)
            )
        }

        // White padlock centered inside shield
        Icon(
            imageVector = Icons.Rounded.Lock,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(size * 0.42f)
        )

        if (showCheckBadge) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-size * 0.05f))
                    .size(size * 0.26f),
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(size * 0.17f)
                    )
                }
            }
        }
    }
}

@Composable
fun CelebrationCheckBadge(modifier: Modifier = Modifier) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val scale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.72f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 260f),
        label = "celebration_scale"
    )

    Box(
        modifier = modifier
            .size(188.dp)
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = size.minDimension * 0.31f

            // Soft outer halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(MintContainer, MintSurface.copy(alpha = 0.25f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = size.minDimension * 0.46f
                ),
                radius = size.minDimension * 0.46f,
                center = Offset(cx, cy)
            )

            // Confetti dashes around the badge
            val confetti = listOf(
                Triple(Offset(cx - r * 1.45f, cy - r * 0.9f), Offset(cx - r * 1.2f, cy - r * 0.7f), EmeraldPrimary),
                Triple(Offset(cx - r * 0.95f, cy - r * 1.45f), Offset(cx - r * 0.85f, cy - r * 1.15f), WarningAmber),
                Triple(Offset(cx + r * 0.95f, cy - r * 1.4f), Offset(cx + r * 0.8f, cy - r * 1.12f), WarningAmber),
                Triple(Offset(cx + r * 1.45f, cy - r * 0.75f), Offset(cx + r * 1.18f, cy - r * 0.6f), EmeraldLight),
                Triple(Offset(cx - r * 1.55f, cy + r * 0.35f), Offset(cx - r * 1.25f, cy + r * 0.3f), EmeraldLight),
                Triple(Offset(cx + r * 1.5f, cy + r * 0.42f), Offset(cx + r * 1.22f, cy + r * 0.36f), WarningAmber)
            )
            confetti.forEach { (start, end, color) ->
                drawLine(
                    color = color,
                    start = start,
                    end = end,
                    strokeWidth = 6f,
                    cap = StrokeCap.Round
                )
            }

            // Main green badge circle
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(EmeraldLight, EmeraldPrimary),
                    start = Offset(cx - r, cy - r),
                    end = Offset(cx + r, cy + r)
                ),
                radius = r,
                center = Offset(cx, cy)
            )

            // Crisp rounded badge inner ring
            drawRoundRect(
                color = Color.White.copy(alpha = 0.22f),
                topLeft = Offset(cx - r * 0.88f, cy - r * 0.88f),
                size = Size(r * 1.76f, r * 1.76f),
                cornerRadius = CornerRadius(r, r),
                style = Stroke(width = 3f)
            )
        }

        Box(
            modifier = Modifier
                .size(64.dp)
                .background(Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}
