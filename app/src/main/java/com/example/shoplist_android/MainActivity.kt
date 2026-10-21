package com.example.shoplist_android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.shoplist_android.ui.theme.LocalShoplistColors
import com.example.shoplist_android.ui.theme.LocalShoplistTypography
import com.example.shoplist_android.ui.theme.ShoplistTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ShoplistTheme {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    val colors = LocalShoplistColors.current
                    val typography = LocalShoplistTypography.current
                    Text(
                        text = "Shoplist",
                        style = typography.title,
                        color = colors.content,
                    )
                }
            }
        }
    }
}
