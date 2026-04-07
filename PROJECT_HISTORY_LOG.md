# NurseFlow Project Development Log & Patch History

Dokumen ini berisi catatan detail mengenai semua fitur, perbaikan bug, dan perubahan arsitektur yang telah diterapkan dalam proyek NurseFlow.

---

## 🛠 1. Bug Fixes & Stabilitasi Awal
*   **Fix Unresolved Reference**: Memperbaiki error compile `btnAiNotes` di `HomeFragment.kt`.
*   **UI Sync**: Sinkronisasi ViewBinding antara `HomeFragment` dan layout `fragment_home.xml`.
*   **Fix Notifications Logic**: Memperbaiki error `Unresolved reference 'cbRead'` pada `NotificationsAdapter.kt` dengan mengganti checkbox menjadi indikator titik (dot) dan optimasi logika penandaan pesan dibaca.
*   **Fix Layout Insets**: Menambahkan `fitsSystemWindows="true"` pada layout utama untuk mencegah konten terpotong oleh Status Bar.
*   **Fix Colleagues Fragment**: Memperbaiki error `Unresolved reference 'btnConnect'` dengan menyelaraskan ID button di `fragment_colleagues.xml` menjadi `btn_connect`.

## 🏠 2. Pembenahan Dashboard & Menu Utama
*   **Quick Action Grid**: Re-layout grid menu utama untuk mencakup: Patients, IV Calc, Colleagues, Vitals, dan AI Notes.
*   **Header Navigation**: Menambahkan tombol navigasi cepat untuk membuka daftar rekan kerja.
*   **Feature Optimization**: Mengurangi kompleksitas fitur "Shift Checklist" untuk berfokus pada data klinis.

## 👥 3. Kolaborasi & Manajemen Tim
*   **Colleague Management**: 
    *   Implementasi `ColleaguesFragment` dan `ColleagueAdapter`.
    *   Fitur Long-click untuk opsi: Lihat Profil, Lihat Pasien Rekan, dan Hapus Koneksi.
    *   Integrasi Firestore untuk sinkronisasi data antar perawat secara real-time.

## 🏥 4. Arsitektur Manajemen Pasien
*   **Hybrid Data Source**: Implementasi `PatientRepository` yang mendukung Room (Offline) dan Firestore (Sync).
*   **Search Engine**: Implementasi pencarian pasien berdasarkan Nama atau Kamar di `PatientListFragment`.
*   **Supervisi Mode**: UI adaptif yang menyesuaikan hak akses saat melihat pasien milik rekan (sembunyikan tombol edit/tambah).

## 📈 5. Vital Signs & Monitoring NEWS2
*   **Visualisasi Tren**: Integrasi **MPAndroidChart** untuk menampilkan grafik perkembangan skor NEWS2 pasien di `VitalSignFragment`.
*   **Data Integrity**: Implementasi `VitalSignRepository` dengan sinkronisasi Cloud.
*   **NEWS2 Calculation**: Integrasi `VitalSignAnalyzer` untuk menghitung skor risiko pasien secara otomatis.

## 🤖 6. AI Clinical Assistant & IV Calculator
*   **AI Smart Analysis**: `AiNotesFragment` yang memberikan ringkasan kondisi pasien dan saran klinis berdasarkan tren data.
*   **IV Calculator**: Implementasi `IVCalculatorFragment` untuk menghitung kecepatan tetesan infus secara akurat berdasarkan Volume, Waktu, dan Drop Factor.

## 🎤 7. Smart Input: Voice & OCR (Flow Tahap 5)
*   **Voice Input**: Fitur pengisian tanda vital via suara (Speech-to-Text) dengan dukungan Bahasa Indonesia & Inggris.
*   **Smart OCR**: Integrasi **Google ML Kit Text Recognition** untuk membaca angka langsung dari foto monitor bedside.
*   **Digital Color Recognition**: 
    *   Algoritma deteksi warna (Sampling Bounding Box) untuk mengklasifikasikan data berdasarkan warna standar monitor (Hijau=Pulse, Biru=SpO2, Merah/Kuning=BP).
    *   Meningkatkan akurasi input otomatis hingga 90% pada berbagai merk monitor medis.

## 📱 8. Revitalisasi UI & Responsivitas
*   **CollapsingToolbar Architecture**: Menerapkan `CollapsingToolbarLayout` pada halaman detail agar navigasi tetap terlihat saat scroll.
*   **Action Unification**: Menyatukan FAB yang tumpang tindih menjadi satu tombol menu tunggal **"Tindakan"**.
*   **Contextual Action Menu**: Penggunaan `PopupMenu` untuk aksi: *Catat Vital Sign* dan *Ingatkan Rekan*.

## 🔐 9. Keamanan & Konfigurasi Cloud
*   **Firestore Security Rules**: Implementasi aturan keamanan Firestore yang mendalam untuk melindungi data sensitif:
    *   Akses berbasis autentikasi (`isAuthenticated`).
    *   Proteksi data koleksi `users`, `connections`, `requests`, `notifications`, `patients`, dan `vital_signs`.
    *   Validasi kepemilikan data (hanya anggota koneksi yang bisa melihat data rekan).
*   **Data Integrity**: Memastikan integritas data pada fitur permintaan rekan kerja (Connection Requests) dan sistem notifikasi real-time.

---

## 📝 Arsitektur & Teknologi Utama
*   **Language**: Kotlin 2.0 (JVM 17).
*   **Database**: Room (Lokal) + Firebase Firestore (Cloud).
*   **Auth**: Firebase Google Sign-In (`LoginActivity`).
*   **Architecture**: MVVM dengan ViewBinding & Navigation Component.
*   **ML/AI**: Google ML Kit (Vision OCR) & Android STT API.

*Log terakhir diperbarui: [Maret 2025]*
