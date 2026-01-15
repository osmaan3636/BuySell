package com.astech.buysell.repository

import android.util.Log
import com.astech.buysell.config.SupabaseClientManager
import com.astech.buysell.models.Product
import com.astech.buysell.models.SellTransaction
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Repository class for handling all Supabase database operations.
 * Provides clean separation between data layer and business logic.
 */
class ProductRepository {
    
    private val supabase = SupabaseClientManager.client
    private val TAG = "ProductRepository"
    
    /**
     * Fetch a single product by ID from Supabase
     * @param productId UUID of the product
     * @return Product object or null if not found
     */
    suspend fun getProductById(productId: String): Product? {
        return try {
            val result = supabase
                .from("products")
                .select(columns = Columns.ALL) {
                    filter {
                        eq("id", productId)
                    }
                }
                .decodeSingle<Product>()
            
            Log.d(TAG, "Product fetched successfully: ${result.name}")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching product: ${e.message}", e)
            null
        }
    }
    
    /**
     * Fetch all products from Supabase
     * @return List of products or empty list on error
     */
    suspend fun getAllProducts(): List<Product> {
        return try {
            val result = supabase
                .from("products")
                .select(columns = Columns.ALL)
                .decodeList<Product>()
            
            Log.d(TAG, "Fetched ${result.size} products")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching products: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Save a sell transaction to Supabase
     * @param transaction SellTransaction object to save
     * @return true if successful, false otherwise
     */
    suspend fun saveSellTransaction(transaction: SellTransaction): Boolean {
        return try {
            supabase
                .from("sell_transactions")
                .insert(transaction)
            
            Log.d(TAG, "Transaction saved successfully for product: ${transaction.productName}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving transaction: ${e.message}", e)
            false
        }
    }
    
    /**
     * Reduce product stock using Supabase RPC function.
     * This uses a PostgreSQL function for atomic transaction safety.
     * 
     * The RPC function should be defined in Supabase as:
     * ```sql
     * CREATE OR REPLACE FUNCTION reduce_product_stock(
     *   product_uuid UUID,
     *   reduce_by INTEGER
     * )
     * RETURNS BOOLEAN AS $$
     * BEGIN
     *   UPDATE products
     *   SET stock = stock - reduce_by
     *   WHERE id = product_uuid AND stock >= reduce_by;
     *   
     *   RETURN FOUND;
     * END;
     * $$ LANGUAGE plpgsql;
     * ```
     * 
     * @param productId UUID of the product
     * @param quantity Amount to reduce stock by
     * @return true if stock was reduced successfully, false if insufficient stock
     */
    suspend fun reduceProductStock(productId: String, quantity: Int): Boolean {
        return try {
            // Build RPC parameters
            val params = buildJsonObject {
                put("product_uuid", productId)
                put("reduce_by", quantity)
            }
            
            // Call the RPC function with parameters
            val result: Boolean = supabase.postgrest.rpc(
                function = "reduce_product_stock",
                parameters = params
            ).decodeAs()
            
            if (result) {
                Log.d(TAG, "Stock reduced successfully for product: $productId by $quantity")
            } else {
                Log.w(TAG, "Failed to reduce stock - insufficient stock or product not found")
            }
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error reducing stock via RPC: ${e.message}", e)
            false
        }
    }
    
    /**
     * Execute a complete sell transaction:
     * 1. Save the transaction record
     * 2. Reduce the product stock
     * 
     * @param transaction SellTransaction to save
     * @return true if both operations succeeded, false otherwise
     */
    suspend fun executeSellTransaction(transaction: SellTransaction): Boolean {
        return try {
            // First, save the transaction
            val transactionSaved = saveSellTransaction(transaction)
            if (!transactionSaved) {
                Log.e(TAG, "Failed to save transaction")
                return false
            }
            
            // Then, reduce stock using RPC
            val stockReduced = reduceProductStock(transaction.productId, transaction.quantity)
            if (!stockReduced) {
                Log.e(TAG, "Failed to reduce stock - transaction saved but stock not updated")
                // Note: In a production app, you might want to implement compensation logic here
                return false
            }
            
            Log.d(TAG, "Sell transaction completed successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error executing sell transaction: ${e.message}", e)
            false
        }
    }
    /**
     * Add a new product to Supabase
     * @param product Product object to add
     * @return true if successful, false otherwise
     */
    suspend fun addProduct(product: Product): Boolean {
        return try {
            // Manually build JSON to exclude 'id' and let Supabase auto-generate it
            val productJson = buildJsonObject {
                put("name", product.name)
                put("buy_price", product.buyPrice)
                put("sell_price", product.sellPrice)
                put("stock", product.stock)
                // Only include nullable fields if they are not null
                product.imageUrl?.let { put("image_url", it) }
                product.createdAt?.let { put("created_at", it) }
            }

            supabase
                .from("products")
                .insert(productJson)
            
            Log.d(TAG, "Product added successfully: ${product.name}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error adding product: ${e.message}", e)
            false
        }
    }
    
    /**
     * Upload product image to Supabase Storage
     * @param byteArray Image data
     * @param fileName Unique file name
     * @return Public URL of the uploaded image or null on error
     */
    suspend fun uploadImage(byteArray: ByteArray, fileName: String): String? {
        return try {
            val bucket = supabase.storage.from("product_images")
            bucket.upload(fileName, byteArray)
            
            val publicUrl = bucket.publicUrl(fileName)
            Log.d(TAG, "Image uploaded successfully: $publicUrl")
            publicUrl
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading image: ${e.message}", e)
            null
        }
    }
    
    /**
     * Fetch all sell transactions from Supabase
     * @return List of SellTransaction
     */
    suspend fun getTransactions(): List<SellTransaction> {
        return try {
            val result = supabase
                .from("sell_transactions")
                .select(columns = Columns.ALL) {
                    order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                }
                .decodeList<SellTransaction>()
            
            Log.d(TAG, "Fetched ${result.size} transactions")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching transactions: ${e.message}", e)
            emptyList()
        }
    }
}
