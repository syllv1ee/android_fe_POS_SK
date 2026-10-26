package com.mhyddnn.mubarokappmobile.core.common

/**
 * Hasil generik untuk operasi async (network/DB).
 *
 * Dipakai sebagai tipe kembalian repository agar ViewModel membedakan sukses / gagal tanpa
 * try-catch: error yang bisa diduga (HTTP, jaringan) menjadi [Error], bukan exception.
 * Bug pemrograman & pembatalan coroutine tetap dilempar sebagai exception.
 *
 * Repository tidak pernah mengembalikan [Loading]; nilai itu hanya untuk state UI, karena itu
 * `when` di ViewModel tetap menulis cabang `Result.Loading -> Unit`.
 */
sealed interface Result<out T> {
    data object Loading : Result<Nothing>

    data class Success<out T>(val data: T) : Result<T>

    /**
     * @param message pesan ringkas untuk ditampilkan ke user.
     * @param code HTTP status (401/403/400/…); null bila bukan error HTTP.
     * @param fieldErrors map `field -> pesan` dari validasi backend (400). Dipakai untuk
     *        menandai field bermasalah di form.
     * @param cause exception asal (untuk logging).
     */
    data class Error(
        val message: String,
        val code: Int? = null,
        val fieldErrors: Map<String, String>? = null,
        val cause: Throwable? = null,
    ) : Result<Nothing>
}

/** Ubah isi [Result.Success]; [Result.Error] & [Result.Loading] diteruskan apa adanya. */
inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> = when (this) {
    is Result.Loading -> Result.Loading
    is Result.Error -> this
    is Result.Success -> Result.Success(transform(data))
}

/** Isi bila sukses, selain itu null (pesan error hilang — pakai `when` bila perlu ditampilkan). */
val <T> Result<T>.dataOrNull: T?
    get() = (this as? Result.Success)?.data
