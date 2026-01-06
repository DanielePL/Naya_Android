package com.example.menotracker.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.menotracker.ui.theme.Inter
import com.example.menotracker.ui.theme.NayaTheme
import androidx.compose.foundation.BorderStroke

@Composable
fun SearchScreen(navController: NavController) {
    Column(modifier = Modifier.fillMaxSize()) {
        SearchBar()
        FilterChips()
        ExerciseGrid(navController = navController)
    }
}

@Composable
private fun SearchBar() {
    var isFocused by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(28.dp)
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
            Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.width(8.dp))
            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState -> isFocused = focusState.isFocused },
                placeholder = { Text("Search exercises, programs...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 16.sp) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontFamily = Inter)
            )
        }
    }
}

@Composable
private fun FilterChips() {
    val filters = listOf("All", "Bodybuilding", "Powerlifting", "Weightlifting", "CrossFit")
    var selectedChip by remember { mutableStateOf("All") }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filters) {
            filter ->
            val isSelected = selectedChip == filter
            Chip(label = filter, isSelected = isSelected, onSelected = { selectedChip = filter })
        }
    }
}

@Composable
private fun Chip(label: String, isSelected: Boolean, onSelected: () -> Unit) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val contentColor = MaterialTheme.colorScheme.onSurface
    val border = if (isSelected) BorderStroke(0.dp, Color.Transparent) else BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)

    Surface(
        modifier = Modifier.clickable(onClick = onSelected),
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor,
        border = border,
    ) {
        Text(
            text = label,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
        )
    }
}

@Composable
private fun ExerciseGrid(navController: NavController) {
    val items = (1..10).toList()
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(items) {
            ExerciseGridItem(navController = navController)
        }
    }
}

@Composable
private fun ExerciseGridItem(navController: NavController) {
    Card(
        modifier = Modifier
            .aspectRatio(3f / 4f)
            .clickable { navController.navigate("exerciseDetail") },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Placeholder for image
            Box(modifier = Modifier.fillMaxWidth().weight(1f).background(Color.DarkGray).clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)))
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.Start)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("LEGS", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold))
                }
            }
            Column(modifier = Modifier.align(Alignment.End).padding(16.dp).fillMaxHeight(), verticalArrangement = Arrangement.Bottom) {
                Text("Barbell Squat", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
                Spacer(modifier = Modifier.height(4.dp))
                Text("Beginner • 15 min", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F0F)
@Composable
private fun SearchScreenPreview() {
    NayaTheme {
        val navController = rememberNavController()
        SearchScreen(navController)
    }
}
