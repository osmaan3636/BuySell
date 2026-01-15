package com.astech.buysell.config

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json

/**
 * Singleton object to manage Supabase client instance.
 * 
 * ⚠️ SETUP REQUIRED: Before running the app, you MUST configure your Supabase credentials.
 * 
 * 📋 Steps to configure:
 * 1. Go to your Supabase Dashboard (https://app.supabase.com)
 * 2. Navigate to: Settings → API
 * 3. Copy your Project URL (format: https://xxxxx.supabase.co)
 * 4. Copy your anon/public key (starts with 'eyJ...')
 * 5. Replace the placeholder values below with your actual credentials
 * 
 * 📖 See .env.example file for reference format
 * 
 * 🔒 Security Note: Never commit real credentials to version control!
 */
object SupabaseClientManager {
    
    // ⚠️ REPLACE WITH YOUR ACTUAL SUPABASE PROJECT URL
    // Get from: Supabase Dashboard → Settings → API → Project URL
    private const val SUPABASE_URL = "https://your-project-id.supabase.co"
    
    // ⚠️ REPLACE WITH YOUR ACTUAL SUPABASE ANON KEY
    // Get from: Supabase Dashboard → Settings → API → anon/public key
    private const val SUPABASE_ANON_KEY = "your_supabase_anon_key_here"
    
    /**
     * Lazy-initialized Supabase client instance
     */
    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_ANON_KEY
        ) {
            // Configure JSON serializer to include default values
            defaultSerializer = KotlinXSerializer(Json {
                encodeDefaults = true
                ignoreUnknownKeys = true
            })
            
            // Install Postgrest module for database operations
            install(Postgrest)
            install(Storage)
        }
    }
    
    /**
     * Check if Supabase is properly configured
     */
    fun isConfigured(): Boolean {
        return SUPABASE_URL != "SUPABASE_URL" &&
                SUPABASE_ANON_KEY != "SUPABASE_ANON_KEY"
    }
}
