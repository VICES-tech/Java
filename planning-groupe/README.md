# 🎵 Planning de groupe — Java

Application web pour organiser un groupe de musique (chorale, groupe de louange, orchestre…) :
chaque membre l'ouvre depuis **son téléphone ou son ordinateur**, dans le navigateur, sans rien installer.

## ✨ Fonctionnalités

- **Planning partagé** : répétitions, concerts, réunions, avec une vue par semaine.
- **Participation** : chaque membre clique sur « ✋ Je participe ».
- **Programme** : on associe une liste de chants à chaque événement.
- **Répertoire de chants** : paroles et accords, avec recherche.
- **Pièces jointes** : on joint à chaque chant n'importe quel fichier (enregistrement audio,
  vidéo, partition PDF, photo…). Les sons et vidéos se lisent directement dans la page.
- **Transposition** de n'importe quel chant :
  - monter ou descendre par demi-ton, ou choisir directement la tonalité d'arrivée ;
  - accords **au-dessus des paroles** (l'alignement est conservé) ou **entre crochets** `[Am]comme ça` ;
  - notation **anglaise** (C, D, E…) et **française** (Do, Ré, Mi…) ;
  - dièses ou bémols choisis automatiquement selon la tonalité (ou forcés).

## 🛠️ Technologies

| Partie | Technologie |
|---|---|
| Serveur | Java 21, Spring Boot 3 (web, JPA, validation) |
| Base de données | H2 (fichier `data/planning.mv.db`) ; fichiers joints dans `data/fichiers/` |
| Interface | HTML / CSS / JavaScript (sans framework), responsive |
| Tests | JUnit 5, MockMvc |

## 🚀 Lancer l'application

Prérequis : **JDK 21** et **Maven**.

```bash
cd planning-groupe
mvn spring-boot:run
```

Puis ouvrir <http://localhost:8080>.

**Depuis un téléphone** (même réseau Wi-Fi que le PC) : trouver l'adresse IP du PC
(`ipconfig` sous Windows, `ip a` sous Linux) puis ouvrir `http://<IP-du-PC>:8080`.

Lancer les tests :

```bash
mvn test
```

## 🗂️ Organisation du code

```
src/main/java/fr/vices/planning/
├── PlanningGroupeApplication.java   point d'entrée
├── musique/Transposeur.java         ⭐ moteur de transposition (Java pur, sans Spring)
├── modele/                          Membre, Evenement, Chant, PieceJointe… (tables de la base)
├── fichiers/StockageFichiers.java   range les fichiers joints sur le disque
├── depot/                           accès à la base (Spring Data génère le code)
└── web/                             API REST appelée par l'interface
src/main/resources/static/           interface web (index.html, style.css, app.js)
```

### API REST

| Méthode | URL | Rôle |
|---|---|---|
| GET / POST | `/api/membres` | lister / ajouter un membre |
| PUT / DELETE | `/api/membres/{id}` | modifier / supprimer |
| GET | `/api/evenements?du=AAAA-MM-JJ&au=AAAA-MM-JJ` | événements d'une période |
| POST | `/api/evenements` | créer un événement |
| PUT / DELETE | `/api/evenements/{id}` | modifier / supprimer |
| POST / DELETE | `/api/evenements/{id}/participants/{membreId}` | s'inscrire / se désinscrire |
| GET / POST | `/api/chants` | lister / ajouter un chant |
| PUT / DELETE | `/api/chants/{id}` | modifier / supprimer |
| GET / POST | `/api/chants/{id}/pieces-jointes` | lister / joindre un fichier (champ `fichier`) |
| GET / DELETE | `/api/pieces-jointes/{id}` | lire ou télécharger / supprimer un fichier |
| POST | `/api/transposition` | transposer un texte (`demiTons` ou `tonaliteCible`) |

## 🧑‍💻 À toi de jouer (idées d'exercices)

Classées de la plus simple à la plus difficile :

1. **Ajouter un champ « tempo » (BPM) aux chants** : `Chant.java`, `ChantControleur.ChantRequete`, puis un `<input>` dans `index.html` et `app.js`.
2. **Ajouter un type d'événement** (ex. `MESSE`, `ENREGISTREMENT`) dans `TypeEvenement` et dans le `<select>` du formulaire.
3. **Suggestion de capo** : dans `Transposeur`, écrire `int capoPour(String tonaliteJouee, String tonaliteVoulue)` + un test dans `TransposeurTest`.
4. **Indisponibilités** : un membre indique « absent du … au … » (nouvelle entité + contrôleur).
5. **Comptes et mots de passe** avec Spring Security, pour que seuls les membres accèdent au planning.
6. **Mettre en ligne** l'application (Render, Railway, un VPS…) pour y accéder hors du Wi-Fi.

## 👤 Auteur

Fabien — [VICES-tech](https://github.com/VICES-tech)
