package com.sdk.translation.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import com.sdk.translation.TranslationSDK

@Composable
fun TranslatedText(
    rawText: String,
    translationRequired: Boolean = false,
    targetLanguage: String? = null,
    content: @Composable (
        rawText: String,
        translation: String?,
        isTranslationVisible: Boolean,
        onToggleTranslation: () -> Unit
    ) -> Unit
) {
    if (!translationRequired || !TranslationSDK.isInitialized) {
        content(rawText, null, false, {})
        return
    }

    val resolvedLanguage = targetLanguage ?: TranslationSDK.instance.defaultLanguage
    val translationKey = "$resolvedLanguage:${rawText.hashCode()}_${rawText.length}"
    val viewModel = rememberTranslationViewModel(TranslationSDK.instance)
    val translationState = remember(translationKey) {
        viewModel.getTranslationState(translationKey)
    }
    val translation by translationState.collectAsState()
    var isTranslationVisible by remember(translationKey) { mutableStateOf(false) }

    LaunchedEffect(translationKey) {
        viewModel.translate(translationKey, rawText, resolvedLanguage)
    }

    content(
        rawText,
        translation,
        isTranslationVisible
    ) {
        isTranslationVisible = !isTranslationVisible
    }
}

@Composable
fun TranslatedText(
    rawText: String,
    modifier: Modifier = Modifier,
    translationRequired: Boolean = false,
    targetLanguage: String? = null,
    color: Color = Color.Unspecified,
    autoSize: TextAutoSize? = null,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = LocalTextStyle.current,
    seeTranslationText: String = "See translation",
    hideTranslationText: String = "Hide translation"
) {
    TranslatedText(
        rawText = rawText,
        translationRequired = translationRequired,
        targetLanguage = targetLanguage
    ) { currentRawText, translation, isTranslationVisible, onToggleTranslation ->
        if (!translationRequired) {
            StyledText(
                text = currentRawText,
                modifier = modifier,
                color = color,
                autoSize = autoSize,
                fontSize = fontSize,
                fontStyle = fontStyle,
                fontWeight = fontWeight,
                fontFamily = fontFamily,
                letterSpacing = letterSpacing,
                textDecoration = textDecoration,
                textAlign = textAlign,
                lineHeight = lineHeight,
                overflow = overflow,
                softWrap = softWrap,
                maxLines = maxLines,
                minLines = minLines,
                onTextLayout = onTextLayout,
                style = style
            )
        } else {
            TranslatedTextContent(
                rawText = currentRawText,
                translation = translation,
                isTranslationVisible = isTranslationVisible,
                onToggleTranslation = onToggleTranslation,
                modifier = modifier,
                color = color,
                autoSize = autoSize,
                fontSize = fontSize,
                fontStyle = fontStyle,
                fontWeight = fontWeight,
                fontFamily = fontFamily,
                letterSpacing = letterSpacing,
                textDecoration = textDecoration,
                textAlign = textAlign,
                lineHeight = lineHeight,
                overflow = overflow,
                softWrap = softWrap,
                maxLines = maxLines,
                minLines = minLines,
                onTextLayout = onTextLayout,
                style = style,
                seeTranslationText = seeTranslationText,
                hideTranslationText = hideTranslationText
            )
        }
    }
}

@Composable
private fun TranslatedTextContent(
    rawText: String,
    translation: String?,
    isTranslationVisible: Boolean,
    onToggleTranslation: () -> Unit,
    modifier: Modifier,
    color: Color,
    autoSize: TextAutoSize?,
    fontSize: TextUnit,
    fontStyle: FontStyle?,
    fontWeight: FontWeight?,
    fontFamily: FontFamily?,
    letterSpacing: TextUnit,
    textDecoration: TextDecoration?,
    textAlign: TextAlign?,
    lineHeight: TextUnit,
    overflow: TextOverflow,
    softWrap: Boolean,
    maxLines: Int,
    minLines: Int,
    onTextLayout: ((TextLayoutResult) -> Unit)?,
    style: TextStyle,
    seeTranslationText: String,
    hideTranslationText: String
) {
    Row(modifier = modifier.height(IntrinsicSize.Min)) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(3.dp)
                .background(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(2.dp)
                )
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column {
            StyledText(
                text = if (isTranslationVisible) translation ?: rawText else rawText,
                modifier = Modifier,
                color = color,
                autoSize = autoSize,
                fontSize = fontSize,
                fontStyle = fontStyle,
                fontWeight = fontWeight,
                fontFamily = fontFamily,
                letterSpacing = letterSpacing,
                textDecoration = textDecoration,
                textAlign = textAlign,
                lineHeight = lineHeight,
                overflow = overflow,
                softWrap = softWrap,
                maxLines = maxLines,
                minLines = minLines,
                onTextLayout = onTextLayout,
                style = style
            )

            Spacer(modifier = Modifier.height(2.dp))

            TextButton(onClick = onToggleTranslation) {
                Text(if (isTranslationVisible) hideTranslationText else seeTranslationText)
            }
        }
    }
}

@Composable
private fun StyledText(
    text: String,
    modifier: Modifier,
    color: Color,
    autoSize: TextAutoSize?,
    fontSize: TextUnit,
    fontStyle: FontStyle?,
    fontWeight: FontWeight?,
    fontFamily: FontFamily?,
    letterSpacing: TextUnit,
    textDecoration: TextDecoration?,
    textAlign: TextAlign?,
    lineHeight: TextUnit,
    overflow: TextOverflow,
    softWrap: Boolean,
    maxLines: Int,
    minLines: Int,
    onTextLayout: ((TextLayoutResult) -> Unit)?,
    style: TextStyle
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        autoSize = autoSize,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = style
    )
}
