package main.java.lexeur;

// ici j'énumère les types de tokens
public enum TypeJeton {

    Si, Sinon, TantQue,//deja implemente
    Retourne, Pour, Fonction, Classe, // ici je mets mes mots-clés
    //deja implemente
    Entier, Flottant, Booleen, Caractere, Texte, Vide, // ici je mets mes types de données

    Plus, Moins, Mult, Div, Modulo, Incr, Decr, // ici je mets mes opérateurs arithmétiques

    Affecte, Egal, PasEgal, Inf, Superieur, InfEgal, SupEgal, // ici mes opérateurs de comparaison
    Et, Ou, Non,//icci je mets mes opérateurs logique

    ParOuvr, ParFerm, AccoladeOuvr, AccoFerma,CrochetOuvrant, CrochetFermant, // ici mes parenthèses et accolades

    PointVirgule, Virgule,DeuxPoints, Point, // ici les séparateurs

    Identifiant, Nombre, TexteLitteral, CaractereLitteral, Vrai, Faux, // ici les valeurs et noms
    Affiche,
    AfficheSansRetourLigne,
    Lire,
    FinFichier // ici pour savoir qu'on est à la fin du code
}
