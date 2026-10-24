# LibreHU Widgets

Plusieurs widgets d'écran d'accueil dans un seul APK, pour autoradios Jancar / Autochips AC8257 (UJC201), avec une
app de configuration. Thème clair / sombre automatique, comme les autres applis LibreHU.

| Widget | Contenu |
|---|---|
| **Voyants** | rangée de voyants qui s'allument : frein à main (rouge), feux, contact, marche arrière, clignotants, liaison MCU, **ampli externe** (sortie REM) — choix et ordre réglables |
| **Horloge** | heure, date et **temps de trajet** depuis la mise du contact (chronomètre) |
| **Infos** | liste de valeurs : contact, frein à main, ampli externe, temps de trajet, volume, version MCU… — choix et ordre réglables |
| **Voyant unique** | un grand voyant ou une valeur, choisi en posant le widget |

L'app **Widgets LibreHU** montre les valeurs en direct (« sondes »), règle ce que chaque widget affiche, le thème
(comme le lanceur, sombre avec les feux, clair, sombre) et un **mode démo** pour essayer hors de la voiture.
Un service au premier plan (notification discrète) tient les widgets à jour.

## Captures d'écran

| | |
|---|---|
| ![Valeurs en direct et réglage des widgets](docs/screenshots/main.png)<br>Valeurs en direct et réglage des widgets |  |

Captures sur l'émulateur Android 9 (1280×720, 160 dpi comme l'autoradio), sans autoradio branché : les valeurs du véhicule et de la MCU sont vides. Refaites-les avec l'action **Screenshots** de [LibreHU-OTA-app](https://github.com/LibreHU/LibreHU-OTA-app/tree/main/screenshots).

## Branches

| Branche | Source des données |
|---|---|
| `main` | aucune (Android générique) : horloge seulement, voyants « — » ; mode démo |
| `ivi` | Jancar **ivi-services** : `ICar` (frein à main, marche arrière, feux, clignotants, version MCU), contact (`Settings.Global accStatus`), volume (`IAudio`) |
| `librehu-service` | [LibreHU-service](https://github.com/LibreHU/LibreHU-service) : indicateurs véhicule, version MCU, volume |

Le thème suit [LibreHU Launcher](https://github.com/LibreHU/LibreHU-Launcher-App) (y compris son mode
automatique selon les feux et sa **couleur d'accent** : heure, icônes, titres des widgets et app de réglages) ; sans lui, le thème sombre d'Android.

Non testé sur l'autoradio à ce stade.

## Optimisation (R8)

L'APK publié par la CI (`assembleRelease`) est **optimisé par R8** : code et ressources réduits et optimisés, ce qui
rend Jetpack Compose nettement plus fluide ([Compose performance](https://developer.android.com/develop/ui/compose/performance)),
**sans renommage** (`-dontobfuscate` dans `app/proguard-rules.pro`) : les traces des journaux restent lisibles. Les
règles gardent aussi ce qui n'est appelé que par son nom (méthodes natives, helpers root lancés par `app_process`). Si
une fonction plante avec « class / method not found », regarder `usage.txt` dans l'artefact *R8 reports* de la CI (ce
que R8 a retiré) et ajouter une règle `-keep`.

**Style rétro de LibreHU Launcher** : quand le launcher est en style rétro (VFD ambre, LCD vert, cassette), cette appli
prend sa palette (jour ou nuit) et sa police d'afficheur à segments, en majuscules sans accents (« LibreHU Seg14 »,
tirée de DSEG14 © keshikan, SIL Open Font License 1.1, voir `third_party/DSEG-LICENSE.txt`). Le launcher publie le
style et la palette par `content://org.librehu.launcher.theme/theme` (colonnes `style`, `background`, `surface`,
`surface_high`, `on_accent`, `text`, `text_dim`) et par la diffusion `org.librehu.action.THEME_CHANGED` (mêmes noms
en extras).
