"""Shahobcha mavjudligini haydovchilar tasdiqlashi mantig'i (sof funksiyalar)."""


def is_verified(source: str, exists: int, missing: int, threshold: int) -> bool:
    """Admin qo'shgan shahobcha darhol ishonchli. Community shahobcha esa kamida
    `threshold` ta turli haydovchi "bor" degach va "yo'q" deganlardan ko'p bo'lsa tasdiqlanadi."""
    if source == "admin":
        return True
    return exists >= threshold and exists > missing


def should_hide(source: str, exists: int, missing: int, threshold: int) -> bool:
    """Ko'pchilik "bu yerda shahobcha yo'q" desa, community shahobcha yashiriladi.
    Admin qo'shgan shahobchalar ovoz bilan yashirilmaydi."""
    return source == "community" and missing >= threshold and missing > exists
