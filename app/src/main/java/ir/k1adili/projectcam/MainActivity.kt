package ir.k1adili.projectcam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import ir.k1adili.projectcam.ui.ProjectCamNavHost
import ir.k1adili.projectcam.ui.theme.ProjectCamTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ProjectCamTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ProjectCamNavHost()
                }
            }
        }
    }
}
