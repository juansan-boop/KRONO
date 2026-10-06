package com.krono.core.data.auth.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.krono.core.data.auth.HashedPassword
import com.krono.core.domain.auth.User

/** Cuenta local (D-1). Solo guarda el hash PBKDF2 y su sal, nunca la contraseña. */
@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)],
)
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    @ColumnInfo(name = "password_hash") val passwordHash: String,
    @ColumnInfo(name = "password_salt") val passwordSalt: String,
    @ColumnInfo(name = "password_iterations") val passwordIterations: Int,
) {
    fun toDomain(): User = User(id = id, email = email)

    fun toHashedPassword(): HashedPassword =
        HashedPassword(hash = passwordHash, salt = passwordSalt, iterations = passwordIterations)
}

/**
 * Sesión activa: como máximo una fila (id fijo). Persistir aquí permite reabrir
 * la app sin pasar por el login (F-22 a F-25 CA-6).
 */
@Entity(
    tableName = "session",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["user_id"])],
)
data class SessionEntity(
    @ColumnInfo(name = "user_id") val userId: String,
    @PrimaryKey val id: Int = SINGLE_SESSION_ID,
) {
    companion object {
        const val SINGLE_SESSION_ID = 0
    }
}
