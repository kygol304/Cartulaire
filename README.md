# Cartulaire

Application Android — carte GPS enluminée des **églises**, **châteaux médiévaux**, **villages / cités médiévales**, **autels** et **sanctuaires**.

L’interface imite un cartulaire du XIV<sup>e</sup> siècle : parchemin, or, médaillons enluminés, rose des vents de portulan.

## Fonctionnalités

- Carte vectorielle au style parchemin (tuiles [OpenFreeMap](https://openfreemap.org), données [OpenStreetMap](https://www.openstreetmap.org))
- Points d’intérêt chargés autour de la vue via l’[API Overpass](https://overpass-api.de)
- Bouton menu (livre enluminé) pour afficher les filtres : églises, châteaux, villages, autels, sanctuaires
- Base locale SQLite (Wikidata + OSM) : les lieux s’affichent hors-ligne
- Enluminures de cités façon manuscrit (zoom arrière) et cartouche de ville (zoom avant)
- GPS (pèlerin sur la carte) et recentrage par sceau de cire
- Itinéraire pédestre (OSRM) tracé à l’encre d’or
- Fiche de lieu, distance, ouverture dans une appli de navigation, lien Wikipédia
- Enluminures d’angles, médaillons manuscrits, boussole de portulan

Les églises de village apparaissent en se rapprochant. Cathédrales, châteaux, sanctuaires et cités enluminées restent visibles de plus loin.

## Ouvrir le projet

1. Android Studio (SDK 35, JDK 17+)
2. **File → Open** → dossier `Cartulaire`
3. Lancer le module `app` sur un téléphone ou un émulateur

En ligne de commande :

```bat
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
gradlew.bat assembleDebug
```

L’APK se trouve dans `app\build\outputs\apk\debug\`.

## Publier une version (GitHub + Obtainium)

Plus besoin de copier l’APK à la main. Chaque tag `v…` construit l’APK et crée une **Release** GitHub.

## Données

| Couche | Source OSM |
|---|---|
| Églises | `building=church/cathedral/chapel`, `amenity=place_of_worship` + `religion=christian` |
| Châteaux | `historic=castle/fort`, `castle_type=*`, ruines de château |
| Villages médiévaux | `place=village/town` + `historic` ou `heritage`, `historic=citywalls`, `historic:civilization=medieval` |
| Autels | `historic=altar`, `man_made=altar` |
| Sanctuaires | `historic=shrine` / `wayside_shrine`, `building=shrine`, `pilgrimage=yes`, nom « sanctuaire » |

Ce n’est pas un inventaire exhaustif du monde entier en une seule vue : Overpass interroge la **zone visible**. Zoomez sur une région pour la peupler.

## Licence des données

© contributeurs OpenStreetMap, [ODbL](https://opendatacommons.org/licenses/odbl/).
