# Open Gaz Android

Kotlin + Jetpack Compose. Xarita: OpenStreetMap (osmdroid), API kalit va to'lov kartasi kerak emas.
Joylashuv: Google Play Services (kalit kerak emas).

## Ishga tushirish
1. Backendni ishga tushiring (`backend/README.md`): `uvicorn app.main:app --reload`.
2. Android Studio (stable) da `android/` papkasini oching, Gradle sync, Run.
3. Emulyatorda backend manzili `http://10.0.2.2:8000/` (standart). Haqiqiy telefon yoki serverda:
   `./gradlew assembleDebug -PAPI_BASE_URL=https://sizning-server.uz/`
   (`http://` faqat debug build'da ruxsat etilgan, release faqat `https://`).

Gradle wrapper (`gradlew`) repoda yo'q: Android Studio birinchi ochilganda yaratadi (Gradle 8.7+, JDK 17).

## Ekranlar
- **Ro'yxat va xarita**: ochiq shahobchalar tepada, yoqilg'i/viloyat/"faqat ochiq" filtrlari, 60 soniyada yangilanadi.
  Xaritada marker rangi holatni bildiradi (yashil/qizil/kulrang); tasdiqlanmagan shahobchaning chegarasi to'q sariq.
- **Shahobcha tafsiloti**: "necha haydovchi tasdiqladi", har yoqilg'i uchun holat va o'rtacha navbat (nechta xabar asosida),
  holat va navbat yuborish, "Bu yerda bor / yo'q" tasdiqlash, izohlar.
- **Shahobcha qo'shish**: xaritada pin qo'yiladi, qo'shgan haydovchi shahobchadan 500 m ichida bo'lishi shart.
- **Kirish / ro'yxatdan o'tish**: ko'rish uchun kirish shart emas, yuborish uchun kerak.

## Holati
Kod yozilgan, lekin hali Android Studio'da yig'ib ko'rilmagan (kompilyatsiya xatolari chiqishi mumkin).
`app/src/test` da `Parser` uchun JVM testlari bor (`./gradlew test`).

## Keyingi qadamlar
- Token xavfsiz saqlash (EncryptedSharedPreferences), qurilmada tarmoq xatolarini yaxshiroq ko'rsatish.
- Yo'nalish (navigatsiya) tugmasi, ilova ikonkasi, qorong'u rejim.
- Push xabarlar, o'z hissa tarixi (profil ekrani).
