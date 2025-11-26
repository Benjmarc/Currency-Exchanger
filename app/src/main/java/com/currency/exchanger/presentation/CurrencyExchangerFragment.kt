package com.currency.exchanger.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.currency.exchanger.R
import com.currency.exchanger.data.local.entity.Balance
import com.currency.exchanger.databinding.FragmentCurrencyExchangerBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import com.currency.exchanger.data.local.entity.User
import com.currency.exchanger.databinding.ItemBalanceBinding
import com.currency.exchanger.presentation.CurrencyExchangerViewModel.Companion.ErrorState
import java.text.NumberFormat
import java.util.*

@AndroidEntryPoint
class CurrencyExchangerFragment : Fragment() {

    private var _binding: FragmentCurrencyExchangerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CurrencyExchangerViewModel by viewModels()
    
    private var loadingDialog: AlertDialog? = null
    private var errorDialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCurrencyExchangerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up spinners
        setupSellCurrencySpinner()
        setupReceiveCurrencySpinner()

        viewModel.createUserWithInitialBalances()
        viewModel.fetchExchangeRates()
        
        // Set up submit button click listener
        binding.btnConvert.setOnClickListener {
            handleExchange()
        }
        
        // Observe exchange rates to update conversion when rates change
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.exchangeRates.collect {
                    calculateReceivedAmount()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.userWithBalances.collect { userWithBalances ->
                    userWithBalances.user?.let { updateUserInfo(it) }
                    updateBalances(userWithBalances.balances)
                }
            }
        }
        
        // Observe loading state
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { isLoading ->
                    if (isLoading) {
                        showLoading()
                    } else {
                        hideLoading()
                    }
                }
            }
        }
        
        // Observe error state
        observeErrorState()
    }

    private fun updateUserInfo(user: User) {
        binding.tvUserName.text = "${user.firstName} ${user.lastName}"
    }

    private fun updateBalances(balances: List<Balance>) {
        // Update balances list
        val adapter = object : ArrayAdapter<Balance>(
            requireContext(),
            R.layout.item_balance,
            R.id.tvCurrencyCode,
            balances
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val binding = convertView?.tag as? ItemBalanceBinding ?:
                    ItemBalanceBinding.inflate(LayoutInflater.from(context), parent, false).apply {
                        root.tag = this
                    }

                val balance = getItem(position) ?: return binding.root

                binding.tvCurrencyCode.text = balance.currency
                binding.tvAmount.text = NumberFormat.getNumberInstance(Locale.getDefault())
                    .format(balance.amount)

                return binding.root
            }
        }

        binding.lvBalances.adapter = adapter

        // Update sell currency spinner with available currencies
        val currencies = balances.map { it.currency }.toTypedArray()
        val sellAdapter = ArrayAdapter(
            requireContext(),
            R.layout.spinner_item,
            currencies
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.currencySellSpinner.adapter = sellAdapter
    }

    private fun setupSellCurrencySpinner() {
        // The adapter will be set when we have the balances
        binding.currencySellSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val adapter = parent?.adapter as? ArrayAdapter<*>
                val selectedCurrency = adapter?.getItem(position) as? String
                selectedCurrency?.let { currency ->
                    // Update your ViewModel with the selected currency
                    updateSellAmountWithDefault(currency)
                }
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                // Handle no selection if needed
            }
        }
    }

    private fun updateSellAmountWithDefault(currency: String) {
        // Get the user's balance for the selected currency
        val balance = viewModel.userWithBalances.value.balances
            .firstOrNull { it.currency == currency }?.amount ?: 0.0

        // Format the amount with 2 decimal places
        val formattedAmount = String.format("%.2f", balance)

        // Only update if the field is empty to avoid overriding user input
        if (binding.etSellAmount.text.isNullOrEmpty()) {
            binding.etSellAmount.setText(formattedAmount)
            calculateReceivedAmount()
        }
    }

    private fun setupReceiveCurrencySpinner() {
        // Set up the spinner with empty data initially
        val adapter = ArrayAdapter<String>(
            requireContext(),
            R.layout.spinner_item,
            mutableListOf()
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.currencyReceiveSpinner.adapter = adapter

        // Observe exchange rates from ViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.exchangeRates.collect { rates ->
                    updateReceiveCurrencySpinner(rates.keys.toList())
                }
            }
        }

        // Handle selection
        binding.currencyReceiveSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                calculateReceivedAmount()
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                // Handle no selection if needed
            }
        }
        
        // Add text watcher to update received amount when sell amount changes
        binding.etSellAmount.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                calculateReceivedAmount()
            }
        })
    }
    
    private fun updateReceiveCurrencySpinner(currencies: List<String>) {
        val adapter = binding.currencyReceiveSpinner.adapter as? ArrayAdapter<String> ?: return
        val currentSelection = binding.currencyReceiveSpinner.selectedItem as? String
        
        // Filter out the currently selected sell currency
        val sellCurrency = binding.currencySellSpinner.selectedItem as? String
        val filteredCurrencies = if (sellCurrency != null) {
            currencies.filter { it != sellCurrency }
        } else {
            currencies
        }
        
        // Update the adapter with filtered currencies
        adapter.clear()
        adapter.addAll(filteredCurrencies)
        
        // Try to maintain previous selection if possible
        if (currentSelection != null && filteredCurrencies.contains(currentSelection)) {
            val position = filteredCurrencies.indexOf(currentSelection)
            binding.currencyReceiveSpinner.setSelection(position)
        } else if (filteredCurrencies.isNotEmpty()) {
            binding.currencyReceiveSpinner.setSelection(0)
        }
        
        // Trigger amount calculation after spinner update
        calculateReceivedAmount()
    }
    
    private fun calculateReceivedAmount() {
        try {
            // Get selected currencies
            val sellCurrency = binding.currencySellSpinner.selectedItem as? String
            val receiveCurrency = binding.currencyReceiveSpinner.selectedItem as? String
            
            // Get sell amount
            val sellAmountText = binding.etSellAmount.text.toString()
            if (sellAmountText.isBlank() || sellCurrency == null || receiveCurrency == null) {
                binding.tvReceiveAmount.text = "0.00"
                return
            }
            
            val sellAmount = sellAmountText.toDouble()
            
            // Get exchange rates
            val exchangeRates = viewModel.exchangeRates.value
            
            // If same currency, no conversion needed
            if (sellCurrency == receiveCurrency) {
                binding.tvReceiveAmount.text = String.format("%.2f", sellAmount)
                return
            }
            
            // Get the exchange rates (assuming rates are based on EUR as base currency)
            val sellRate = exchangeRates[sellCurrency] ?: 1.0
            val receiveRate = exchangeRates[receiveCurrency] ?: 1.0
            
            // Calculate received amount: (sellAmount / sellRate) * receiveRate
            val receivedAmount = (sellAmount / sellRate) * receiveRate
            
            // Update UI with formatted amount
            binding.tvReceiveAmount.text = String.format("%.2f", receivedAmount)
            
        } catch (e: NumberFormatException) {
            binding.tvReceiveAmount.text = "0.00"
        } catch (e: Exception) {
            binding.tvReceiveAmount.text = "0.00"
        }
    }
    
    private fun handleExchange() {
        // Clear previous errors
        binding.sellAmountLayout.error = null

        // Get selected currencies and amount
        val sellCurrency = binding.currencySellSpinner.selectedItem as? String
        val receiveCurrency = binding.currencyReceiveSpinner.selectedItem as? String
        val sellAmountText = binding.etSellAmount.text.toString()

        // Validate sell currency is selected
        if (sellCurrency.isNullOrEmpty()) {
            binding.sellAmountLayout.error = getString(R.string.error_select_sell_currency)
            return
        }

        // Validate receive currency is selected
        if (receiveCurrency.isNullOrEmpty()) {
            binding.sellAmountLayout.error = getString(R.string.error_select_receive_currency)
            return
        }

        // Prevent same currency exchange
        if (sellCurrency == receiveCurrency) {
            binding.sellAmountLayout.error = getString(R.string.error_same_currency)
            return
        }

        // Validate amount is not empty
        if (sellAmountText.isBlank()) {
            binding.sellAmountLayout.error = getString(R.string.error_amount_empty)
            return
        }

        // Parse and validate amount
        val sellAmount = try {
            sellAmountText.toDouble()
        } catch (e: NumberFormatException) {
            binding.sellAmountLayout.error = getString(R.string.error_invalid_amount)
            return
        }

        // Validate amount is positive
        if (sellAmount <= 0) {
            binding.sellAmountLayout.error = getString(R.string.error_amount_positive)
            return
        }

        // Get user's balance for the sell currency
        val userBalances = viewModel.userWithBalances.value.balances
        val userBalance = userBalances.find { it.currency == sellCurrency }?.amount ?: 0.0

        // Check if user has sufficient balance
        if (sellAmount > userBalance) {
            binding.sellAmountLayout.error = getString(
                R.string.error_insufficient_balance,
                String.format("%.2f", userBalance),
                sellCurrency
            )
            return
        }

        // All validations passed, proceed with exchange
        viewModel.performExchange(sellCurrency, receiveCurrency, sellAmount)
        
        // Optionally, show a success message
        Toast.makeText(
            requireContext(),
            getString(R.string.success_exchange),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun observeErrorState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.errorState.collect { errorState ->
                    errorState?.let { showErrorDialog(it) }
                }
            }
        }
    }
    
    private fun showErrorDialog(errorState: ErrorState) {
        // Dismiss any existing error dialog
        errorDialog?.dismiss()
        
        errorDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.error_title))
            .setMessage(errorState.message)
            .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                dialog.dismiss()
                viewModel.clearError()
            }
            .setOnDismissListener {
                viewModel.clearError()
            }
            .create()
        
        // Only show if fragment is still attached
        if (!isDetached) {
            errorDialog?.show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        errorDialog?.dismiss()
        errorDialog = null
        hideLoading()
        _binding = null
    }
    
    private fun showLoading() {
        if (loadingDialog?.isShowing == true || !isAdded || isDetached) return
        
        loadingDialog = MaterialAlertDialogBuilder(requireContext())
            .setView(R.layout.dialog_loading)
            .setCancelable(false)
            .create()
            
        try {
            loadingDialog?.show()
        } catch (e: Exception) {
            // Handle any potential WindowManager$BadTokenException or other issues
            e.printStackTrace()
        }
    }
    
    private fun hideLoading() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }

    companion object {
        @JvmStatic
        fun newInstance() = CurrencyExchangerFragment()
    }
}
