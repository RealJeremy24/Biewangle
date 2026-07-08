package com.biewangle.dontforget.ui.screens.memo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biewangle.dontforget.ui.theme.scaledSp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.biewangle.dontforget.ui.components.MemoCard
import com.biewangle.dontforget.ui.components.QuickTemplateChipRow
import com.biewangle.dontforget.ui.theme.BackgroundWarm
import com.biewangle.dontforget.ui.theme.CardWhite
import com.biewangle.dontforget.ui.theme.DividerWarm
import com.biewangle.dontforget.ui.theme.PrimaryOrange
import com.biewangle.dontforget.ui.theme.TextDarkBrown
import com.biewangle.dontforget.ui.theme.TextWarmGray
import com.biewangle.dontforget.ui.theme.WhiteText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoScreen(
    navController: NavHostController,
    viewModel: MemoViewModel = viewModel(factory = MemoViewModel.Factory)
) {
    val memoGroups by viewModel.memoGroups.collectAsState()
    val showForm by viewModel.showForm.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(modifier = Modifier.fillMaxSize()) {
        // 主内容
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundWarm),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // 品牌顶部栏 — 暖橙渐变
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(PrimaryOrange, Color(0xFFF5CBA0)),
                                start = Offset(0f, 0f),
                                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                            ),
                            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                        )
                        .padding(horizontal = 20.dp, vertical = 22.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🏠", fontSize = 28.sp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "冲凉最舒适",
                                fontSize = scaledSp(26),
                                fontWeight = FontWeight.Bold,
                                color = WhiteText
                            )
                            Text(
                                "别忘乐 · 生活好帮手",
                                fontSize = scaledSp(14),
                                color = WhiteText.copy(alpha = 0.75f)
                            )
                        }
                    }
                }
            }

            // 快捷模板
            item {
                Text(
                    text = "快捷添加",
                    fontSize = scaledSp(20),
                    fontWeight = FontWeight.Medium,
                    color = TextDarkBrown,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                QuickTemplateChipRow(
                    onTemplateClick = { template ->
                        viewModel.showNewForm()
                        viewModel.applyTemplate(template)
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // 头部分隔线
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(DividerWarm)
                )
            }

            // 按日期分组的事项列表
            if (memoGroups.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "还没有事项\n点击下方按钮添加吧",
                            fontSize = scaledSp(22),
                            color = TextWarmGray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                memoGroups.forEach { group ->
                    // 日期标题
                    item {
                        Text(
                            text = group.dateLabel,
                            fontSize = scaledSp(24),
                            fontWeight = FontWeight.Bold,
                            color = TextDarkBrown,
                            modifier = Modifier.padding(
                                start = 16.dp, end = 16.dp,
                                top = 16.dp, bottom = 8.dp
                            )
                        )
                    }

                    // 事项卡片
                    items(
                        items = group.memos,
                        key = { it.id }
                    ) { memo ->
                        MemoCard(
                            memo = memo,
                            onClick = { viewModel.showEditForm(memo) },
                            onToggleComplete = { viewModel.toggleComplete(memo.id) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // 浮动添加按钮
        FloatingActionButton(
            onClick = { viewModel.showNewForm() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            containerColor = PrimaryOrange,
            contentColor = WhiteText,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "添加事项",
                modifier = Modifier.size(32.dp)
            )
        }
    }

    // 底部弹出表单
    if (showForm) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.hideForm() },
            sheetState = sheetState,
            containerColor = BackgroundWarm
        ) {
            AddMemoSheet(viewModel = viewModel)
        }
    }
}
