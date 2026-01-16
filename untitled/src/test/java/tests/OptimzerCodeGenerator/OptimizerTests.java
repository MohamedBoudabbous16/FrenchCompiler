package tests.OptimzerCodeGenerator;
import main.java.optimizer.Optimizer;
import main.java.parseur.ast.Programme;

import org.junit.jupiter.api.Test;
import tests.TestTools;

import static org.junit.jupiter.api.Assertions.*;

public class OptimizerTests {

    private static final String SRC_DEAD_CODE = """
        fonction main() {
          affiche("a");
          retourne 1;
          affiche("b"); // code mort
        }
        """;

    private static final String SRC_IF_TRUE = """
        fonction main() {
          si (true) {
            affiche("ok");
          } sinon {
            affiche("ko");
          }
          retourne 1;
        }
        """;

    private static final String SRC_TANTQUE_FALSE = """
        fonction main() {
          tantque(false) {
            affiche("jamais");
          }
          retourne 1;
        }
        """;

    private static final String SRC_CONST_FOLD = """
        fonction main() {
          x = (2 + 3) * 4;
          affiche("x=", x);
          retourne x;
        }
        """;

    @Test
    void optimizer_smoke_dead_code() {
        Object programme = TestTools.parseProgramme(SRC_DEAD_CODE);
        Optimizer opt = new Optimizer();
        opt.optimize((Programme) programme);
        assertNotNull(programme);
    }

    @Test
    void optimizer_smoke_si_true() {
        Object programme = TestTools.parseProgramme(SRC_IF_TRUE);
        Optimizer opt = new Optimizer();
        opt.optimize((Programme) programme);
        assertNotNull(programme);
    }

    @Test
    void optimizer_smoke_tantque_false() {
        Object programme = TestTools.parseProgramme(SRC_TANTQUE_FALSE);
        Optimizer opt = new Optimizer();
        opt.optimize((Programme) programme);
        assertNotNull(programme);
    }

    @Test
    void optimizer_smoke_const_fold() {
        Object programme = TestTools.parseProgramme(SRC_CONST_FOLD);
        Optimizer opt = new Optimizer();
        opt.optimize((Programme) programme);
        assertNotNull(programme);
    }
}
