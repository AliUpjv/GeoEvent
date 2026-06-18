Groupe 6 : 
Chahinez MADI
Kaouthar Sarah TIBA
Ali LEKHAL

# GeoEvent

Application Android permettant de créer, consulter et liker des événements géolocalisés sur une carte. Un utilisateur peut taper n'importe où sur la carte pour publier un événement à cet endroit (titre, description, photo), voir les événements des autres utilisateurs autour de lui, les liker, et les supprimer s'il en est l'auteur (ou administrateur).

## Fonctionnalités

- **Authentification** (email / mot de passe) via Firebase Auth, avec un rôle (`user` ou `admin`) stocké séparément dans Firestore.
- **Carte interactive** (OpenStreetMap via osmdroid) centrée sur la position de l'utilisateur, avec mise à jour en temps réel de sa position.
- **Création d'événement géolocalisé** : un tap sur la carte mémorise l'emplacement choisi (un marker temporaire s'affiche) puis ouvre l'écran de création, pré-rempli avec ces coordonnées. Le bouton "+" fait de même : il utilise en priorité le dernier point tapé sur la carte, sinon la position GPS courante.
- **Synchronisation en temps réel** : les événements sont récupérés via un `addSnapshotListener` Firestore, donc tout changement (nouvel event, like, suppression) se reflète instantanément sur la carte de tous les utilisateurs connectés.
- **Filtrage par distance** : seuls les événements dans un rayon de 50 km autour de l'utilisateur sont affichés (calcul de distance réelle via la formule de Haversine).
- **Markers dont la taille dépend du nombre de likes** : plus un événement est aimé, plus son marker grossit sur la carte.
- **Clustering automatique des markers** : quand on dézoome et que plusieurs événements sont proches les uns des autres, ils sont regroupés en un seul marker affichant leur nombre ; en zoomant, on retrouve les markers individuels (détails plus bas).
- **Suppression d'un événement**, réservée à son créateur ou à un compte admin.
- **Bannière hors-ligne** : un bandeau s'affiche quand la connexion réseau est perdue.

## Comptes de test

Un compte admin est disponible pour tester la suppression d'événements créés par d'autres utilisateurs (la suppression normale, réservée à l'auteur, fonctionne avec n'importe quel compte) :

  Rôle : admin 
  Email : professeur@gmail.com                
  Mot de passe : professeur123  

Pour qu'un compte soit reconnu comme admin, son document dans la collection Firestore `users` doit avoir le champ `role` à `"admin"` (sinon `"user"` par défaut). Le rôle est récupéré à la connexion et transmis jusqu'à l'écran de détail d'un événement, qui affiche alors le bouton "Supprimer" même si ce n'est pas l'auteur de l'événement.

## Stack technique

- **Kotlin**, Android Views classiques (`Activity` + XML + `findViewById`) — pas Jetpack Compose. *Note : le projet contient encore les fichiers `ui/theme/Color.kt`, `Theme.kt`, `Type.kt` et les dépendances Compose dans `build.gradle.kts`, restes du template "Empty Activity" généré par défaut par Android Studio. Ils ne sont utilisés par aucun écran et peuvent être supprimés sans rien casser.*
- **Firebase Auth** pour l'identité, **Firestore** pour les données (événements + rôles utilisateurs), **Firebase Storage** ajouté en dépendance mais non utilisé actuellement (les images sont stockées encodées en base64 directement dans le document Firestore de l'événement, pas dans Storage).
- **osmdroid 6.1.18** pour la carte (OpenStreetMap), choisi pour éviter une clé API et les coûts associés à Google Maps.
- **Coroutines Kotlin** pour l'asynchrone, **ViewModel + StateFlow** pour exposer l'état à l'UI.

## Architecture

Le projet suit une architecture en couches inspirée de la Clean Architecture, avec une règle simple : **les couches internes ne connaissent jamais les couches externes**. Concrètement, `domain` ne dépend de rien d'autre dans le projet ; `data` dépend de `domain` ; `ui` dépend de `domain` (et un peu de `data`, pour instancier les implémentations).

```
com.example.geoevent
├── domain/      → les règles métier et les contrats (interfaces), aucune dépendance Android/Firebase/osmdroid
├── data/        → les implémentations concrètes (Firebase, osmdroid)
├── ui/          → les écrans (Activities) et le ViewModel
└── util/        → utilitaires transverses (ex : détection de connectivité)
```

### `domain` — le cœur métier, indépendant de toute librairie externe

- **`Event`** : modèle de données d'un événement (titre, description, coordonnées, auteur, likes...).
- **`MapMarkerItem`** : représentation d'un point à afficher sur la carte (id, coordonnées, titre, taille), volontairement séparée de `Event` pour que la couche carte n'ait jamais besoin de connaître la classe `Event` en entier.
- **`EventRepository`** (interface) : définit ce qu'on peut faire avec les événements (lire, ajouter, supprimer, liker) sans dire *comment* — c'est `FirestoreEventRepository` dans `data` qui répond à "comment".
- **`AuthRepository`** (interface) : même principe pour l'authentification.
- **`MapProvider`** (interface) : définit toutes les opérations possibles sur une carte (afficher, centrer, poser des markers, écouter les clics...). Grâce à cette interface, remplacer osmdroid par Google Maps demanderait de créer une seule classe `GoogleMapProvider` dans `data`, sans toucher à `MapActivity` ni au reste du `domain`.
- **`LocationUseCase`** : logique de calcul de distance GPS (formule de Haversine) et de filtrage des événements par rayon.
- **`GetEventsUseCase`** : orchestre `EventRepository` (récupération des données) et `LocationUseCase` (filtrage par distance) pour fournir à l'UI une liste d'événements déjà filtrée et triée par popularité.

### `data` — les implémentations concrètes

- **`FirestoreEventRepository`** implémente `EventRepository` avec Firebase Firestore (collection `events`).
- **`FirebaseAuthRepository`** implémente `AuthRepository` avec Firebase Auth + une collection Firestore `users` pour stocker le rôle (`user`/`admin`) de chaque compte.
- **`OsmMapProvider`** implémente `MapProvider` avec osmdroid. C'est ici que vivent tous les détails spécifiques à osmdroid (création de `Marker`, dessin des icônes, gestion du zoom...), pour que le reste de l'app n'ait jamais à importer une seule classe `org.osmdroid.*`.

### `ui` — les écrans

- **`AuthActivity`** : connexion / inscription.
- **`MapActivity`** : écran principal, affiche la carte et orchestre tout (création d'event via tap/bouton +, affichage des markers, déconnexion, profil).
- **`AddEventActivity`** : formulaire de création d'un événement (titre, description, photo) aux coordonnées reçues en paramètre.
- **`EventDetailActivity`** : détail d'un événement, avec bouton "Aimer" et bouton "Supprimer" (affiché seulement si l'utilisateur est l'auteur ou un admin).
- **`EventViewModel`** : seul point de contact entre l'UI et le `domain`. Expose la liste d'événements (`StateFlow<List<Event>>`) et le rôle de l'utilisateur courant, sans jamais manipuler Firebase ou osmdroid directement.

### Pourquoi cette séparation ?

Trois bénéfices concrets dans ce projet :
1. **Remplacer une brique sans tout casser** : passer d'osmdroid à Google Maps, ou de Firestore à une autre base, ne touche qu'un seul fichier dans `data`.
2. **Tester la logique métier sans Android** : `LocationUseCase` ou `GetEventsUseCase` peuvent être testés avec du JUnit pur, sans émulateur, puisqu'ils ne dépendent d'aucune classe Android.
3. **Lisibilité** : chaque fichier a une responsabilité unique et clairement nommée (un repository ne dessine pas de carte, un use case ne parle pas à Firebase directement).

## Le clustering des markers

Quand on dézoome la carte et que plusieurs événements sont géographiquement proches, les afficher tous individuellement deviendrait illisible (markers superposés). `OsmMapProvider.setEventMarkers()` résout ça avec un algorithme de clustering "glouton" :

1. À chaque rafraîchissement des données **et** à chaque changement de niveau de zoom (écouté via un `MapListener` osmdroid), on recalcule les groupes.
2. Le rayon de regroupement est exprimé en pixels écran (70px par défaut) puis converti en kilomètres réels selon le niveau de zoom et la latitude courante, grâce à la formule standard de projection Web Mercator (`metersPerPixel`). Plus on dézoome, plus un pixel représente une grande distance réelle, donc plus le rayon de regroupement augmente automatiquement — pas besoin de recalibrer à la main pour chaque niveau de zoom.
3. On prend un événement, on lui rattache tous les autres événements restants situés à moins de ce rayon (distance réelle calculée avec `LocationUseCase.distanceKm`, la même formule de Haversine déjà utilisée pour le filtrage par rayon), on les retire de la liste, et on recommence jusqu'à épuisement. C'est le même principe que la classe `RadiusMarkerClusterer` de la librairie `osmbonuspack`, réimplémenté ici directement pour éviter une dépendance externe supplémentaire.
4. Un groupe d'un seul événement devient un marker individuel classique (taille selon les likes). Un groupe de plusieurs événements devient un marker de cluster (cercle bleu) affichant leur nombre, centré sur la position moyenne du groupe. Taper sur un cluster zoome automatiquement sur sa zone, ce qui fait apparaître les markers individuels.

### Taille des markers individuels selon les likes

Calculée dans `OsmMapProvider.createSizedMarkerBitmap(size)` : le diamètre part d'une base de 28dp et augmente de 5dp par like, plafonné à 90dp (soit un maximum atteint autour de 12 likes). `size` correspond à `event.likes + 1` (calculé dans `MapActivity`, jamais directement le nombre de likes, pour qu'un événement à 0 like ait déjà un marker visible). Un marker de cluster, lui, garde toujours la même taille fixe (50dp) quel que soit le nombre de likes des événements qu'il regroupe — seul son chiffre central change.

## Limitations connues

- Les images sont stockées en base64 directement dans Firestore plutôt que dans Firebase Storage (dépendance présente mais inutilisée), ce qui limite leur taille et alourdit les documents.
- Aucun test unitaire n'est encore écrit pour `LocationUseCase` ou `GetEventsUseCase`, bien que leur indépendance d'Android le permette facilement.
