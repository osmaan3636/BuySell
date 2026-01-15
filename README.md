# 📊 BuySell - Modern Inventory Management System

A feature-rich Android inventory management application built with Kotlin and powered by Supabase. Track your products, manage sales, monitor stock levels, and analyze profits with beautiful charts and real-time data synchronization.

## ✨ Features

### 📦 **Product Management**
- Add, edit, and delete products with images
- Track buy price, sell price, and stock quantities
- Image upload and management with Supabase Storage
- Low stock alerts and monitoring

### 💰 **Sales Tracking**
- Flexible discount system (percentage or direct price)
- Real-time price calculations
- Product search and quick selection
- Shopping cart functionality
- Transaction history with detailed records

### 📈 **Analytics & Reports**
- Interactive profit charts (daily, weekly, monthly)
- Revenue tracking and analysis
- Stock level monitoring
- Custom chart markers with detailed information
- Beautiful data visualizations using MPAndroidChart

### 🔄 **Real-time Synchronization**
- Cloud-based data storage with Supabase
- Automatic stock updates
- Transaction logging
- Safe concurrent operations with PostgreSQL RPC functions

## 🛠️ Tech Stack

### **Core Technologies**
- **Language**: Kotlin
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 36
- **Architecture**: MVVM (Model-View-ViewModel)

### **Key Libraries**
- **Backend**: [Supabase](https://supabase.com) - PostgreSQL database, Storage, and real-time features
- **UI**: Material Design 3, ViewBinding, ConstraintLayout
- **Charts**: [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart) - Beautiful data visualizations
- **Image Loading**: [Coil](https://coil-kt.github.io/coil/) - Fast and efficient image loading
- **Async**: Kotlin Coroutines & Flow
- **Lifecycle**: ViewModel, LiveData
- **Networking**: Ktor Client
- **Serialization**: Kotlinx Serialization

## 📸 Screenshots

![BuySell Screenshot 1](https://njbharoyzzqdkhhpcccg.supabase.co/storage/v1/object/public/ss/1.jpeg)
![BuySell Screenshot 2](https://njbharoyzzqdkhhpcccg.supabase.co/storage/v1/object/public/ss/2.jpeg)
 

```

## 🚀 Getting Started

### Prerequisites

- Android Studio (latest version recommended)
- JDK 11 or higher
- A Supabase account (free tier available at [supabase.com](https://supabase.com))

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/osmaan3636/BuySell.git
   cd BuySell
   ```

2. **Configure Supabase**
   
   - Create a new project on [Supabase](https://app.supabase.com)
   - Go to Settings → API
   - Copy your Project URL and anon/public key
   - Open `app/src/main/java/com/astech/buysell/config/SupabaseClientManager.kt`
   - Replace the placeholder values:
     ```kotlin
     private const val SUPABASE_URL = "https://your-project-id.supabase.co"
     private const val SUPABASE_ANON_KEY = "your_supabase_anon_key_here"
     ```

3. **Set up Database Tables**
   
   Execute the following SQL in your Supabase SQL Editor:

   ```sql
   -- Create products table
   CREATE TABLE products (
     id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
     name TEXT NOT NULL,
     buy_price DECIMAL(10,2) NOT NULL,
     sell_price DECIMAL(10,2) NOT NULL,
     stock INTEGER NOT NULL DEFAULT 0,
     image_url TEXT,
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
   ```

4. **Build and Run**
   ```bash
   ./gradlew installDebug
   # Or open in Android Studio and click Run
   ```

## 📚 Architecture

The app follows the **MVVM (Model-View-ViewModel)** architecture pattern:

```
┌─────────────────┐
│   UI Layer      │  Activities, Fragments, Adapters
│  (ViewBinding)  │
└────────┬────────┘
         │
┌────────▼────────┐
│  ViewModel      │  Business Logic, LiveData
│                 │
└────────┬────────┘
         │
┌────────▼────────┐
│  Repository     │  Data Access Layer
│                 │
└────────┬────────┘
         │
┌────────▼────────┐
│   Supabase      │  PostgreSQL + Storage
│   Backend       │
└─────────────────┘
```

### Key Components

- **Fragments**: StockFragment, SellFragment, ReportsFragment, AddProductFragment
- **ViewModels**: StockViewModel, CartViewModel, SellViewModel, AddProductViewModel
- **Repository**: ProductRepository - Handles all data operations
- **Config**: SupabaseClientManager - Singleton for Supabase client
- **Models**: Product, SellTransaction, CartItem, DiscountType

## 🔐 Security Notes

- **Never commit sensitive credentials** to version control
- Supabase credentials are excluded via `.gitignore`
- See `.env.example` for configuration template
- For production, implement Row Level Security (RLS) in Supabase
- Consider using encrypted local storage for sensitive data

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the project
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👤 Author

**Mohammad Osman**
- GitHub: [@osmaan3636](https://github.com/osmaan3636)
- Email: acfosman353636a@gmail.com
- WhatsApp: +8801857353636

## 🙏 Acknowledgments

- [Supabase](https://supabase.com) for the amazing backend platform
- [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart) for beautiful charts
- The Kotlin and Android communities

## 📞 Support

For setup help or questions, please refer to [SETUP_GUIDE.md](SETUP_GUIDE.md) or open an issue.

---

**Made with ❤️ using Kotlin and Supabase**
