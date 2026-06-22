package io.github.leogallego.ansiblejane

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
fun setupMainDispatcher() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
}

@OptIn(ExperimentalCoroutinesApi::class)
fun tearDownMainDispatcher() {
    Dispatchers.resetMain()
}
