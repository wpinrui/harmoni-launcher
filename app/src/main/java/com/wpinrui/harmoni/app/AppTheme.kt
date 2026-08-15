package com.wpinrui.harmoni.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.wpinrui.harmoni.ui.theme.Accent
import com.wpinrui.harmoni.ui.theme.Karla
import com.wpinrui.harmoni.ui.theme.noRipple

/**
 * The furniture every screen in this package is built from, so they read the same way down the
 * page and a change to one changes all of them.
 */

/** A collapsible section title, with a rule running out to the edge. */
@Composable
fun SectionHeader(title: String, open: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .noRipple(onClick = onToggle)
            .padding(top = 22.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = if (open) "▾" else "▸",
            style = HeaderStyle.copy(color = Accent, fontSize = 14.sp),
        )
        Text(text = title, style = HeaderStyle)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.14f)),
        )
    }
}

/** One card. Tappable when given something to do, inert otherwise. */
@Composable
fun Panel(onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val surface = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .background(PanelColour)

    Column(
        modifier = (if (onClick == null) surface else surface.noRipple(onClick = onClick))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        content = content,
    )
}

/** A label on the left and its value on the right, which is most of what this screen reports. */
@Composable
fun KeyValue(key: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = key, style = BodyStyle, modifier = Modifier.weight(1f))
        Text(text = value, style = ValueStyle)
    }
}

/** A row that opens something, with the action named on the right. */
@Composable
fun ActionPanel(label: String, action: String, onClick: () -> Unit) {
    Panel(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = label, style = ValueStyle, modifier = Modifier.weight(1f))
            Text(text = action, style = ValueStyle.copy(color = Accent))
        }
    }
}

internal val PanelColour = Color(0xFF1C1714)
internal val Live = Color(0xFF9FC9A6)
internal val Dead = Color(0xFFCE8B7F)

internal val HeaderStyle = TextStyle(
    fontFamily = Karla,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    letterSpacing = 0.22.em,
    color = Color(0xFFCFC6BD),
)

internal val BodyStyle = TextStyle(
    fontFamily = Karla,
    fontWeight = FontWeight.Light,
    fontSize = 14.sp,
    lineHeight = 1.4.em,
    color = Color(0xFFB9AFA7),
)

internal val ValueStyle = TextStyle(
    fontFamily = Karla,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    letterSpacing = 0.04.em,
    color = Color.White,
)

internal val NoteStyle = TextStyle(
    fontFamily = Karla,
    fontWeight = FontWeight.Light,
    fontSize = 11.5.sp,
    lineHeight = 1.4.em,
    color = Color(0xFF8B817A),
)
