# Integrasi Android ↔ Backend NestJS (My Mubarok ERP)

Panduan untuk membangun **aplikasi Android** yang terhubung ke backend NestJS ini, mulai dari
**autentikasi** (login + bentuk response + token), penanganan **envelope & error**, **permission**,
hingga endpoint **insert data Sales Kanvas Sale** (`POST /api/sales-kanvas/sale`).

Contoh kode memakai **Kotlin + Retrofit2 + OkHttp**.

---

## 0. Dasar koneksi

| Item | Nilai |
|---|---|
| Prefix global | `/api` (semua endpoint diawali ini) |
| Base URL (emulator → host) | `http://10.0.2.2:3000/api/` |
| Base URL (device fisik, 1 jaringan) | `http://<IP-LAN-server>:3000/api/` |
| Base URL (produksi) | `https://<domain>/api/` |
| Format tanggal | string `YYYY-MM-DD` atau ISO; backend mengonversi ke `Date` |
| Content-Type | `application/json` |

> **Emulator Android** tidak bisa pakai `localhost` (itu merujuk ke emulator sendiri). Gunakan
> `10.0.2.2` untuk menembak `localhost` mesin host.

### Cleartext (HTTP) saat development
Android 9+ memblokir HTTP non-TLS secara default. Untuk dev (server `http://`), tambahkan
`res/xml/network_security_config.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
  <domain-config cleartextTrafficPermitted="true">
    <domain includeSubdomains="true">10.0.2.2</domain>
    <!-- tambahkan IP LAN server bila pakai device fisik -->
  </domain-config>
</network-security-config>
```
lalu di `AndroidManifest.xml` → `<application android:networkSecurityConfig="@xml/network_security_config" ...>`.
Produksi memakai HTTPS, jadi blok ini tidak diperlukan.

---

## 1. Bentuk response (envelope) & error

**Semua** response sukses dibungkus envelope global:
```jsonc
{
  "success": true,
  "message": "….",
  "data": { /* atau [ … ] */ },
  "statusCode": 200,
  "meta": { "page": 1, "limit": 10, "total": 42, "totalPages": 5 }  // hanya untuk list
}
```

**Error validasi (400)** — Zod global, field tak dikenal/tidak valid ditolak:
```jsonc
{
  "success": false,
  "message": "Validation failed",
  "errors": { "vehicle_id": "Required", "salesKanvasDetail.0.qty": "..." },
  "statusCode": 400,
  "timestamp": "2026-06-14T07:00:00.000Z",
  "path": "/api/sales-kanvas/sale"
}
```
`errors` = map `{ "namaField": "pesan" }` (satu pesan per field; error level-form di key `form`).

**Error lain:** `401` (belum/silakan login), `403` (login tapi tak punya permission),
`404`/`409` (mis. FK/duplikat dari Prisma) — semuanya `{ success:false, message, statusCode }`.

### Model envelope di Kotlin
```kotlin
data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T?,
    val statusCode: Int,
    val meta: Meta? = null,
    val errors: Map<String, String>? = null
)

data class Meta(val page: Int, val limit: Int, val total: Int, val totalPages: Int)
```

---

## 2. Autentikasi

### ⚠️ Hal terpenting: token disimpan di httpOnly cookie
Saat login, backend **tidak** mengembalikan token di body. Token dikirim lewat header
`Set-Cookie`:
- `access_token` — JWT, masa berlaku **1 jam**.
- `refresh_token` — masa berlaku **7 hari**.

Atribut cookie: dev → `secure=false`, `SameSite=Lax`; produksi → `secure=true`, `SameSite=None` (butuh HTTPS).
`SameSite` hanya relevan untuk browser; OkHttp native cukup menyimpan & mengirim ulang cookie berdasar domain/path.

Backend menerima token dengan urutan: **cookie `access_token` dulu**, lalu fallback header
`Authorization: Bearer <token>`. Jadi Android punya **dua pilihan**:

| Pendekatan | Cara | Rekomendasi |
|---|---|---|
| **A. CookieJar** | OkHttp menyimpan cookie dari login dan mengirim ulang otomatis | ✅ Paling sederhana, sesuai desain BE |
| **B. Bearer** | Ambil nilai `access_token` dari header `Set-Cookie` login, simpan, kirim sebagai `Authorization: Bearer` | Dipakai bila ingin kontrol token manual |

---

### 2.1 Endpoint Auth

| Method | Endpoint | Body | Auth | Aksi |
|---|---|---|---|---|
| POST | `/api/auth/login` | `{ username, password }` | — | Login; set cookie `access_token` + `refresh_token` |
| POST | `/api/auth/refresh` | — (butuh cookie `refresh_token`) | cookie | Terbitkan `access_token` baru |
| POST | `/api/auth/logout` | — | access token | Revoke sesi + hapus cookie |
| GET  | `/api/auth/profile` | — | access token | Data user + permission |

> Rate limit: login 5×/menit, refresh 10×/menit.

### 2.2 Login — request
```jsonc
POST /api/auth/login
{ "username": "salesman01", "password": "rahasia123" }
```
Validasi: `username` 3–255 char, `password` min 6 char.

### 2.3 Login — response (body)
Status `201`. Body **tanpa token** (token ada di `Set-Cookie`):
```jsonc
{
  "success": true,
  "message": "Login successful",
  "data": {
    "user": {
      "id": 7,
      "username": "salesman01",
      "is_active": true,
      "last_login_at": "2026-06-14T07:00:00.000Z",
      "employee": { "id": 3, "name": "Andi", "tgl_lahir": null, "tgl_masuk": "2024-01-10" },
      "role": { "id": 2, "name": "Sales" },
      "permissions": [
        { "id": 11, "name": "sales-kanvas:write" },
        { "id": 12, "name": "sales-kanvas:read:full" }
        // …permission lain
      ]
    }
  },
  "statusCode": 201
}
```
Header response (penting): `Set-Cookie: access_token=…; HttpOnly; …` dan `Set-Cookie: refresh_token=…; HttpOnly; …`.

> **Simpan `data.user.permissions`** di app untuk menyembunyikan/menonaktifkan menu sesuai hak akses
> (mis. tampilkan tombol "Simpan Penjualan" hanya bila ada `sales-kanvas:write`).

### 2.4 Profile — response
`GET /api/auth/profile` → `data` = `{ id, username, last_login_at, created_at, is_active, employee, role, permissions[] }`.

### 2.5 Refresh
`access_token` hidup 1 jam. Bila request mengembalikan `401`, panggil `POST /api/auth/refresh`
(mengandalkan cookie `refresh_token`), lalu **ulangi** request asli. Bila refresh juga `401`
(refresh kedaluwarsa / sesi dicabut) → arahkan user **login ulang**.

---

### 2.6 Setup Retrofit + CookieJar (Pendekatan A — rekomendasi)

Tambah dependency:
```kotlin
// build.gradle (app)
implementation "com.squareup.retrofit2:retrofit:2.11.0"
implementation "com.squareup.retrofit2:converter-gson:2.11.0"
implementation "com.squareup.okhttp3:okhttp:4.12.0"
implementation "com.github.franmontiel:PersistentCookieJar:v1.0.1" // cookie jar persisten
```

```kotlin
val cookieJar = PersistentCookieJar(
    SetCookieCache(),
    SharedPrefsCookiePersistor(context)
)

val client = OkHttpClient.Builder()
    .cookieJar(cookieJar)           // simpan & kirim ulang cookie otomatis
    .addInterceptor(authRefreshInterceptor) // lihat 2.7
    .build()

val retrofit = Retrofit.Builder()
    .baseUrl("http://10.0.2.2:3000/api/")
    .client(client)
    .addConverterFactory(GsonConverterFactory.create())
    .build()
```

Dengan CookieJar, setelah `login()` berhasil semua request berikutnya otomatis membawa
cookie `access_token` — tidak perlu menambah header manual.

### 2.7 Auto-refresh saat 401 (Authenticator)
```kotlin
val authRefreshInterceptor = okhttp3.Authenticator { _, response ->
    if (response.request.url.encodedPath.endsWith("/auth/refresh")) return@Authenticator null
    // panggil /auth/refresh secara sinkron (cookie refresh_token ikut via cookieJar)
    val refreshOk = runBlocking { authApi.refresh().isSuccessful }
    if (refreshOk) response.request.newBuilder().build() else null // ulangi request asli
}
```

### 2.8 Alternatif Bearer (Pendekatan B)
Bila tidak memakai CookieJar: baca header `Set-Cookie` dari response login, ekstrak nilai
`access_token`, simpan (mis. EncryptedSharedPreferences), lalu kirim di tiap request:
```kotlin
// Ekstraksi sederhana dari header Set-Cookie
val token = loginResponse.headers().values("Set-Cookie")
    .firstOrNull { it.startsWith("access_token=") }
    ?.substringAfter("access_token=")?.substringBefore(";")

// Interceptor menambahkan header
.addInterceptor { chain ->
    val req = chain.request().newBuilder()
        .apply { token?.let { addHeader("Authorization", "Bearer $it") } }
        .build()
    chain.proceed(req)
}
```
Refresh-nya tetap butuh cookie `refresh_token`, jadi Pendekatan A umumnya lebih praktis.

### 2.9 Retrofit interface — Auth
```kotlin
data class LoginRequest(val username: String, val password: String)
data class LoginData(val user: UserProfile)
data class UserProfile(
    val id: Int, val username: String, val is_active: Boolean,
    val role: RoleRef?, val employee: EmployeeRef?, val permissions: List<PermissionRef>
)
data class RoleRef(val id: Int, val name: String)
data class EmployeeRef(val id: Int, val name: String)
data class PermissionRef(val id: Int, val name: String)

interface AuthApi {
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): Response<ApiResponse<LoginData>>

    @POST("auth/refresh")
    suspend fun refresh(): Response<ApiResponse<LoginData>>

    @POST("auth/logout")
    suspend fun logout(): Response<ApiResponse<Unit>>

    @GET("auth/profile")
    suspend fun profile(): Response<ApiResponse<UserProfile>>
}
```

---

## 3. Insert Sales Kanvas Sale

### 3.1 Endpoint
| Method | Endpoint | Permission | Sukses |
|---|---|---|---|
| POST | `/api/sales-kanvas/sale` | `sales-kanvas:write` | `201` |

Guard: `JwtAuthGuard` + `PermissionsGuard` (fail-closed). User wajib login **dan** punya
permission `sales-kanvas:write` (atau wildcard `sales-kanvas:*` / Super Admin).

### 3.2 Request body
```jsonc
POST /api/sales-kanvas/sale
{
  "sales_kanvas_plan_id": 12,        // wajib — plan trip yang sudah DISETUJUI/aktif
  "customer_id": 10,                 // wajib
  "type_transaction": "TUNAI",       // wajib — "TUNAI" | "KREDIT"
  "group_sale_id": 1,                // wajib — 1 Jual, 2 Sumbangan, 3 Sample
  "term_of_payment": 0,              // wajib — tempo hari (KREDIT diisi, TUNAI 0)
  "tgl_invoice": "2026-06-14",       // wajib
  "total": 1000000,                  // wajib — > 0, penjualan kotor (A)
  "note": "Penjualan hari-1",        // opsional

  "salesKanvasDetail": [             // wajib — minimal isi 1 baris produk
    {
      "product_id": 1, "qty": 10, "price": 50000,   // qty & price harus > 0
      "batch": "B001",                              // opsional
      "tgl_production": "2026-05-01",               // opsional
      "tgl_expired": "2026-12-01"                   // opsional
    },
    { "product_id": 2, "qty": 5, "price": 100000 }
  ],

  "salesKanvasPayment": [            // OPSIONAL (mis. TUNAI). Hilangkan untuk KREDIT tanpa bayar.
    {
      "total_discount": 0,           // >= 0
      "total_retur": 50000,
      "total": 1000000,              // > 0
      "grand_total": 950000,         // > 0  (total - diskon - retur)
      "payment_method": "CASH",      // "CASH" | "TRANSFER" | "QRIS" | "KARTU" (nullable)
      "payment_date": "2026-06-14",  // nullable
      "amount_received": 1000000,    // > 0
      "payment_remaining": 0,        // >= 0
      "bank_id": null,               // opsional (non-tunai)
      "bank_type": null,             // opsional "DEBIT"/"CREDIT"
      "atas_nama": null,             // opsional
      "payment_reference": null,     // opsional
      "note": null                   // opsional
    }
  ],

  "salesKanvasRetur": {              // OPSIONAL — retur saat transaksi
    "customer_id": 10,
    "tgl_invoice": "2026-06-14",
    "total": 50000,                  // > 0
    "salesKanvasReturDetail": [
      {
        "product_id": 1, "qty": 1, "price": 50000,   // qty & price > 0
        "batch": "B001",                              // wajib di retur
        "retur_condition": "RUSAK",                   // wajib (mis. RUSAK/EXPIRED)
        "tgl_expired": null,                          // opsional
        "note": "kemasan penyok"                      // opsional
      }
    ]
  }
}
```

**Catatan field:**
- Body memakai `.strict()` → **field asing ditolak** (`400`).
- **Jangan kirim** `user_id` & `ip` — diisi otomatis backend dari token & IP.
- Tidak ada lagi `transaction_status` (sudah dihapus) maupun `change_amount` pada payment (sudah dihapus).
- `invoice` digenerate otomatis (prefix `SK-`); retur otomatis (prefix `SKR-`).

### 3.3 Response sukses (201)
```jsonc
{
  "success": true,
  "message": "Sales kanvas created successfully",
  "data": {
    "id": 55,
    "invoice": "SK-20260614-0001",
    "sales_kanvas_plan_id": 12,
    "customer_id": 10,
    "type_transaction": "TUNAI",
    "group_sale_id": 1,
    "term_of_payment": 0,
    "tgl_invoice": "2026-06-14T00:00:00.000Z",
    "total": "1000000",
    "note": "Penjualan hari-1",
    "salesKanvasDetail": [ { "id": 1, "product_id": 1, "qty": 10, "price": "50000", "batch": "B001", "tgl_production": "2026-05-01T00:00:00.000Z", "tgl_expired": "2026-12-01T00:00:00.000Z" }, … ],
    "salesKanvasPayment": [ { "id": 1, "payment_method": "CASH", "grand_total": "950000", "amount_received": "1000000", "payment_remaining": "0", … } ],
    "salesKanvasRetur": {
      "id": 9, "invoice": "SKR-20260614-0001", "total": "50000",
      "salesKanvasReturDetail": [ { "id": 1, "product_id": 1, "qty": 1, "batch": "B001", "retur_condition": "RUSAK", "price": "50000" } ]
    }
  },
  "statusCode": 201
}
```
> Nilai Decimal (mis. `total`, `price`, `grand_total`) dikirim sebagai **string** oleh Prisma. Di Kotlin parse ke `String`/`BigDecimal`, jangan `Double` agar presisi rupiah aman.

### 3.4 Kemungkinan error
| Status | Sebab |
|---|---|
| `400` | Validasi Zod (field wajib hilang, `qty`/`price`/`total` ≤ 0, enum salah, field asing) |
| `401` | Belum login / access token kedaluwarsa → coba refresh |
| `403` | Login tapi tak punya `sales-kanvas:write` |
| `409`/`400` | FK invalid (mis. `customer_id` tak ada) → P2003 dari PrismaErrorHandler |

### 3.5 Retrofit interface + model — Create Sale
```kotlin
// ---- Request models ----
data class CreateSaleRequest(
    val sales_kanvas_plan_id: Int,
    val customer_id: Int,
    val type_transaction: String,          // "TUNAI" | "KREDIT"
    val group_sale_id: Int,
    val term_of_payment: Int,
    val tgl_invoice: String,               // "2026-06-14"
    val total: Long,
    val note: String? = null,
    val salesKanvasDetail: List<SaleDetail>,
    val salesKanvasPayment: List<SalePayment>? = null,
    val salesKanvasRetur: SaleRetur? = null
)
data class SaleDetail(
    val product_id: Int, val qty: Int, val price: Long,
    val batch: String? = null, val tgl_production: String? = null, val tgl_expired: String? = null
)
data class SalePayment(
    val total_discount: Long, val total_retur: Long, val total: Long, val grand_total: Long,
    val payment_method: String?, val payment_date: String?,
    val amount_received: Long, val payment_remaining: Long,
    val bank_id: Int? = null, val bank_type: String? = null, val atas_nama: String? = null,
    val payment_reference: String? = null, val note: String? = null
)
data class SaleRetur(
    val customer_id: Int, val tgl_invoice: String, val total: Long,
    val salesKanvasReturDetail: List<SaleReturDetail>
)
data class SaleReturDetail(
    val product_id: Int, val qty: Int, val price: Long,
    val batch: String, val retur_condition: String,
    val tgl_expired: String? = null, val note: String? = null
)

// ---- Response model (ambil seperlunya) ----
data class SaleData(val id: Int, val invoice: String, val total: String)

interface SalesKanvasApi {
    @POST("sales-kanvas/sale")
    suspend fun createSale(@Body body: CreateSaleRequest): Response<ApiResponse<SaleData>>
}
```

### 3.6 Contoh pemanggilan + penanganan error
```kotlin
suspend fun submitSale(req: CreateSaleRequest) {
    val res = salesKanvasApi.createSale(req)
    when {
        res.isSuccessful -> {
            val invoice = res.body()?.data?.invoice
            // tampilkan sukses, simpan invoice
        }
        res.code() == 401 -> { /* refresh lalu retry, atau login ulang */ }
        res.code() == 403 -> { /* user tak punya izin sales-kanvas:write */ }
        res.code() == 400 -> {
            val errs = parseErrors(res.errorBody())   // map field → pesan
            // tandai field bermasalah di form
        }
        else -> { /* tampilkan message dari body error */ }
    }
}
```

---

## 4. Ringkas alur Android

1. **Login** → `POST /auth/login` → cookie tersimpan di CookieJar + simpan `permissions`.
2. **Cek izin** → tampilkan aksi sesuai `permissions` (`sales-kanvas:write` untuk simpan penjualan).
3. **Ambil master** (plan aktif, customer, product, group sale, bank) lewat endpoint masing-masing untuk mengisi dropdown form.
4. **Submit penjualan** → `POST /sales-kanvas/sale` dengan body bagian 3.2.
5. **401 di tengah jalan** → `POST /auth/refresh` → ulangi request; bila gagal → login ulang.
6. **Logout** → `POST /auth/logout`.

> Endpoint master (customer/product/plan/dll.) mengikuti pola modul masing-masing; banyak modul
> menyediakan `GET …/select-options` (butuh sekadar login) untuk mengisi dropdown. Cek dokumen modul terkait.
