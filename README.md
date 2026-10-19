# Riftbound Counter

Application Android de comptage de points pour les parties de Riftbound. Elle permet de suivre un ou deux joueurs, de gérer un chronomètre de partie et de personnaliser les couleurs des joueurs.

## Fonctionnalités

- Mode un joueur ou deux joueurs, avec le panneau du second joueur retourné pour jouer face à face.
- Scores limités de 0 à 8 ; l’atteinte de 8 points déclenche une animation de victoire.
- Chronomètre configurable, avec démarrage, pause, remise à zéro et affichage du temps supplémentaire.
- Choix de couleur pour chaque joueur ; les couleurs sont conservées entre les lancements de l’application.
- Interface conçue avec Jetpack Compose et Material 3.

## Prérequis

- Android Studio et un JDK 17.
- Android SDK 37 pour compiler ; l’application fonctionne à partir d’Android 7.0 (API 24).

## Compilation et tests

Depuis la racine du projet, sous Windows :

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
```

L’APK de débogage est généré dans `app/build/outputs/apk/debug/`.

## Générer une version release signée

La signature release est facultative et configurée localement via `local.properties`, qui ne doit pas être partagé ni commité. Ajoutez-y les propriétés suivantes avec les chemins et secrets de votre keystore :

```properties
RELEASE_STORE_FILE=chemin/vers/keystore.jks
RELEASE_STORE_PASSWORD=mot-de-passe-du-keystore
RELEASE_KEY_ALIAS=alias-de-cle
RELEASE_KEY_PASSWORD=mot-de-passe-de-la-cle
```

Puis lancez :

```powershell
.\gradlew.bat assembleRelease
```

Si aucun keystore n’est configuré, Gradle génère une release non signée. La sortie se trouve dans `app/build/outputs/apk/release/`.

## Organisation du code

- `game/GameState.kt` : état immuable de la partie, règles de score et formatage du chronomètre.
- `ui/CounterViewModel.kt` : actions de l’interface, état observable et persistance des couleurs.
- `ui/CounterScreen.kt` : écran principal et liaison entre l’interface et le ViewModel.
- `ui/Components.kt` : composants Compose du chronomètre, des panneaux joueurs, des options et des animations.
- `MainActivity.kt` : point d’entrée Android.

## Licence

Voir le fichier [LICENSE](LICENSE).
