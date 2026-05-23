package io.github.leogallego.ansiblejane

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import io.github.leogallego.ansiblejane.navigation.AppNavigation
import io.github.leogallego.ansiblejane.ui.theme.AnsibleJaneTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AnsibleJaneTheme {
                AppNavigation(modifier = Modifier.semantics { testTagsAsResourceId = true })
            }
        }
    }
}
