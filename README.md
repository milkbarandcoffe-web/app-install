# app-install

APK "NOME install": un APK per ogni app Google Apps Script.

- Primo avvio: apre l'INSTALLER dell'app (login, permessi e installazione dentro l'APK).
- Dopo l'installazione l'APK apre direttamente l'app.
- Ogni app ha un file `apps/ID.json` con `nome`, `colore` e `pacchetto` (id del pacchetto creato dal comando `pacchetto` di CLAUDE-EXE).
- GitHub compila e pubblica ogni APK nella release con tag `ID`: il link di download resta sempre lo stesso.
- Firma con chiave fissa salvata nei Secrets del repo (KEYSTORE_B64, KEYSTORE_PASS).

idea e sviluppo by Daniel Zavan
