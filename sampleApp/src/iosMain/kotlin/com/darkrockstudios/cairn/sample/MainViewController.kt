package com.darkrockstudios.cairn.sample

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

@Suppress("unused", "FunctionName")
fun MainViewController(): UIViewController = ComposeUIViewController { App() }
