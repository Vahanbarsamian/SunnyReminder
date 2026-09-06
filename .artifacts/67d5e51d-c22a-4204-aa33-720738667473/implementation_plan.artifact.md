# Plan d'Action - Bouton de Sortie dans le Panneau Info & Version 2.7 🏖️🚪📊

Ce plan déplace le mécanisme de fermeture de l'alerte vers le panneau d'informations pour une meilleure ergonomie par tous les temps, et met à jour l'identité du logiciel.

## Modifications Proposées

### 1. Interface de Sortie (BeachScene)

#### [MODIFY] [BeachScene.kt](file:///C:/Users/vahan/AndroidStudioProjects/Sunnyreminder/app/src/main/java/com/vahan/sunnyreminder/ui/BeachScene.kt)
- **Bouton de Sortie dans Info** :
    - Ajouter une icône **"X"** (croix de fermeture) ou un bouton **"Quitter"** stylisé à l'intérieur du panneau d'informations (celui qui s'ouvre avec le bouton **(i)**).
    - Modifier la détection du clic (`onTap`) pour que, si le panneau d'infos est ouvert, un appui sur cette croix déclenche la sortie (`onSunClick`).
- **Retrait du Clic Soleil** : Comme demandé, le bouton principal de sortie devient celui du panneau Info, évitant les problèmes de visibilité du soleil selon la météo.

### 2. Guide & Aide (MainActivity)

#### [MODIFY] [MainActivity.kt](file:///C:/Users/vahan/AndroidStudioProjects/Sunnyreminder/app/src/main/java/com/vahan/sunnyreminder/MainActivity.kt)
- **Dialogue d'Aide** : Mettre à jour l'explication pour le "Soleil" et l' "Info". Préciser que la sortie se fait désormais via le panneau d'informations.
- **Version** : L'affichage de la version se mettra à jour automatiquement via le Gradle.

### 3. Métadonnées & Documentation

#### [MODIFY] [build.gradle.kts](file:///C:/Users/vahan/AndroidStudioProjects/Sunnyreminder/app/build.gradle.kts)
- Passer la version à **2.7**.

#### [MODIFY] [README.md](file:///C:/Users/vahan/AndroidStudioProjects/Sunnyreminder/README.md)
- Mettre à jour le numéro de version et la description du fonctionnement du bouton de sortie.

## Vérification Plan

### Tests Manuels
1. **Sortie de l'Alerte** :
    - Lancer un test (5s).
    - Cliquer sur **(i)** en haut à gauche.
    - Cliquer sur la nouvelle croix **(X)** dans le panneau.
    - Vérifier que l'alerte se ferme bien.
2. **Vérification Version** :
    - Aller dans les réglages (Engrenage).
    - Vérifier que la version affichée est bien **2.7**.
3. **Aide** :
    - Ouvrir le menu d'aide (?) et vérifier que les nouvelles instructions sont claires.
