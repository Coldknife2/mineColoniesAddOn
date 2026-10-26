# MineColonies Minergy (add-on)

Add-on **Forge 1.20.1** (branche `1.20.1` ; la version NeoForge 1.21.1 pour All the Mods 10 est sur `main`) pour [MineColonies](https://github.com/ldtteam/minecolonies) qui ajoute la **Minergie** (*Minergy*) : une énergie propre à chaque colonie, produite au **Mystical Site**, que les bâtisseurs dépensent pour poser les blocs qui leur manquent.

## Fonctionnement

- **1 Minergie = 1 bloc** (configurable).
- **Mystical Mage** : nouveau métier du Mystical Site, avec le même skin que le Researcher. Il médite dans le bâtiment et génère de la Minergie à chaque seconde de travail :
  - 4/s au niveau 1 avec un mage débutant ;
  - jusqu'à 20/s au niveau 5 avec Mana 99 ;
  - formule : `(4 + 3 × (niveau − 1)) × (1 + 0,25 × Mana/99)`, tous les paramètres sont configurables.
- **Liée à la colonie** : la Minergie est stockée par colonie (dimension + id) et ne peut servir à aucune autre. La capacité vaut 100 000 × le niveau du meilleur Mystical Site (configurable, 0 = illimitée).
- **Construction** : à chaque pose de bloc, le bâtisseur choisit entre les vrais blocs et la Minergie selon le mode :
  - mode *Secours* (défaut) : on pose le vrai bloc s'il est dans l'inventaire. Sinon, s'il reste au moins 1 Minergie, elle est consommée et le bloc est posé. Les requêtes de vrais blocs continuent normalement.
  - mode *Minergie d'abord* : la Minergie est utilisée en priorité. Le bâtisseur ne réclame pas aux livreurs les lots de ressources que la Minergie peut payer.
- **Exclusions** : les entités (cadres, armor stands), les fluides (seaux) et le feu exigent toujours leurs vrais items, sinon on pourrait les récupérer.
- **Blocs Minergie** : ils ne droppent rien quand on les casse, que ce soit par un joueur, une explosion, un piston, ou un bâtisseur qui déconstruit ou fait une amélioration. La marque est stockée dans le chunk et suit le bloc quand un piston le pousse ou le tire.
- **Réglages** : le Mystical Site gagne trois onglets :
  - *Minergie* : réserve, débit, état ;
  - *Travailleur* : recrutement du mage ;
  - *Réglages* : activer ou désactiver la Minergie pour la construction, et choisir le mode.

  C'est le Mystical Site de plus haut niveau qui porte les réglages de la colonie.
- **Build mode (Structurize)** : la Minergie de la colonie s'affiche avec son icône en haut à gauche, quand le joueur tient le Build Tool ou que la fenêtre de placement est ouverte.

## Intégration technique

- Le métier est enregistré dans le registre `minecolonies:jobs`.
- Les modules sont greffés sur l'entrée `minecolonies:mysticalsite` existante, sans remplacer le bâtiment.
- Trois Mixins (`remap = false`) :
  - `BuildingStructureHandler` : `hasRequiredItems`, `triggerSuccess`, `consume` ;
  - `BuildingResourcesModule.checkOrRequestBucket` : mode *Minergie d'abord* ;
  - `BlockPosUtil.getBlockDrops` : drops calculés par les citoyens.
- Un Global Loot Modifier `minergy:no_drop`.

## Télécharger le mod

Le mod existe en deux versions, une par branche :

| Branche | Minecraft | Loader | Jar publié | Exemple de modpack |
|---|---|---|---|---|
| `main` | 1.21.1 | NeoForge | `minergy-neoforge-1.21.1-<version>.jar` | All the Mods 10 |
| `1.20.1` | 1.20.1 | Forge | `minergy-forge-1.20.1-<version>.jar` | packs Forge 1.20.1 |

Chaque merge sur l'une de ces branches déclenche un build GitHub Actions qui publie une **Release** nommée d'après le loader et la version de Minecraft (par exemple *Minergy 0.2.0 · Forge 1.20.1*, tag `forge-1.20.1-v0.2.0-build6`). Le badge *Latest* reste sur la version NeoForge 1.21.1. Il suffit de déposer le jar dans le dossier `mods/` d'une instance qui contient déjà MineColonies et ses dépendances (Structurize, BlockUI, Domum Ornamentum, Multi-Piston).

Les nouvelles fonctionnalités sont développées sur `main` puis reportées sur `1.20.1` (seule la couche propre au loader diffère).

Les pull requests sont aussi buildées : le jar est alors disponible dans l'onglet *Actions* (artefact du run).

## Développement

Il faut un JDK 17.

```
./gradlew build        # jar dans build/libs
./gradlew runClient    # client de dev avec MineColonies
```

Versions : voir `gradle.properties`. L'add-on est aligné sur MineColonies `1.20.1-1.1.1303-snapshot` et Structurize `1.20.1-1.0.818`.

## Pistes pour la suite (autres systèmes branchables sur la Minergie)

Outils manquants, livraisons du warehouse, ingrédients manquants des crafters, carburant des fours, mineur (échelles, comblement, soutènement).
