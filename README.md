
# Compilateur (AST → IR → Optimisation → Java)

Ce dépôt contient un compilateur pédagogique pour un langage “mini” (mots-clés en français) qui génère du **Java** compilable.
Le pipeline est volontairement structuré en étapes nettes : **lexing → parsing (AST) → analyse sémantique → (optionnel) IR + optimisations → génération Java**.

---

## 1) Objectif du projet

Le compilateur doit :

1. Lire un programme source (chaîne ou fichier).
2. Construire un **AST** (Abstract Syntax Tree).
3. Exécuter une **analyse sémantique** (types, symboles, validations).
4. (Optionnel) Convertir l’AST vers un **IR** (Intermediate Representation) afin de :

    * faciliter l’inspection (ex : détecter l’usage de `lire()`),
    * simplifier les optimisations (const folding, dead code, etc.),
    * rendre la génération Java plus régulière et testable.
5. Produire du **code Java** compilable.

---

## 2) Organisation du dépôt

### Arborescence (importante)

Ce projet utilise des dossiers de sources **non standards** (configurés via `build-helper-maven-plugin` dans le `pom.xml`) :

* Code principal : `untitled/src/main/`
* Utilitaires : `untitled/src/utils/`
* Tests : `untitled/src/test/java/`

> Si ton IDE ne reconnaît pas les tests / JUnit, c’est souvent parce que ces dossiers ne sont pas marqués correctement comme “Sources Root” / “Test Sources Root”. Voir section **7) Configuration IDE**.

### Modules / packages

* `main.java.lexeur`
  Lexer : transforme le texte source en jetons (`Lexeur`, `Jeton`, `TypeJeton`).

* `main.java.parseur` + `main.java.parseur.ast`
  Parser + AST : construit `Programme`, `Classe`, `Fonction`, `Bloc`, instructions/expressions, etc.

* `main.java.semantic`
  Analyse sémantique : `AnalyseSemantique`, table des symboles, types, erreurs.

* `main.java.ir` + `main.java.ir.convertisseur`
  IR (ex : `IrProgramme`, `IrFonction`, `IrBloc`, `IrAffectation`, etc.) + conversion AST→IR (`AstVersIr`) et génération IR→Java (`IrVersJava`).

* `main.java.optimizer`
  Passes d’optimisation (const folding, dead code elimination, etc.) via `Optimizer`.

* `main.java.codegenerator`
  Génération Java “haut niveau” (`JavaGenerator`) + runtime support (`Scanner` et `lire()`), patch d’imports, patch du code.

* `main.java.cli`
  Point d’entrée en ligne de commande (`CompilerCli`) pour compiler rapidement sans IDE.

---

## 3) Prérequis

* Java **17** (selon `pom.xml`)
* Maven

Vérification :

```bash
java -version
mvn -version
```

---

## 4) Compilation et tests

### Lancer toute la suite de tests

```bash
mvn -q clean test
```

### Exécuter une classe de test précise (JUnit 5 + Surefire)

Exécuter uniquement `IrModelAndInspectorTests` :

```bash
mvn -q -Dtest=IrModelAndInspectorTests test
```

Si Maven a besoin du nom complet :

```bash
mvn -q -Dtest=tests.ir.IrModelAndInspectorTests test
```

### Exécuter une seule méthode de test

```bash
mvn -q -Dtest=IrModelAndInspectorTests#irProgramme_refuse_nulls test
```

---

## 5) Utilisation en ligne de commande (CLI)

Le module `main.java.cli.CompilerCli` sert d’entrée “commande” pour compiler un programme.

Workflow standard :

```bash
mvn -q -DskipTests package
java -cp target/classes main.java.cli.CompilerCli <arguments>
```

### Exemple minimal (si ton CLI accepte un fichier)

*(adapte le chemin selon ton projet)*

```bash
java -cp target/classes main.java.cli.CompilerCli programme.txt
```

### Exemple (si ton CLI accepte un mode “stdin”)

```bash
cat programme.txt | java -cp target/classes main.java.cli.CompilerCli
```

> Si tu veux standardiser l’usage, l’idéal est que `CompilerCli` supporte `--help` et affiche une aide claire en cas d’arguments invalides.

---

## 6) Conventions et pièges importants

### A) Interdiction des packages `java.*`

Java interdit les packages commençant par `java.` (ex : `java.ir`, `java.lexeur`, etc.).
Tout doit rester sous un namespace non réservé (ex : `main.java.*`).

### B) Diagnostics : `DiagnosticCollector` obligatoire

Le projet centralise les erreurs via un collecteur de diagnostics (position, intervalle, messages).
Plusieurs composants attendent explicitement un `DiagnosticCollector` :

* `Lexeur(String, DiagnosticCollector)`
* `AnaSynt.analyser(String, DiagnosticCollector)`
* `AnalyseSemantique(DiagnosticCollector)`

Exemple typique :

```java
DiagnosticCollector diags = new DiagnosticCollector();

Lexeur lex = new Lexeur(source, diags);
var prog = AnaSynt.analyser(source, diags);

AnalyseSemantique sem = new AnalyseSemantique(diags);
sem.verifier(prog);
```

### C) Contrats de null-safety (IR)

Les tests imposent des comportements explicites :

* `IrBloc(null)` → **liste vide** (et jamais `null`)
* `IrConstTexte(null)` → **NullPointerException**
* `IrProgramme(null, …)` et `IrProgramme(…, null)` → **NullPointerException** (si tes tests l’exigent)

Conséquence : sur certains records IR, il ne faut **pas** “normaliser silencieusement” des champs `null` si les tests attendent une exception.

---

## 7) Configuration IDE (IntelliJ / Eclipse) — important

Si l’IDE affiche :

> `java: package org.junit.jupiter.api does not exist`

Alors, l’IDE n’utilise probablement pas Maven correctement **ou** ne reconnaît pas les bons dossiers de tests.

### IntelliJ (recommandé)

1. Ouvre le projet **comme projet Maven** (clic droit `pom.xml` → *Add as Maven Project*).
2. Vérifie que ces dossiers sont bien reconnus :

    * `untitled/src/main` en **Sources Root**
    * `untitled/src/utils` en **Sources Root**
    * `untitled/src/test/java` en **Test Sources Root**
3. Ensuite : *Maven → Reload Project*.

### Pourquoi ?

Ton `pom.xml` ajoute ces répertoires via `build-helper-maven-plugin`.
Maven compile correctement, mais l’IDE peut ne pas les marquer automatiquement.

---

## 8) Exemple de programme (langage source)

```txt
fonction main() {
  x = lire();
  affiche("x=", x);
  retourne x;
}
```

Ce programme :

* déclenche l’injection du runtime `Scanner` + `lire()`,
* génère du Java compilable,
* sert de base aux tests “codegen + runtime”.

---

## 9) Licence

Ce projet est distribué sous licence **GNU Affero General Public License v3.0 (AGPL-3.0-only)**.
Toute modification utilisée pour fournir un service accessible via un réseau doit rendre le code source correspondant disponible aux utilisateurs du service.

---

## 10) Auteur
Mohamed Boudabbous


