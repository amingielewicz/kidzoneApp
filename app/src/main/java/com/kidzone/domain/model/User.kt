package com.kidzone.domain.model

/**
 * Użytkownik aplikacji kidZone.
 *
 * Odpowiada tabeli USERS z dokumentu projektu.
 *
 * Konwencja pól:
 *  - [name] – publiczny "login"/nick widoczny w rankingach, autorach miejsc
 *    i opinii. Ustawiany przy rejestracji jako "Nazwa", edytowalny w profilu.
 *  - [firstName] / [lastName] – dane osobowe, opcjonalne, wpisywane na ekranie
 *    profilu. Nie wymagane do działania reszty aplikacji – istniejący userzy
 *    będą je mieli puste, dopóki sami ich nie uzupełnią.
 *  - [avatarUrl] – pełny URL do avatara (Firebase Storage downloadUrl albo
 *    photoUrl z Google Sign-In). Null = brak avatara, UI pokazuje placeholder
 *    z ikoną osoby.
 */
data class User(
    val id: String,
    val name: String,
    val email: String,
    val firstName: String = "",
    val lastName: String = "",
    val avatarUrl: String? = null,
    val placesAddedCount: Int = 0,
    val reviewsCount: Int = 0,
    val createdAtMillis: Long = 0L
)
