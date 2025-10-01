package com.sirim.scanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.compose.rememberNavController
import com.sirim.scanner.data.AppContainer
import com.sirim.scanner.ui.screens.dashboard.DashboardScreen
import com.sirim.scanner.ui.screens.dashboard.DashboardViewModel
import com.sirim.scanner.ui.screens.export.ExportScreen
import com.sirim.scanner.ui.screens.export.ExportViewModel
import com.sirim.scanner.ui.screens.login.LoginScreen
import com.sirim.scanner.ui.screens.login.LoginViewModel
import com.sirim.scanner.ui.screens.records.RecordFormScreen
import com.sirim.scanner.ui.screens.records.RecordListScreen
import com.sirim.scanner.ui.screens.records.RecordViewModel
import com.sirim.scanner.ui.screens.scanner.ScannerScreen
import com.sirim.scanner.ui.screens.scanner.ScannerViewModel
import com.sirim.scanner.ui.theme.SirimScannerTheme

class MainActivity : ComponentActivity() {

    private val container: AppContainer by lazy {
        (application as SirimScannerApplication).container
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SirimScannerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SirimApp(container = container)
                }
            }
        }
    }
}

sealed class Destinations(val route: String) {
    data object Login : Destinations("login")
    data object Dashboard : Destinations("dashboard")
    data object Scanner : Destinations("scanner")
    data object RecordList : Destinations("records")
    data object RecordForm : Destinations("recordForm")
    data object Export : Destinations("export")
}

@Composable
fun SirimApp(container: AppContainer) {
    val navController = rememberNavController()
    NavGraph(container = container, navController = navController)
}

@Composable
private fun NavGraph(container: AppContainer, navController: NavHostController) {
    NavHost(navController = navController, startDestination = Destinations.Login.route) {
        composable(Destinations.Login.route) {
            val viewModel: LoginViewModel = viewModel(factory = LoginViewModel.Factory())
            LoginScreen(
                viewModel = viewModel,
                onAuthenticated = {
                    navController.navigate(Destinations.Dashboard.route) {
                        popUpTo(Destinations.Login.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Destinations.Dashboard.route) {
            val viewModel: DashboardViewModel = viewModel(
                factory = DashboardViewModel.Factory(container.repository)
            )
            DashboardScreen(
                viewModel = viewModel,
                navigateToScanner = { navController.navigate(Destinations.Scanner.route) },
                navigateToRecords = { navController.navigate(Destinations.RecordList.route) },
                navigateToExport = { navController.navigate(Destinations.Export.route) }
            )
        }
        composable(Destinations.Scanner.route) {
            val viewModel: ScannerViewModel = viewModel(
                factory = ScannerViewModel.Factory(
                    repository = container.repository,
                    analyzer = container.labelAnalyzer,
                    appScope = container.applicationScope
                )
            )
            ScannerScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onRecordSaved = { id ->
                    navController.navigate("${Destinations.RecordForm.route}?recordId=$id") {
                        popUpTo(Destinations.Scanner.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Destinations.RecordList.route) {
            val viewModel: RecordViewModel = viewModel(
                factory = RecordViewModel.Factory(container.repository)
            )
            RecordListScreen(
                viewModel = viewModel,
                onAdd = { navController.navigate("${Destinations.RecordForm.route}") },
                onEdit = { record ->
                    navController.navigate("${Destinations.RecordForm.route}?recordId=${record.id}")
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Destinations.RecordForm.route + "?recordId={recordId}",
            arguments = listOf(
                navArgument("recordId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val viewModel: RecordViewModel = viewModel(
                factory = RecordViewModel.Factory(container.repository)
            )
            RecordFormScreen(
                viewModel = viewModel,
                onSaved = {
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() },
                recordId = backStackEntry.arguments?.getLong("recordId")?.takeIf { it > 0 }
            )
        }
        composable(Destinations.Export.route) {
            val viewModel: ExportViewModel = viewModel(
                factory = ExportViewModel.Factory(container.repository, container.exportManager)
            )
            ExportScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
