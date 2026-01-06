package com.example.eventfinder.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.delay

@Composable
fun MarqueeText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified
) {
    var shouldAnimate by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    LaunchedEffect(text) {
        delay(1000) // Initial delay
        shouldAnimate = true
        while (shouldAnimate) {
            scrollState.animateScrollTo(
                scrollState.maxValue,
                animationSpec = tween(
                    durationMillis = text.length * 100,
                    easing = LinearEasing
                )
            )
            delay(1000)
            scrollState.scrollTo(0)
            delay(1000)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
    ) {
        Text(
            text = text,
            style = style,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.horizontalScroll(scrollState, enabled = false)
        )
    }
}