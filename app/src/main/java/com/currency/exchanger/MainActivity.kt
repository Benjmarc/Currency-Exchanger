package com.currency.exchanger

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.currency.exchanger.presentation.CurrencyExchangerFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Add the fragment to the container if this is the first time the activity is created
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, CurrencyExchangerFragment.newInstance())
                .commitNow()
        }
    }
}
