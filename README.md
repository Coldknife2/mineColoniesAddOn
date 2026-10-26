# MineColonies Minergy (add-on)

Add-on **NeoForge 1.21.1** (compatible **All the Mods 10**) pour [MineColonies](https://github.com/ldtteam/minecolonies) qui ajoute la **Minergie** (*Minergy*) : une énergie propre à chaque colonie, produite à la **Bibliothèque** (*Library*) (aucune recherche requise), que les bâtisseurs dépensent pour poser les blocs qui leur manquent.

## Fonctionnement

- **1 Minergie = 1 bloc** (configurable).
- **Mystical Mage** : nouveau métier de la Bibliothèque, avec le même skin que le Researcher. Il s'assoit près d'une étagère, reste immobile et génère de la Minergie à chaque seconde de travail :
  - 4/s au niveau 1 avec un mage débutant ;
  - jusqu'à 20/s au niveau 5 avec Mana 99 ;
  - formule : `(4 + 3 × (niveau − 1)) × (1 + 0,25 × Mana/99)`, tous les paramètres sont configurables.
- **Hôtel de ville** : dès qu'il est posé, il génère 1 Minergie par seconde (configurable), pour démarrer la colonie avant d'avoir une Bibliothèque.
- **Liée à la colonie** : la Minergie est stockée par colonie (dimension + id) et ne peut servir à aucune autre. La capacité vaut 10 000 + 100 000 × le niveau de la meilleure Bibliothèque (configurable, 0 = illimitée).
- **Construction** : à chaque pose de bloc, le bâtisseur choisit entre les vrais blocs et la Minergie selon le mode :
  - mode *Secours* (défaut) : on pose le vrai bloc s'il est dans l'inventaire du bâtisseur. Sinon, s'il reste au moins 1 Minergie, elle est consommée et le bloc est posé sans attendre de livraison. Les commandes de lots au warehouse continuent normalement.
  - mode *Minergie d'abord* : la Minergie est utilisée en priorité. Le bâtisseur ne réclame pas aux livreurs les lots de ressources que la Minergie peut payer.
- **Boost de vitesse** : 120 Minergie achètent +50 % de vitesse de travail et de marche pour un citoyen pendant 60 s ; 240 donnent +100 %, 360 +150 %, etc. (jusqu'à 10 paliers, tout est configurable). Les boosts se cumulent. Un citoyen boosté place ses blocs, mine et fabrique plus vite (tous les délais de son IA sont divisés par 1 + bonus) et se déplace plus vite.
  - depuis le **menu du citoyen** (clic droit) : onglet *Boost Minergie*, sous les autres onglets ;
  - depuis l'onglet *Boost* de la Bibliothèque : liste des citoyens, bâtisseurs en tête ;
  - **automatiquement par le mage** : au-delà d'un seuil de Minergie réglable dans la Bibliothèque (5 000 par défaut, 0 = jamais), le Mage mystique arrête de méditer et parcourt la colonie pour booster les travailleurs (bâtisseurs d'abord), avec un nombre de paliers réglable.
- **Exclusions** : les entités (cadres, armor stands), les fluides (seaux) et le feu exigent toujours leurs vrais items, sinon on pourrait les récupérer.
- **Blocs Minergie** : ils ne droppent rien quand on les casse, que ce soit par un joueur, une explosion, un piston, ou un bâtisseur qui déconstruit ou fait une amélioration. La marque est stockée dans le chunk (data attachment NeoForge) et suit le bloc quand un piston le pousse ou le tire.
- **Réglages** : la Bibliothèque gagne quatre onglets :
  - *Minergie* : réserve, débit, état ;
  - *Boost* : booster un citoyen ;
  - *Travailleur* : recrutement du mage ;
  - *Réglages* : Minergie pour la construction et son mode, seuil et niveau du boost automatique du mage.

  C'est la Bibliothèque de plus haut niveau qui porte les réglages de la colonie.
- **Build mode (Structurize)** : la Minergie de la colonie s'affiche avec son icône en haut à gauche, quand le joueur tient le Build Tool ou que la fenêtre de placement est ouverte.

## Intégration technique

- Le métier est enregistré dans le registre `minecolonies:jobs`.
- Les modules sont greffés sur l'entrée `minecolonies:library` existante, sans remplacer le bâtiment.
- Mixins (`remap = false`) :
  - `BuildingStructureHandler` : `hasRequiredItems`, `triggerSuccess`, `consume` ;
  - `BuildingResourcesModule.checkOrRequestBucket` : mode *Minergie d'abord* ;
  - `BlockPosUtil.getBlockDrops` : drops calculés par les citoyens ;
  - `AbstractEntityAIBasic.setDelay` : délais de travail raccourcis pour un citoyen boosté ;
  - `AbstractWindowCitizen` (client) : onglet *Boost Minergie* du menu citoyen.
- Un Global Loot Modifier `minergy:no_drop`.

## Télécharger le mod

Le mod existe en deux versions, une par branche :

| Branche | Minecraft | Loader | Jar publié | Exemple de modpack |
|---|---|---|---|---|
| `main` | 1.21.1 | NeoForge | `minergy-neoforge-1.21.1-<version>.jar` | All the Mods 10 |
| `1.20.1` | 1.20.1 | Forge | `minergy-forge-1.20.1-<version>.jar` | packs Forge 1.20.1 |

Chaque merge sur l'une de ces branches déclenche un build GitHub Actions qui publie une **Release** nommée d'après le loader et la version de Minecraft (par exemple *Minergy 0.2.0 · NeoForge 1.21.1*, tag `neoforge-1.21.1-v0.2.0-build5`). Le badge *Latest* reste sur la version NeoForge 1.21.1. Il suffit de déposer le jar dans le dossier `mods/` d'une instance qui contient déjà MineColonies et ses dépendances (Structurize, BlockUI, Domum Ornamentum, Multi-Piston).

Les nouvelles fonctionnalités sont développées sur `main` puis reportées sur `1.20.1` (seule la couche propre au loader diffère).

Les pull requests sont aussi buildées : le jar est alors disponible dans l'onglet *Actions* (artefact du run).

## Développement

Il faut un JDK 21.

```
./gradlew build        # jar dans build/libs
./gradlew runClient    # client de dev avec MineColonies
```

Versions : voir `gradle.properties`. L'add-on est compilé contre MineColonies `1.1.1403-1.21.1` (Structurize `1.0.832-1.21.1-snapshot`, NeoForge `21.1.215`) et accepte MineColonies `1.1.1300-1.21.1` et plus.

## Pistes pour la suite (autres systèmes branchables sur la Minergie)

Outils manquants, livraisons du warehouse, ingrédients manquants des crafters, carburant des fours, mineur (échelles, comblement, soutènement).
