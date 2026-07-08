package com.biewangle.dontforget.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biewangle.dontforget.ui.theme.CardWhite
import com.biewangle.dontforget.ui.theme.ChipSelected
import com.biewangle.dontforget.ui.theme.ChipUnselected
import com.biewangle.dontforget.ui.theme.TextDarkBrown
import com.biewangle.dontforget.ui.theme.WhiteText
import com.biewangle.dontforget.ui.theme.scaledSp
import com.biewangle.dontforget.util.Constants
import com.biewangle.dontforget.util.TemplateItem

@Composable
fun QuickTemplateChipRow(
    onTemplateClick: (TemplateItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(vertical = 4.dp)
            .background(CardWhite, RoundedCornerShape(16.dp))
            .padding(vertical = 12.dp, horizontal = 8.dp)
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            items(Constants.QUICK_TEMPLATES) { template ->
                TemplateChip(
                    template = template,
                    onClick = { onTemplateClick(template) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplateChip(
    template: TemplateItem,
    onClick: () -> Unit
) {
    FilterChip(
        selected = false,
        onClick = onClick,
        label = {
            Text(
                text = template.label,
                fontSize = scaledSp(20),
                fontWeight = FontWeight.Medium,
                color = TextDarkBrown
            )
        },
        modifier = Modifier.height(52.dp),
        shape = RoundedCornerShape(26.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = ChipUnselected,
            selectedContainerColor = ChipSelected,
            labelColor = TextDarkBrown,
            selectedLabelColor = WhiteText
        )
    )
}
