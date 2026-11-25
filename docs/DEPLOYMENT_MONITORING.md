# Plan de Tests, Déploiement et Monitoring — STRATA (Livrable 9 — Sections 14 & 17)

## 1. Stratégie de Tests Automatisés
- **Moteur audio (`MultiStemAudioEngine`)** : vérification du mixage synchrone 4 stems sur horloge d'échantillon unique, transition mix → stems calée sur `beatInBar == 1`, étirement temporel 50–150 % sans décalage de hauteur, et boucle A-B calée sur la mesure.
- **Alignement Généalogie (`GenealogyAlignerTest`)** : DTW sur grille de temps, recherche sur les 12 transpositions chromatiques pour `cover_of`, DTW sous-séquence pour `sample_of`, objectif MVP 90 % des points à ± 50 ms.
- **Grand Livre & Forks (`LedgerEngineTest`)** : invariance double entrée `SUM(debit) == SUM(credit)` en centimes de XOF (`bigint`), cascade limitée à 3 niveaux, contre-passation immuable, détection des forks vides.
- **Synchronisation Orchestre (`ClockSyncProtocol`)** : filtrage RTT NTP (< 2 ms), détection de pic chirp par intercorrélation (< 3 ms), micro-rééchantillonnage de dérive (± 0,1 %).
- **Parcours UI Compose (`ExampleRobolectricTest`)** : recherche par BPM/titre/tonalité, bascule des crans 0 à 4, création de session Orchestre et consultation du Grand Livre Artiste.

## 2. Déploiement Progressif (Côte d'Ivoire)
1. **Phase 1 — Prototype local** : validation hors-ligne sur appareils Android d'entrée de gamme (CPU audio < 25 %, APK < 40 Mo).
2. **Phase 2 — Bêta fermée (500 testeurs)** : écoles de musique, chorales, beatmakers et DJs à Abidjan (`crash-free >= 99 %`).
3. **Phase 3 — Lancement public Android** : Play Store + APK léger, suivi rétention J1/J7/J30 (cible J30 >= 20 %).

## 3. Indicateurs de Pilotage (Monitoring)
- **Technique** : démarrage de lecture `p50/p95` (< 1,5 s en 4G / < 3 s en 3G Éco), taux de coupure 4G → 3G, écart de synchro Orchestre (< 10 ms), précision d'alignement Généalogie.
- **Produit & Revenus** : répartition du temps par cran (0–4), % d'écoutes avec navigation Généalogie, conversion gratuit → Pass/Premium/Pro, montants reversés aux ayants droit (XOF).
