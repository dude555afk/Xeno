package com.dude555afk.xeno.ui.corebrowser

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SheetColor = Color(0xFF2D2E32)
private val SearchPillColor = Color(0xFF4F5052)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBottomSheet(
    s: Float,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.imePadding(),
        containerColor = SheetColor,
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = (44f * s).dp, topEnd = (44f * s).dp),
        dragHandle = {}
    ) {
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
            keyboard?.show()
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = (22f * s).dp, vertical = (18f * s).dp)
                .height((68f * s).dp)
                .background(SearchPillColor, RoundedCornerShape((34f * s).dp))
                .padding(horizontal = (18f * s).dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy((8f * s).dp)
        ) {
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = (17f * s).sp),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.White),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        val value = query.trim()
                        if (value.isNotEmpty()) {
                            keyboard?.hide()
                            onSubmit(resolveSearchTarget(value))
                        }
                    }
                ),
                decorationBox = { inner ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                "Search...",
                                color = Color.White.copy(alpha = 0.62f),
                                fontSize = (17f * s).sp,
                                modifier = Modifier.padding(start = (4f * s).dp)
                            )
                        }
                        inner()
                    }
                }
            )
            if (query.isNotEmpty()) {
                IconButton(onClick = { query = "" }) {
                    Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = Color.White)
                }
            } else {
                Icon(Icons.Rounded.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.72f))
            }
        }
    }
}

fun resolveSearchTarget(input: String): String {
    val text = input.trim()
    if (text.isEmpty()) return ""
    val candidate = if (text.startsWith("http://", true) || text.startsWith("https://", true)) {
        text
    } else {
        "https://" + text
    }
    val uri = runCatching { Uri.parse(candidate) }.getOrNull()
    val host = uri?.host
    val looksLikeUrl = !text.contains(' ') &&
        uri?.scheme in listOf("http", "https") &&
        !host.isNullOrBlank() &&
        (host.contains('.') || host == "localhost" || host.startsWith("192.168.") || host.startsWith("10."))
    return if (looksLikeUrl) candidate else {
        "https://duckduckgo.com/?q=" + Uri.encode(text)
    }
}
