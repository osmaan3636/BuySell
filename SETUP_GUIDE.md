# Sell Feature - Setup Guide

## Quick Start

This document explains how to configure and test the Sell feature with Supabase backend.

## Prerequisites

1. **Supabase Account**: Create a free account at [supabase.com](https://supabase.com)
2. **Android Studio**: Latest version with Kotlin support
3. **Internet Connection**: Required for Supabase API calls

## Supabase Configuration

### Step 1: Get Your Credentials

1. Go to your Supabase project dashboard
2. Navigate to **Settings** → **API**
3. Copy the following values:
   - **Project URL** (format: `https://xxxxx.supabase.co`)
   - **anon/public key** (long string starting with `eyJ...`)

### Step 2: Add Credentials to App

Open `SupabaseClientManager.kt` and replace the placeholder values:

```kotlin
private const val SUPABASE_URL = "https://your-project.supabase.co"
private const val SUPABASE_ANON_KEY = "your_anon_key_here"
```

### Step 3: Create Database Tables

Copy and execute the following SQL in your Supabase SQL Editor:

```sql
-- Create products table
CREATE TABLE products (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  name TEXT NOT NULL,
  buy_price DECIMAL(10,2) NOT NULL,
  sell_price DECIMAL(10,2) NOT NULL,
  stock INTEGER NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Create sell_transactions table
CREATE TABLE sell_transactions (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  product_id UUID REFERENCES products(id),
  product_name TEXT NOT NULL,
  original_sell_price DECIMAL(10,2) NOT NULL,
  discount_type TEXT CHECK (discount_type IN ('PERCENTAGE', 'DIRECT_PRICE', 'NONE')),
  discount_value DECIMAL(10,2) NOT NULL DEFAULT 0,
  final_price DECIMAL(10,2) NOT NULL,
  quantity INTEGER NOT NULL,
  buy_price DECIMAL(10,2) NOT NULL,
  total_profit DECIMAL(10,2) NOT NULL,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Create RPC function for safe stock reduction
CREATE OR REPLACE FUNCTION reduce_product_stock(
  product_uuid UUID,
  reduce_by INTEGER
)
RETURNS BOOLEAN AS $$
BEGIN
  UPDATE products
  SET stock = stock - reduce_by
  WHERE id = product_uuid AND stock >= reduce_by;
  
  RETURN FOUND;
END;
$$ LANGUAGE plpgsql;

-- Insert sample product for testing
INSERT INTO products (name, buy_price, sell_price, stock)
VALUES ('Sample Product', 800.00, 1500.00, 25);
```

### Step 4: Enable Row Level Security (Optional but Recommended)

For production, add RLS policies:

```sql
-- Enable RLS
ALTER TABLE products ENABLE ROW LEVEL SECURITY;
ALTER TABLE sell_transactions ENABLE ROW LEVEL SECURITY;

-- Allow read access to products
CREATE POLICY "Allow public read access to products"
  ON products FOR SELECT
  TO anon, authenticated
  USING (true);

-- Allow insert to sell_transactions
CREATE POLICY "Allow insert to sell_transactions"
  ON sell_transactions FOR INSERT
  TO anon, authenticated
  WITH CHECK (true);
```

## Build and Run

### Option 1: Android Studio

1. Open the project in Android Studio
2. Sync Gradle (it should happen automatically)
3. Run the app on an emulator or device
4. Tap "Test Sell Feature" button on the main screen

### Option 2: Command Line

```bash
# From project root directory
./gradlew installDebug

# Or on Windows:
gradlew.bat installDebug
```

## Testing the Features

### Test Case 1: Percentage Discount

1. Open the app and tap "Test Sell Feature"
2. The sample product should be loaded (1500 TK sell price)
3. Enter quantity: **2**
4. Select "Percentage Discount"
5. Enter **5** in the percentage field
6. **Expected**: Final price shows 1425 TK
7. Tap "Sell" and confirm
8. **Expected**: Success message, transaction saved to Supabase

### Test Case 2: Direct Final Price

1. Open SellActivity
2. Select "Direct Final Price"
3. Enter **1450**
4. **Expected**: Discount shows -50 TK (3.3%)
5. Sell and verify in Supabase

### Test Case 3: No Discount

1. Keep "No Discount" selected
2. **Expected**: Final price = Original price (1500 TK)
3. Complete sale

## Verify in Supabase Dashboard

1. Go to Supabase Dashboard → Table Editor
2. Navigate to `sell_transactions` table
3. You should see your transactions with all details
4. Check `products` table to see reduced stock

## Troubleshooting

### App crashes on launch
- Make sure you've added your Supabase credentials
- Check that internet permission is in AndroidManifest.xml

### "Error saving transaction"
- Verify your Supabase URL and anon key are correct
- Check internet connection
- Ensure database tables are created

### Stock not reducing
- Verify the RPC function is created in Supabase
- Check Supabase logs in the dashboard

### Calculation not working
- This is real-time, check if TextWatchers are firing
- Verify ViewModel observers are set up in SellActivity

## Architecture Overview

```
SellActivity (UI)
    ↓ (ViewBinding + LiveData)
SellViewModel (Business Logic)
    ↓ (Coroutines)
ProductRepository (Data Layer)
    ↓ (Supabase SDK)
Supabase PostgreSQL Database
```

## Key Features Implemented

✅ Dual discount system (percentage OR direct price)  
✅ Mutual exclusivity (only one discount type active)  
✅ Real-time price calculations  
✅ Input validation with error messages  
✅ Safe stock reduction via PostgreSQL RPC function  
✅ Confirmation dialog before sale  
✅ Success/error feedback via Snackbar  
✅ MVVM architecture with ViewBinding  
✅ Kotlinx Serialization for Supabase  
✅ Material Design 3 UI components  

## Next Steps

To integrate this into your full app:

1. **Product Selection**: Create a product list screen
2. **Pass Product ID**: Use intent extras to pass product ID to SellActivity
3. **Remove Sample Product**: Delete the `initializeSampleProduct()` code in SellActivity
4. **Load Real Data**: Uncomment the product loading code in SellActivity
5. **Authentication**: Add Supabase Auth if needed for multi-user support

## Support

For issues or questions about:
- **Supabase**: Check [supabase.com/docs](https://supabase.com/docs)
- **Android Development**: See [developer.android.com](https://developer.android.com)
- **Kotlin**: Visit [kotlinlang.org](https://kotlinlang.org)
