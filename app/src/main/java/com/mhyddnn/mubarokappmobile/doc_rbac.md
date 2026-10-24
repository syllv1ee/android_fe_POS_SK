# Dokumentasi Sistem RBAC (Role-Based Access Control)

Sistem otorisasi dalam proyek ini menggunakan pendekatan RBAC (Role-Based Access Control) yang sangat granular berbasis **Permission**.

## 1. Konsep Utama

Sistem ini terdiri dari tiga komponen utama:
- **Permission**: Hak akses spesifik (contoh: `product:write`, `employee:read:full`).
- **Role**: Kumpulan dari satu atau lebih Permission (contoh: `Manager`, `Staff Kasir`).
- **User**: Setiap pengguna dikaitkan dengan tepat satu **Role**.

### Karakteristik Khusus
- **Super Admin Bypass**: Role dengan ID `1` atau nama `Super Admin` secara otomatis memiliki akses ke semua endpoint tanpa pengecekan permission. Lihat detail di [Bagian 6](#6-super-admin-bypass-penanganan--disclaimer).
- **Wildcard Support**: Mendukung penggunaan wildcard `*` (contoh: `product:*` memberikan akses ke semua action di resource product).
- **Audit Fields**: Setiap pembuatan atau pembaruan data Role/Permission akan mencatat `user_id` dan `ip` address pelakunya.

## 2. Struktur Data (Prisma)

- `Role` (`ms_role`): Menyimpan nama role.
- `Permission` (`ms_permission`): Menyimpan string permission yang unik.
- `PermissionRole` (`ms_permission_role`): Tabel junction yang menghubungkan Role dengan Permission.

## 3. API Reference

Semua endpoint dilindungi oleh `JwtAuthGuard` dan `PermissionsGuard`.

### Management Permission (`/permission`)

| Method | Endpoint | Permission Required | Deskripsi |
| :--- | :--- | :--- | :--- |
| `GET` | `/permission` | `permission:read:full` | List semua permission dengan pagination & search |
| `GET` | `/permission/:id` | `permission:read:full` | Detail satu permission |
| `POST` | `/permission` | `permission:write` | Membuat permission baru |
| `PATCH` | `/permission/:id` | `permission:write` | Update nama permission |
| `DELETE` | `/permission/:id` | `permission:delete` | Menghapus permission |
| `GET` | `/permission/select-options` | - | List permission untuk dropdown |

### Management Role (`/role`)

| Method | Endpoint | Permission Required | Deskripsi |
| :--- | :--- | :--- | :--- |
| `GET` | `/role` | `role:read:full` | List semua role dengan pagination & search |
| `GET` | `/role/:id` | `role:read:full` | Detail satu role beserta daftar permission-nya |
| `POST` | `/role` | `role:write` | Membuat role baru + mapping permission |
| `PATCH` | `/role/:id` | `role:write` | Update nama role & sinkronisasi permission |
| `DELETE` | `/role/:id` | `role:delete` | Menghapus role (Proteksi jika masih ada user) |
| `GET` | `/role/select-options` | - | List role untuk dropdown |

## 4. Cara Penggunaan (Contoh Payload)

### Membuat Role dengan Permission
`POST /role`
```json
{
  "name": "Manager Gudang",
  "permission_ids": [10, 11, 15] 
}
```

### Update Role (Sinkronisasi Permission)
`PATCH /role/5`
```json
{
  "name": "Manager Gudang Senior",
  "permission_ids": [10, 11, 15, 20]
}
```
*Sistem akan otomatis menghapus mapping lama dan menggantinya dengan yang baru dalam satu transaksi.*

## 5. Implementasi di Kode (Backend)

Gunakan decorator `@RequirePermission` pada method controller:

```typescript
@Post()
@RequirePermission('product:write')
create(@Body() dto: CreateProductDto) {
  return this.productService.create(dto);
}
```

## 6. Super Admin Bypass: Penanganan & Disclaimer

Bypass Super Admin di-handle di `PermissionsGuard.isSuperAdmin()`
(`src/common/guards/permission.guard.ts`). Jika user terdeteksi Super Admin,
**semua `@RequirePermission` dilewati** dan user dapat akses penuh ke seluruh
endpoint tanpa perlu permission apa pun.

### Logika deteksi (saat ini)

User dianggap Super Admin jika **salah satu** kondisi berikut benar (OR):

```ts
const roleName = user.role?.name?.toLowerCase();
roleName === 'super admin'
|| roleName === 'superadmin'
|| roleName === 'super-admin'
|| user.role?.id === 1   // fallback: asumsi role_id 1 = Super Admin
```

Artinya:
- Yang dicek adalah **nama role** (`user.role.name`), bukan harus persis ejaan
  `SuperAdmin`. Tiga variasi penulisan diterima (case-insensitive): `super admin`,
  `superadmin`, `super-admin`.
- Ada **fallback ke `role.id === 1`** — walau nama tidak cocok, role ID 1 tetap
  dianggap Super Admin.

### Cara handle yang dianjurkan
1. **Patok satu ejaan baku** saat seeding, mis. role bernama `Super Admin`.
   Variasi penulisan lain hanya jaring pengaman, jangan diandalkan.
2. **Andalkan nama role, bukan ID.** Fallback `role.id === 1` rapuh: jika
   urutan seeding berubah atau DB di-reset dengan urutan lain, role ID 1 bisa
   jadi **bukan** Super Admin — akibatnya role yang salah dapat akses penuh,
   atau Super Admin asli (ID ≠ 1) malah tertolak.
3. **Konsistenkan antara seed dan guard.** Setiap kali menambah variasi nama,
   pastikan ikut ditambahkan di `isSuperAdmin()`.

### Catatan untuk masa development
Untuk menguji semua endpoint (mis. dari aplikasi Android) tanpa harus seed
setiap permission satu per satu, cukup login dengan user yang role-nya
terdeteksi Super Admin. Verifikasi role user:

```sql
SELECT u.id, u.username, r.id AS role_id, r.name AS role_name
FROM ms_user u
JOIN ms_role r ON r.id = u.role_id;
```

### Rencana perbaikan (opsional, belum diimplementasi)
Ketergantungan ke nama string + ID hardcode kurang ideal. Lebih kokoh dengan
kolom boolean eksplisit di model `Role`, mis. `is_super_admin`, lalu guard cukup
mengecek `user.role?.is_super_admin === true`. Ini menghilangkan ketergantungan
pada ejaan nama maupun urutan ID. Perlu migrasi + penyesuaian `isSuperAdmin()`.
