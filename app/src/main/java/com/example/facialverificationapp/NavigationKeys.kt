package com.example.facialverificationapp

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Main : NavKey
@Serializable data object Register : NavKey
@Serializable data object Verify : NavKey
