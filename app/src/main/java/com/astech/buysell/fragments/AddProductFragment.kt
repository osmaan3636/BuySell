package com.astech.buysell.fragments

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.astech.buysell.databinding.FragmentAddProductBinding
import com.astech.buysell.viewmodel.AddProductViewModel
import java.io.InputStream

class AddProductFragment : Fragment() {

    private var _binding: FragmentAddProductBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: AddProductViewModel by viewModels()
    private var selectedImageBytes: ByteArray? = null

    // Image picker launcher
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            binding.ivProductImage.setImageURI(it)
            binding.ivProductImage.setPadding(0, 0, 0, 0) // Remove padding to show full image
            binding.ivProductImage.imageTintList = null // Remove tint
            
            // Read bytes from URI
            try {
                val inputStream: InputStream? = requireContext().contentResolver.openInputStream(it)
                selectedImageBytes = inputStream?.readBytes()
                inputStream?.close()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnAddProduct.isEnabled = !isLoading
            binding.cardImage.isEnabled = !isLoading
            binding.etName.isEnabled = !isLoading
            binding.etBuyPrice.isEnabled = !isLoading
            binding.etSellPrice.isEnabled = !isLoading
            binding.etStock.isEnabled = !isLoading
        }
        
        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                viewModel.clearMessages()
            }
        }
        
        viewModel.successMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                clearInputs()
                viewModel.clearMessages()
            }
        }
    }

    private fun setupListeners() {
        binding.cardImage.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.btnAddProduct.setOnClickListener {
            val name = binding.etName.text.toString()
            val buyPrice = binding.etBuyPrice.text.toString()
            val sellPrice = binding.etSellPrice.text.toString()
            val stock = binding.etStock.text.toString()
            
            viewModel.addProduct(name, buyPrice, sellPrice, stock, selectedImageBytes)
        }
    }

    private fun clearInputs() {
        binding.etName.text?.clear()
        binding.etBuyPrice.text?.clear()
        binding.etSellPrice.text?.clear()
        binding.etStock.text?.clear()
        
        // Reset image view
        binding.ivProductImage.setImageResource(com.astech.buysell.R.drawable.ic_add)
        binding.ivProductImage.setPadding(32, 32, 32, 32)
        binding.ivProductImage.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.GRAY) // Simplistic reset
        selectedImageBytes = null
        
        binding.etName.requestFocus()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
