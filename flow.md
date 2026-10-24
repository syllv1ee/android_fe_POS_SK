# Alur Teknis Aplikasi

Inti alurnya: **status login disimpan di DataStore**, dan `MainActivity` memantau status itu
untuk menentukan layar. Layar Login dan Logout tidak pernah memanggil navigasi secara langsung.
Mereka cukup mengubah sesi, lalu UI menyesuaikan sendiri.

## 1. Saat aplikasi dibuka

```
MubarokApp (@HiltAndroidApp) → MainActivity → MainViewModel
```

- `MainViewModel` membaca dua hal dari DataStore: `themeMode` dan `isLoggedIn`.
- `isLoggedIn` bernilai `null` selama masih dibaca, dan selama itu yang tampil `LoadingView`.
  - `false` → `AppNavHost(startDestination = Routes.Login)`
  - `true` → `AppNavHost(startDestination = Routes.Home)`

## 2. Login

```
LoginScreen → LoginViewModel.login()
  → AuthRepositoryImpl.login() → safeApiCall { POST /auth/login }
```

1. Server membalas dengan cookie `access_token` dan `refresh_token`.
2. `SessionCookieJar` menyimpan cookie itu di SharedPreferences, jadi sesi tetap ada walaupun
   aplikasi ditutup.
3. Data user dari respons diubah menjadi `UserSession` (username, role, permissions,
   isSuperAdmin), lalu disimpan oleh `SessionManager` ke DataStore dengan `logged_in = true`.
4. Perubahan itu sampai ke `MainViewModel`, dan `MainActivity` mengganti tampilan ke Home.
   Tidak ada pemanggilan `navigate()`.
5. Kalau login gagal, `Result.Error` membawa pesan dan `fieldErrors` (dari respons 400). Pesan
   ini tampil di bawah field username atau password.

## 3. Navigasi setelah login

```
Dashboard → Sales Kanvas (Hub) → Penjualan / Retur / Tagihan
                ↘ Profil (ikon akun)
```

- `DashboardViewModel` menyaring `dashboardMenus` sesuai permission di sesi. Super Admin dan
  wildcard `modul:*` selalu lolos.
- Setiap rute adalah objek `@Serializable` (type-safe). Rute Sales Kanvas didefinisikan di
  `SalesCanvasNavigation.kt`.

## 4. Mengambil data (contoh: daftar Penjualan)

```
SaleListScreen → SaleListViewModel (SaleListUiState)
  → SaleRepository → safeApiCallPaged { GET /sales-kanvas/sale?page&limit }
  → Page<SaleDto> → toDomain() → Page<Sale>
```

- `load()` dipakai untuk muat awal satu layar penuh, `refresh()` untuk tarik-untuk-refresh, dan
  `loadMore()` untuk memuat halaman berikutnya saat scroll. Total halaman diambil dari `meta`.

## 5. Lapisan jaringan (berlaku untuk semua request)

```
Request → ServerUrlInterceptor (debug: ganti host dari setting)
        → HttpLoggingInterceptor (debug: log body)
        → CookieJar menempelkan cookie → server
```

- **Jika server membalas 401**, `TokenAuthenticator` mengirim `POST /auth/refresh` lewat
  OkHttp client terpisah, supaya tidak terjadi 401 berulang tanpa henti.
  - Refresh berhasil: request asli diulang sekali dengan cookie baru.
  - Refresh ditolak (401/403): cookie dihapus dan sesi ditandai `needsRelogin`. Sesi lokal tetap
    ada, jadi aplikasi tetap bisa dipakai offline. Dashboard menampilkan ajakan login ulang, dan
    sinkron antrean berhenti sampai login berhasil.
  - Gagal karena jaringan atau 5xx: request dianggap gagal, tapi user tidak dikeluarkan.
- Semua hasil dibungkus `Result.Success` atau `Result.Error`. Pesan error diambil dari envelope
  `{ success, message, data, statusCode, meta?, errors? }`.

## 6. Logout

```
ProfileScreen → ProfileViewModel → AuthRepository.logout()
  → POST /auth/logout → cookieJar.clear() + sessionManager.clear()
```

Sesi lokal selalu dihapus, apa pun balasan server. Begitu `isLoggedIn` menjadi `false`, layar
kembali ke Login dengan cara yang sama seperti di poin 1.
