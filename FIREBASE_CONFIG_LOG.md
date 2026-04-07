# Konfigurasi Index Firestore - NurseFlow

File ini digunakan untuk mencatat indeks komposit yang telah dikonfigurasi di Firebase Console agar sinkron dengan query di kode aplikasi.

## Indeks Komposit Saat Ini

| Collection ID | Fields Indexed | Query Scope | Status | Kegunaan |
| :--- | :--- | :--- | :--- | :--- |
| **`notifications`** | `userId` (Asc), `timestamp` (Desc), `__name__` (Desc) | Collection | **Enabled** | Memuat notifikasi per user urut waktu terbaru |
| **`handovers`** | `status` (Asc), `timestamp` (Desc), `__name__` (Desc) | Collection | **Enabled** | Menampilkan handover berdasarkan status (PENDING/ACCEPTED) |
| **`handover_tasks`** | `handoverId` (Asc), `timestamp` (Asc), `__name__` (Asc) | Collection | **Enabled** | Menampilkan checklist tugas urut berdasarkan waktu pembuatan |

## Indeks yang Disarankan (Potensial)
| Collection ID | Fields Indexed | Status | Kegunaan |
| :--- | :--- | :--- | :--- |
| **`vital_signs`** | `patientId` (Asc), `timestamp` (Desc) | *Planned* | Menampilkan riwayat tanda vital pasien dari terbaru |
| **`requests`** | `toUid` (Asc), `createdAt` (Desc) | *Planned* | Menampilkan permintaan pertemanan urut waktu terbaru |

## Troubleshooting & Resolusi Error

### 1. Error `FAILED_PRECONDITION`
**Penyebab:** Query menggunakan `.where()` dan `.orderBy()` pada field berbeda tanpa indeks komposit.
**Solusi:** Klik link di Logcat untuk membuat indeks otomatis di Firebase Console.

### 2. Warning `CustomClassMapper` (Data Tidak Muncul di App)
**Penyebab:** Nama field di Firestore berbeda dengan nama variabel di Model Kotlin (misal: `completed` vs `isCompleted`).
**Solusi:** 
- Gunakan `@get:PropertyName("nama_di_firestore")` di Model Kotlin.
- Atau pastikan nama di database disamakan (case-sensitive).

### 3. Error `PERMISSION_DENIED`
**Penyebab:** Aturan di `firestore.rules` memblokir akses.
**Solusi:** Pastikan rules mengizinkan `read` jika `request.auth.uid == resource.data.userId`.
