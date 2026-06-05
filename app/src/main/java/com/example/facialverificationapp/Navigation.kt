package com.example.facialverificationapp

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.facialverificationapp.data.FaceRepository
import com.example.facialverificationapp.data.local.FaceDatabase
import com.example.facialverificationapp.ui.main.MainScreen
import com.example.facialverificationapp.ui.main.MainScreenViewModel
import com.example.facialverificationapp.ui.main.RegisterScreen
import com.example.facialverificationapp.ui.main.VerificationScreen

@Composable
fun MainNavigation() {
  val backStack = rememberNavBackStack(Main)
  val context = LocalContext.current

  // Instantiating dependencies offline in MVVM style
  val viewModel: MainScreenViewModel = viewModel {
    val database = FaceDatabase.getDatabase(context)
    val repository = FaceRepository(database.faceDao())
    MainScreenViewModel(repository)
  }

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Main> {
          MainScreen(
            onItemClick = { navKey -> backStack.add(navKey) },
            viewModel = viewModel,
            modifier = Modifier.safeDrawingPadding()
          )
        }
        entry<Register> {
          RegisterScreen(
            viewModel = viewModel,
            onBackClick = { 
              viewModel.loadRegisteredUsers()
              backStack.removeLastOrNull() 
            },
            modifier = Modifier.safeDrawingPadding()
          )
        }
        entry<Verify> {
          VerificationScreen(
            viewModel = viewModel,
            onBackClick = { 
              viewModel.resetLivenessAndVerification()
              backStack.removeLastOrNull() 
            },
            modifier = Modifier.safeDrawingPadding()
          )
        }
      },
  )
}
