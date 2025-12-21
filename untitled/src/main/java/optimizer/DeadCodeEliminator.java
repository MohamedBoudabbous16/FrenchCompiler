package main.java.optimizer;

import main.java.parseur.ast.*;
import main.java.parseur.ast.controle.Pour;
import main.java.parseur.ast.controle.Si;
import main.java.parseur.ast.controle.TantQue;

import java.util.List;

/**
 * Passe : suppression de code mort + simplifications de contrôle.
 */
class DeadCodeEliminator {

    void optimize(Programme programme) {
        for (Classe c : programme.getClasses()) {
            for (Fonction f : c.getFonctions()) {
                optimizeInstruction(f.getCorps());
            }
        }
    }

    private void optimizeInstruction(Instruction i) {
        if (i == null) return;

        if (i instanceof Bloc b) {
            optimizeBloc(b);
            return;
        }

        if (i instanceof Si s) {
            // Optimiser la condition (si elle est simplifiable, ConstantFolder le fait déjà)
            // puis optimiser les branches
            optimizeInstruction(s.getAlorsInstr());
            if (s.getSinonInstr() != null) optimizeInstruction(s.getSinonInstr());

            // Simplification si condition constante (true/false)
            Boolean cond = ConstEval.tryEvalBoolean(s.getCondition());
            if (cond != null) {
                Instruction replacement = cond ? s.getAlorsInstr() : s.getSinonInstr();
                if (replacement == null) {
                    // remplacer par bloc vide : on retire plutôt au niveau du parent Bloc
                    // ici on ne peut rien faire de sûr sans accès au parent
                    return;
                }
                // Remplacement via réflexion si possible (sinon, parent Bloc fera rien)
                AstReflection.trySetField(s, "condition", s.getCondition()); // no-op safe
                // On ne peut pas "remplacer" l'objet s lui-même ici sans parent,
                // donc l'élimination effective se fait dans optimizeBloc.
            }
            return;
        }

        if (i instanceof TantQue tq) {
            optimizeInstruction(tq.getCorps());

            Boolean cond = ConstEval.tryEvalBoolean(tq.getCondition());
            if (cond != null && !cond) {
                // while(false) => supprimer (parent Bloc)
                return;
            }
            return;
        }

        if (i instanceof Pour p) {
            optimizeInstruction(p.getCorps());
            return;
        }

        // Affectation / Affiche / Retourne / AppelFonctionInstr : rien à faire ici
    }

    private void optimizeBloc(Bloc bloc) {
        List<Instruction> instrs = bloc.getInstructions();
        if (instrs == null) return;

        // 1) Optimiser récursivement chaque instruction
        for (int idx = 0; idx < instrs.size(); idx++) {
            Instruction cur = instrs.get(idx);
            optimizeInstruction(cur);

            // 2) simplifications locales possible : si / tantque avec condition constante
            if (cur instanceof Si s) {
                Boolean cond = ConstEval.tryEvalBoolean(s.getCondition());
                if (cond != null) {
                    Instruction replacement = cond ? s.getAlorsInstr() : s.getSinonInstr();
                    if (replacement == null) {
                        // pas de sinon et condition false => supprimer
                        instrs.remove(idx);
                        idx--;
                        continue;
                    }
                    // remplacer l'instruction Si par la branche choisie
                    instrs.set(idx, replacement);
                    // re-optimiser la branche injectée
                    optimizeInstruction(replacement);
                    continue;
                }
            }

            if (cur instanceof TantQue tq) {
                Boolean cond = ConstEval.tryEvalBoolean(tq.getCondition());
                if (cond != null && !cond) {
                    // tantque(false) => supprimer
                    instrs.remove(idx);
                    idx--;
                    continue;
                }
            }

            // 3) dead code : tout ce qui suit un Retourne est supprimé
            if (cur instanceof Retourne) {
                while (instrs.size() > idx + 1) {
                    instrs.remove(idx + 1);
                }
                break;
            }
        }

        // 4) Optimiser les blocs imbriqués après réécritures
        for (Instruction cur : instrs) {
            optimizeInstruction(cur);
        }
    }
}
