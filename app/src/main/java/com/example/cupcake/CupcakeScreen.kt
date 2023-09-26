/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.cupcake

import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.cupcake.data.DataSource
import com.example.cupcake.ui.OrderSummaryScreen
import com.example.cupcake.ui.OrderViewModel
import com.example.cupcake.ui.SelectOptionScreen
import com.example.cupcake.ui.StartOrderScreen

/**
 * Composable that displays the topBar and displays back button if back navigation is possible.
 */
@Composable
fun CupcakeAppBar(
    currentScreen: CupcakeScreen,
    canNavigateBack: Boolean,
    navigateUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = { Text(stringResource(currentScreen.title)) },
        colors = TopAppBarDefaults.mediumTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = modifier,
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = navigateUp) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back_button)
                    )
                }
            }
        }
    )
}

@Composable
fun CupcakeApp(
    viewModel: OrderViewModel = viewModel(),
    navController: NavHostController = rememberNavController()//it is navhost controller
) {

    val backStackEntry by navController.currentBackStackEntryAsState()

    val currentScreen = CupcakeScreen.valueOf(
        backStackEntry?.destination?.route ?: CupcakeScreen.Start.name
    )

    Scaffold(
        topBar = {
            CupcakeAppBar(
                currentScreen = currentScreen,
                canNavigateBack = navController.previousBackStackEntry != null,
                navigateUp = { navController.navigateUp() }
            )
        }
    ) { innerPadding ->
        val uiState by viewModel.uiState.collectAsState()

        //NavHost
        NavHost(
            navController = navController,
            startDestination = CupcakeScreen.Start.name,
            modifier = Modifier.padding(innerPadding)
        ) {

            //<editor-fold desc="NAV GRAPH">
            //this part is nav graph
            composable(route = CupcakeScreen.Start.name) {
                StartOrderScreen(quantityOptions = DataSource.quantityOptions,
                    {
                        //function for handling data update and navigation to different view
                        onQuantitySelected(it, viewModel, navController)

                    })
            }


            composable(route = CupcakeScreen.Flavor.name) {
                val context = LocalContext.current

                SelectOptionScreen(subtotal = uiState.price,
                    options = DataSource.flavors.map { context.resources.getString(it) },
                    onSelectionChanged = { viewModel.setFlavor(it) },
                    onNextButtonClicked = {
                        onNextClicked(
                            navController,
                            CupcakeScreen.Pickup.name
                        )
                    },
                    onCancelButtonClicked = { onCancelClicked(viewModel, navController) }
                )
            }

            composable(route = CupcakeScreen.Pickup.name) {
                SelectOptionScreen(subtotal = uiState.price,
                    options = uiState.pickupOptions,
                    onSelectionChanged = { viewModel.setDate(it) },
                    onNextButtonClicked = {
                        onNextClicked(
                            navController,
                            CupcakeScreen.Summary.name
                        )
                    },
                    onCancelButtonClicked = { onCancelClicked(viewModel, navController) })
            }

            composable(route = CupcakeScreen.Summary.name) {
                val context = LocalContext.current

                OrderSummaryScreen(orderUiState = uiState,
                    onCancelButtonClicked = { onCancelClicked(viewModel, navController) },
                    onSendButtonClicked = { orderSummary, newOrder ->
                        onSendButtonClicked(
                            context, orderSummary, newOrder
                        )
                    })
            }
            //</editor-fold>
        }
    }
}

private fun onQuantitySelected(
    quantity: Int,
    viewModel: OrderViewModel,
    navController: NavHostController
) {

    viewModel.setQuantity(quantity)

    /**
     *  controls the forward navigation --> route is basically destination
     *  the destination name is already setup during navigation graph creation
     */

    onNextClicked(navController, CupcakeScreen.Flavor.name)
}

private fun onCancelClicked(viewModel: OrderViewModel, navController: NavHostController) {
    /**
     * navigateUp is useful when you want to go in backstack
     */
//    navController.navigateUp()

    /**
     * if we want to restart the order from beginning then
     * we should use popBackStack(@route, @inclusive)
     * @route is the destination -> above this destination all the screen will be removed
     * if @inclusive is true then it will remove all the screens above destination
     * including destination screen from the stack
     */

    viewModel.resetOrder()
    navController.popBackStack(route = CupcakeScreen.Start.name, inclusive = false)
}

private fun onSendButtonClicked(context: Context, orderSummary: String, newOrder: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, newOrder)
        putExtra(Intent.EXTRA_TEXT, orderSummary)
    }

    context.startActivity(Intent.createChooser(
        intent, newOrder
    ))
}

private fun onNextClicked(navController: NavHostController, routeName: String) {
    navController.navigate(route = routeName)
}

@Composable
@Preview
fun CupcakeAppPreview() {
    CupcakeApp()
}

enum class CupcakeScreen(@StringRes val title: Int) {
    Start(title = R.string.app_name),
    Flavor(title = R.string.choose_flavor),
    Pickup(title = R.string.choose_pickup_date),
    Summary(title = R.string.order_summary)
}