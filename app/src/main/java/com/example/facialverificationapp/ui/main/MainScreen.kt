package com.example.facialverificationapp.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.example.facialverificationapp.Register
import com.example.facialverificationapp.Verify

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    viewModel: MainScreenViewModel,
    modifier: Modifier = Modifier
) {
    val securityState by viewModel.securityState.collectAsStateWithLifecycle()

    when (securityState) {
        SecurityState.Checking -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF121212)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Performing system integrity checks...", color = Color.Gray, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    androidx.compose.material3.CircularProgressIndicator(color = Color(0xFF00E676))
                }
            }
        }
        is SecurityState.Failed -> {
            val details = securityState as SecurityState.Failed
            SecurityBlockScreen(isRooted = details.isRooted, isEmulator = details.isEmulator)
        }
        SecurityState.Passed -> {
            DashboardContent(onItemClick = onItemClick, viewModel = viewModel, modifier = modifier)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
    onItemClick: (NavKey) -> Unit,
    viewModel: MainScreenViewModel,
    modifier: Modifier = Modifier
) {
    val registeredUsers by viewModel.registeredUsers.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Datalake 3.0 Verification", fontWeight = FontWeight.Bold, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
            )
        },
        containerColor = Color(0xFF121212)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Dashboard Greeting Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFF2979FF), Color(0xFF00E676))
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Shield",
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Biometric Verification Node",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Completely Offline Identity Engine",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action triggers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { onItemClick(Register) },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1976D2),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                ) {
                    Icon(imageVector = Icons.Default.Face, contentDescription = "Enroll")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enroll Face", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { onItemClick(Verify) },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF388E3C),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                ) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = "Verify")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Verify Identity", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Registered Users List
            Text(
                text = "Registered Offline Identities (${registeredUsers.size})",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            if (registeredUsers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1E1E1E)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No identities registered locally.", color = Color.Gray, textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(registeredUsers) { user ->
                        UserRowItem(userId = user.first, name = user.second) {
                            viewModel.deleteUser(user.first)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserRowItem(
    userId: String,
    name: String,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text(text = "ID: $userId", color = Color.Gray, fontSize = 12.sp)
            }
            IconButton(onClick = onDelete) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFE53935))
            }
        }
    }
}

@Composable
fun SecurityBlockScreen(isRooted: Boolean, isEmulator: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0000)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1010)),
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .border(2.dp, Color(0xFFFF1744), RoundedCornerShape(28.dp))
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Security Breach",
                    tint = Color(0xFFFF1744),
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "SECURITY CHECK FAILED",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF1744),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "This environment violates corporate integrity guidelines for Datalake 3.0.\nOperation is blocked.",
                    fontSize = 14.sp,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                // Details of threat
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(16.dp)
                ) {
                    ThreatRow(label = "Root Privilege Detected", active = isRooted)
                    Spacer(modifier = Modifier.height(8.dp))
                    ThreatRow(label = "Virtual Emulator Environment", active = isEmulator)
                }
            }
        }
    }
}

@Composable
fun ThreatRow(label: String, active: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.Gray, fontSize = 12.sp)
        Text(
            text = if (active) "YES" else "NO",
            color = if (active) Color(0xFFFF1744) else Color(0xFF00E676),
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}
