# Endpoint Plan (Sales Kanvas Plan)

Dokumentasi API modul Plan di `sales-kanvas-be`. Sumber: `src/plan/plan.controller.ts`, `plan.dto.ts`, `plan.service.ts`.

## Ringkasan

| Method  | Path                          | Permission   | Fungsi                         |
| ------- | ----------------------------- | ------------ | ------------------------------ |
| `GET`   | `/api/plan`                   | `plan:read`  | Daftar plan (paging + filter)  |
| `GET`   | `/api/plan/:id`               | `plan:read`  | Detail satu plan               |
| `POST`  | `/api/plan`                   | `plan:write` | Buat plan baru                 |
| `PATCH` | `/api/plan/:id`               | `plan:write` | Revisi plan (buat plan baru)   |
| `PATCH` | `/api/plan/:id/update-status` | `plan:write` | Ubah status plan               |

- Semua endpoint wajib header `Authorization: Bearer <access_token>`.
- Super Admin melewati cek permission; wildcard `plan:*` mencakup `plan:read` dan `plan:write`.
- **Tidak ada endpoint hapus.** Plan dibatalkan dengan mengubah status ke `BATAL`.

## Format respons

Sukses:

```json
{ "success": true, "message": "List Plan", "data": { }, "statusCode": 200 }
```

Gagal:

```json
{
  "success": false,
  "statusCode": 400,
  "message": "Validation failed",
  "errors": [{ "path": ["vehicle_id"], "message": "Invalid input: expected number, received undefined" }],
  "error": "Bad Request",
  "path": "/api/plan"
}
```

`errors` hanya ada untuk error validasi.

## Status plan

| Status      | Arti                                                        |
| ----------- | ----------------------------------------------------------- |
| `DRAFT`     | Baru dibuat (otomatis saat `POST`)                          |
| `DISETUJUI` | Disetujui                                                   |
| `LOADING`   | Barang sedang dimuat                                        |
| `KOMPLIT`   | Selesai (sudah totalan)                                     |
| `BATAL`     | Dibatalkan, atau plan lama yang sudah direvisi. **Terkunci:** tidak bisa direvisi atau diubah statusnya lagi |
| `REVISI`    | Ada di daftar enum, tetapi tidak di-set otomatis oleh BE    |

> BE **tidak membatasi urutan transisi** (mis. `KOMPLIT` → `DRAFT` diterima). Satu-satunya aturan: plan `BATAL` tidak bisa diubah.

---

## 1. `GET /api/plan`: daftar plan

### Query parameter

Semua opsional.

| Param                     | Tipe   | Default | Keterangan                                                          |
| ------------------------- | ------ | ------- | ------------------------------------------------------------------- |
| `page`                    | number | `1`     | Halaman                                                             |
| `limit`                   | number | `10`    | Jumlah per halaman, **maks. 100**                                   |
| `search`                  | string | –       | Cari nomor invoice (`contains`, tidak membedakan huruf besar/kecil) |
| `status`                  | string | –       | Salah satu status di atas                                           |
| `market_area_id`          | number | –       | Filter market area                                                  |
| `tanggalPergiKanvasStart` | string | –       | `tgl_pergi_kanvas >=` tanggal ini (`YYYY-MM-DD`)                    |
| `tanggalPergiKanvasEnd`   | string | –       | `tgl_pergi_kanvas <=` tanggal ini (`YYYY-MM-DD`)                    |
| `sortBy`                  | enum   | `id`    | `id`, `invoice`, `tgl_invoice`, `tgl_pergi_kanvas`, `created_at`    |
| `sortOrder`               | enum   | `desc`  | `asc`, `desc`                                                       |

Contoh:

```
GET /api/plan?page=1&limit=10&status=DRAFT&tanggalPergiKanvasStart=2026-10-01&tanggalPergiKanvasEnd=2026-10-31
```

### Respons `200`

```json
{
  "success": true,
  "message": "List Plan",
  "data": {
    "items": [ /* objek Plan lengkap, lihat bagian 2 */ ],
    "meta": { "page": 1, "limit": 10, "total": 42, "totalPages": 5 }
  },
  "statusCode": 200
}
```

---

## 2. `GET /api/plan/:id`: detail plan

### Respons `200`

```json
{
  "success": true,
  "message": "Plan found",
  "data": {
    "id": 12,
    "invoice": "SKS-20261025-0001",
    "tgl_invoice": "2026-10-25T00:00:00.000Z",
    "tgl_pergi_kanvas": "2026-10-26T00:00:00.000Z",
    "tgl_kembali_kanvas": null,
    "market_area_id": 1,
    "vehicle_id": 1,
    "status": "DRAFT",
    "note": null,
    "revision": 0,
    "revisi_to": null,
    "cancelled_at": null,
    "created_at": "2026-10-25T03:12:45.000Z",
    "updated_at": null,
    "user_id": 2,
    "ip": "::1",
    "vehicle": { "id": 1, "name": "Truk 1", "nopol": "K 1234 AB", "type_vehicle": "Truk", "type_fuel": "Solar" },
    "marketArea": { "id": 1, "name": "Pati" },
    "salesKanvasPersonil": [
      { "id": 1, "employee_id": 3, "position": "Sopir", "employee": { "id": 3, "name": "Budi", "nip": "001" } }
    ],
    "salesKanvasPlanDetail": [
      { "id": 1, "product_id": 5, "qty": 10, "product": { "id": 5, "code": "JNG-01", "name": "Jenang Original" } }
    ],
    "salesKanvasKasbonMaterial": [
      { "id": 1, "kasbon_material_type_id": 2, "price": "50000", "qty": 1, "kasbonMaterialType": { "id": 2, "name": "Solar" } }
    ]
  },
  "statusCode": 200
}
```

Isi objek relasi (`vehicle`, `employee`, `product`, dst.) adalah record lengkap dari tabel master; contoh di atas dipersingkat.

- **Tanggal:** dikirim dalam format ISO UTC.
- **`price`:** berupa **string**, karena kolomnya bertipe Decimal di database.
- **Error:** `404` bila id tidak ditemukan.

---

## 3. `POST /api/plan`: buat plan

### Body

```json
{
  "tgl_invoice": "2026-10-25",
  "tgl_pergi_kanvas": "2026-10-26",
  "tgl_kembali_kanvas": null,
  "market_area_id": 1,
  "vehicle_id": 1,
  "note": "Rute utara",
  "salesKanvasPlanPersonil": [
    { "employee_id": 3, "position": "Sopir" },
    { "employee_id": 4, "position": "Sales" }
  ],
  "salesKanvasPlanDetail": [
    { "product_id": 5, "qty": 10 }
  ],
  "salesKanvasKasbonMaterial": [
    { "kasbon_material_type_id": 2, "price": 50000, "qty": 1 }
  ]
}
```

| Field                       | Tipe           | Wajib | Aturan                                    |
| --------------------------- | -------------- | ----- | ----------------------------------------- |
| `tgl_invoice`               | string         | ✅    | Tanggal, disarankan `YYYY-MM-DD`          |
| `tgl_pergi_kanvas`          | string         | ✅    | Tanggal                                   |
| `tgl_kembali_kanvas`        | string \| null | –     | Tanggal                                   |
| `market_area_id`            | number         | ✅    | id market area                            |
| `vehicle_id`                | number         | ✅    | Bilangan bulat > 0                        |
| `note`                      | string \| null | –     |                                           |
| `salesKanvasPlanPersonil`   | array          | ✅    | Boleh kosong `[]`                         |
| ↳ `employee_id`             | number         | ✅    | Bilangan bulat > 0                        |
| ↳ `position`                | string         | ✅    | Bebas, mis. `Sopir`, `Sales`, `Helper`    |
| `salesKanvasPlanDetail`     | array          | ✅    | Boleh kosong `[]`                         |
| ↳ `product_id`              | number         | ✅    | > 0                                       |
| ↳ `qty`                     | number         | ✅    | > 0                                       |
| `salesKanvasKasbonMaterial` | array          | –     | Boleh tidak dikirim                       |
| ↳ `kasbon_material_type_id` | number         | ✅    | Bilangan bulat > 0                        |
| ↳ `price`                   | number         | ✅    | > 0                                       |
| ↳ `qty`                     | number         | ✅    | Bilangan bulat > 0                        |

Aturan:

- **`status`:** tidak bisa dikirim. Plan baru selalu `DRAFT`.
- **`invoice`:** dibuat otomatis dengan format `SKS-YYYYMMDD-NNNN`, nomor urut per tanggal invoice.
- **Field tambahan:** field yang tidak ada di tabel ditolak (`400 Unrecognized key`), karena skemanya `strict`.
- **Referensi tidak ada:** id market area, kendaraan, karyawan, produk, atau jenis kasbon yang tidak ada di database menghasilkan `400 Invalid reference`.

### Respons `201`

`message: "Plan created successfully"`, `data` berisi objek Plan lengkap seperti di bagian 2.

---

## 4. `PATCH /api/plan/:id`: revisi plan

Body sama dengan `POST`, tetapi **semua field opsional**. Field yang tidak dikirim mengambil nilai dari plan lama, termasuk array personil, produk, dan kasbon. Bila salah satu array dikirim, array itu **menggantikan seluruh** isi array lama.

Yang terjadi di BE (dalam satu transaksi):

1. **Plan lama ditutup:** di-set `status = BATAL`, `cancelled_at = now`, dan `revisi_to` diisi invoice baru.
2. **Plan baru dibuat:**
    - **id** dan **invoice** baru.
    - `revision` = revision lama + 1.
    - `status` mewarisi status plan lama.

### Respons `200`

`message: "Plan revised successfully"`, `data` berisi **plan baru** (id baru). Gunakan `data.id` untuk navigasi berikutnya.

### Error

| Kode  | Kapan                                                                 |
| ----- | --------------------------------------------------------------------- |
| `400` | Plan sudah `BATAL`: `"Tidak dapat merevisi plan yang sudah dibatalkan"` |
| `404` | id tidak ditemukan                                                    |

---

## 5. `PATCH /api/plan/:id/update-status`: ubah status

### Body

```json
{ "status": "DISETUJUI" }
```

`status` wajib berisi salah satu: `DRAFT`, `DISETUJUI`, `BATAL`, `REVISI`, `LOADING`, `KOMPLIT`.

### Respons `200`

`message: "Plan status updated successfully"`, `data` berisi record plan **tanpa relasi**.

### Error

| Kode  | Kapan                                                                         |
| ----- | ----------------------------------------------------------------------------- |
| `400` | Plan sudah `BATAL`: `"Tidak dapat mengubah status plan yang sudah dibatalkan"` |
| `400` | `status` bukan nilai enum yang valid                                          |
| `404` | id tidak ditemukan                                                            |

**Aman dikirim ulang:** bila `status` yang diminta **sama dengan status saat ini**, server membalas `200` dengan record apa adanya (tanpa perubahan). Ini berlaku juga untuk `BATAL` yang terkirim dua kali (mis. outbox HP setelah timeout), jadi tidak lagi ditolak `400`. Mengubah plan `BATAL` ke status **lain** tetap ditolak.

Catatan: mengubah status ke `BATAL` lewat endpoint ini **tidak** mengisi `cancelled_at`; kolom itu hanya diisi saat revisi.

---

## Header `Idempotency-Key` (POST & revisi)

`POST /api/plan` dan `PATCH /api/plan/:id` (revisi) menerima header opsional:

```
Idempotency-Key: 3f6c2a1e-8b4d-4c1a-9e2f-7d5b6a4c3e21
```

- **Gunanya:** request yang dikirim ulang (mis. outbox HP setelah timeout, padahal server sudah memproses) **tidak membuat plan dobel**. Kiriman dengan key yang sama mengembalikan plan yang dulu dibuat, dengan `id` dan `invoice` yang sama.
- **Format:** 1–100 karakter `A–Z a–z 0–9 _ -` (UUID cocok). Format lain ditolak `400 "Idempotency-Key tidak valid"`.
- **Satu key = satu operasi.** Buat key baru untuk setiap plan atau revisi baru, dan pakai key yang sama hanya saat **mengirim ulang** operasi yang sama. Isi body kiriman ulang tidak dibandingkan.
- **Revisi:** kiriman ulang revisi yang sudah sukses mengembalikan plan hasil revisi, bukan `400 "plan sudah dibatalkan"`.
- **Tanpa header:** perilaku biasa, setiap request membuat plan baru.
- **Tidak berlaku untuk** `PATCH /update-status`, karena mengirim status yang sama dua kali hasilnya tetap sama.

Key disimpan di kolom unik `ts_sales_kanvas_plan.idempotency_key`, jadi dua kiriman bersamaan dengan key yang sama juga hanya menghasilkan satu plan.

---

## Error umum (semua endpoint)

| Kode  | Kapan                                                     |
| ----- | --------------------------------------------------------- |
| `400` | Validasi gagal (`errors` berisi detail per field)         |
| `401` | Token tidak ada / kedaluwarsa, lakukan refresh token      |
| `403` | User tidak punya permission `plan:read` / `plan:write`    |
| `404` | Data tidak ditemukan                                      |
