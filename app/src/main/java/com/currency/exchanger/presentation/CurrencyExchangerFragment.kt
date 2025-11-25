package com.currency.exchanger.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.currency.exchanger.R
import com.currency.exchanger.data.local.entity.Balance
import com.currency.exchanger.data.local.entity.User
import com.currency.exchanger.databinding.FragmentCurrencyExchangerBinding
import com.currency.exchanger.databinding.ItemBalanceBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

@AndroidEntryPoint
class CurrencyExchangerFragment : Fragment() {

    private var _binding: FragmentCurrencyExchangerBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: CurrencyExchangerViewModel by viewModels()

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
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.userWithBalances.collect { userWithBalances ->
                    userWithBalances.user?.let { updateUserInfo(it) }
                    updateBalances(userWithBalances.balances)
                }
            }
        }
    }
    
    private fun updateUserInfo(user: User) {
        binding.tvUserName.text = "${user.firstName} ${user.lastName}"
    }
    
    private fun updateBalances(balances: List<Balance>) {
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
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance() = CurrencyExchangerFragment()
    }
}
