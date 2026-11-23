# 🎵 PlanIt — planning de groupe en Java

Application web privée pour organiser un groupe de musique (chorale, groupe de louange, orchestre…) :
chaque membre l'ouvre depuis **son téléphone ou son ordinateur**, dans le navigateur, et se connecte avec son compte.

## ✨ Fonctionnalités

- **Comptes** : chaque membre se connecte avec son e-mail et son mot de passe ; rien n'est visible sans connexion.
  Les administrateurs créent les comptes ou envoient une **invitation** (un code valable 7 jours, à usage unique),
  redonnent un mot de passe oublié et retirent un membre.
- **Droits** : tout le monde ajoute des dates, des chants et des fichiers ; seuls les administrateurs
  les modifient ou les suppriment.
- **Planning partagé** : répétitions, concerts, réunions, avec une vue par semaine.
- **Participation** : chaque membre clique sur « ✋ Je participe ».
- **Programme** : on associe une liste de chants à chaque événement.
- **Répertoire de chants** : paroles et accords, avec recherche.
- **Pièces jointes** : on joint à chaque chant n'importe quel fichier (enregistrement audio,
  vidéo, partition PDF, photo…), jusqu'à 500 Mo. Les sons et vidéos se lisent directement dans la page.
- **Transposition** de n'importe quel chant :
  - monter ou descendre par demi-ton, ou choisir directement la tonalité d'arrivée ;
  - accords **au-dessus des paroles** (l'alignement est conservé) ou **entre crochets** `[Am]comme ça` ;
  - notation **anglaise** (C, D, E…) et **française** (Do, Ré, Mi…) ;
  - dièses ou bémols choisis automatiquement selon la tonalité (ou forcés).

## 🛠️ Technologies

| Partie | Technologie |
|---|---|
| Serveur | Java 21, Spring Boot 4.1 (web, JPA, validation, sécurité) |
| Base de données | PostgreSQL en production ; H2 (fichier `data/planning.mv.db`) en développement et pour les tests. Schéma géré par Flyway |
| Fichiers joints | Dossier `data/fichiers/` |
| Interface | HTML / CSS / JavaScript (sans framework), responsive |
| Tests | JUnit 5, MockMvc, Spring Security Test ; lancés sur H2 et sur PostgreSQL par GitHub Actions |

## 🚀 Lancer l'application

Prérequis : **JDK 21** et **Maven**.

```bash
cd planning-groupe
mvn spring-boot:run
```

Puis ouvrir <http://localhost:8080>.

**Premier lancement** : la page propose de créer le premier compte, qui devient l'administrateur du groupe.
Il ajoute ensuite les autres membres dans l'onglet « Membres », de deux façons :
- directement : il reçoit un mot de passe provisoire à transmettre, à changer à la première connexion ;
- par invitation : il envoie un lien avec un code, et le futur membre crée lui-même son compte.

**Depuis un téléphone** (même réseau Wi-Fi que le PC) : trouver l'adresse IP du PC
(`ipconfig` sous Windows, `ip a` sous Linux) puis ouvrir `http://<IP-du-PC>:8080`.

Lancer les tests :

```bash
mvn test
```

Les lancer sur un vrai PostgreSQL (base vide `planit_test`) :

```bash
PLANIT_TEST_DB_URL=jdbc:postgresql://localhost:5432/planit_test \
PLANIT_TEST_DB_USER=planit PLANIT_TEST_DB_PASSWORD=... mvn test
```

### Base de données

En développement, rien à installer : la base H2 est rangée dans `data/`, en mode compatible PostgreSQL.
Les tables sont créées par les scripts **Flyway** de `src/main/resources/db/migration/`
(`V1__schema_initial.sql`, puis `V2__…`, etc.) ; Hibernate vérifie au démarrage qu'elles
correspondent aux classes Java.

- **Faire évoluer la base** : ajouter un nouveau fichier `V2__description.sql`. Ne jamais modifier
  un fichier déjà appliqué.
- **Ancienne base de développement** : si le dossier `data/` vient d'une version antérieure au
  23 novembre 2026, le supprimer avant de relancer (Flyway refuse une base créée sans lui).

### Production

```bash
mvn package
SPRING_PROFILES_ACTIVE=prod \
PLANIT_DB_URL=jdbc:postgresql://localhost:5432/planit \
PLANIT_DB_USER=planit PLANIT_DB_PASSWORD=... \
PLANIT_CODE_INSTALLATION=... \
java -jar target/planning-groupe-0.1.0.jar
```

Sans l'une de ces variables, l'application refuse de démarrer et dit laquelle manque.

## 🗂️ Organisation du code

```
src/main/java/fr/vices/planning/
├── PlanningGroupeApplication.java   point d'entrée
├── musique/Transposeur.java         ⭐ moteur de transposition (Java pur, sans Spring)
├── modele/                          Membre, Evenement, Chant, PieceJointe… (tables de la base)
├── fichiers/StockageFichiers.java   range les fichiers joints sur le disque
├── depot/                           accès à la base (Spring Data génère le code)
├── securite/                        connexion, mots de passe chiffrés, droits des administrateurs
└── web/                             API REST appelée par l'interface
src/main/resources/static/           interface web (index.html, connexion.html, style.css, app.js…)
src/main/resources/db/migration/     scripts Flyway qui créent et font évoluer la base
```

### API REST

| Méthode | URL | Rôle |
|---|---|---|
| GET | `/api/session` | qui est connecté ? |
| POST | `/api/connexion`, `/api/deconnexion` | se connecter / se déconnecter |
| POST | `/api/inscription` | créer le tout premier compte (administrateur), puis un compte avec un code d'invitation |
| POST | `/api/compte/mot-de-passe`, `/api/compte/suppression` | changer son mot de passe / supprimer son compte |
| GET | `/api/membres` | lister les membres |
| POST | `/api/membres` | créer un compte (administrateur) |
| PUT | `/api/membres/{id}` | modifier une fiche (la sienne, ou toutes pour un administrateur) |
| PUT | `/api/membres/{id}/admin` | donner ou retirer le droit d'administrateur |
| POST | `/api/membres/{id}/mot-de-passe` | redonner un mot de passe provisoire (administrateur) |
| DELETE | `/api/membres/{id}` | supprimer un compte (administrateur) |
| GET / POST | `/api/invitations` | lister / créer une invitation (administrateur) |
| DELETE | `/api/invitations/{id}` | annuler une invitation (administrateur) |
| GET | `/api/evenements?du=AAAA-MM-JJ&au=AAAA-MM-JJ` | événements d'une période |
| POST | `/api/evenements` | créer un événement |
| PUT / DELETE | `/api/evenements/{id}` | modifier / supprimer |
| POST / DELETE | `/api/evenements/{id}/participation` | s'inscrire / se désinscrire (le membre connecté) |
| GET / POST | `/api/chants` | lister / ajouter un chant |
| PUT / DELETE | `/api/chants/{id}` | modifier / supprimer |
| GET / POST | `/api/chants/{id}/pieces-jointes` | lister / joindre un fichier (champ `fichier`) |
| GET / DELETE | `/api/pieces-jointes/{id}` | lire ou télécharger / supprimer un fichier |
| POST | `/api/transposition` | transposer un texte (`demiTons` ou `tonaliteCible`) |

Toutes les adresses demandent d'être connecté, sauf `/api/session`, `/api/connexion` et `/api/inscription`.

### Sécurité

- Les mots de passe sont chiffrés (BCrypt) : le serveur ne connaît jamais le mot de passe en clair.
- Chaque envoi qui modifie quelque chose porte un jeton anti-CSRF.
- L'e-mail d'un membre n'est visible que par lui-même et par les administrateurs.
- Il reste toujours au moins un administrateur dans le groupe.
- Modifier ou supprimer un événement, un chant ou un fichier est réservé aux administrateurs (vérifié par le serveur).

Avant la mise en ligne, il restera à activer : HTTPS, la limitation des tentatives de connexion
et des sessions qui survivent au redémarrage du serveur. Le code d'installation est obligatoire
en production (`PLANIT_CODE_INSTALLATION`).

## 🧑‍💻 À toi de jouer (idées d'exercices)

Classées de la plus simple à la plus difficile :

1. ✅ **Ajouter un champ « tempo » (BPM) aux chants** : `Chant.java`, `ChantControleur.ChantRequete`, puis un `<input>` dans `index.html` et `app.js`. *(fait)*
2. **Ajouter un type d'événement** (ex. `MESSE`, `ENREGISTREMENT`) dans `TypeEvenement` et dans le `<select>` du formulaire.
3. **Suggestion de capo** : dans `Transposeur`, écrire `int capoPour(String tonaliteJouee, String tonaliteVoulue)` + un test dans `TransposeurTest`.
4. **Indisponibilités** : un membre indique « absent du … au … » (nouvelle entité + contrôleur).
5. ✅ **Invitation par code** : la fiche `Invitation`, la règle `estValide` et la recherche `findByCode`. *(fait)*
6. **Mettre en ligne** l'application sur un petit serveur pour y accéder hors du Wi-Fi.

## 👤 Auteur

Fabien — [VICES-tech](https://github.com/VICES-tech)
