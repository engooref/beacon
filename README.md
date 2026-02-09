# Quiz - Tests unitaires

Ce depot contient des tests unitaires JVM pour la logique Kotlin (sans emulateur).

## Lancer les tests (Windows PowerShell)

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

## Notes

- Les tests sont dans `app/src/test/java`.
- Ils couvrent les utilitaires CSV et la synchronisation `WaitNotify`.

