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

## Branches

Cette branche : **`ivi`** (Jancar ivi-services).


| Branche | Source des données |
|---|---|
| `main` | aucune (Android générique) : horloge seulement, voyants « — » ; mode démo |
| `ivi` | Jancar **ivi-services** : `ICar` (frein à main, marche arrière, feux, clignotants, version MCU), contact (`Settings.Global accStatus`), volume (`IAudio`) |
| `librehu-service` | [LibreHU-service](https://github.com/LibreHU/LibreHU-service) : indicateurs véhicule, version MCU, volume |

Le thème suit [LibreHU Launcher](https://github.com/LibreHU/LibreHU-Launcher-App) (y compris son mode
automatique selon les feux et sa **couleur d'accent** : heure, icônes, titres des widgets et app de réglages) ; sans lui, le thème sombre d'Android.

Non testé sur l'autoradio à ce stade.
