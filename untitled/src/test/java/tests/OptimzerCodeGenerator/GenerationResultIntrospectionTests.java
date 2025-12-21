package test.java.tests.OptimzerCodeGenerator;

import org.junit.jupiter.api.Test;
import test.java.tests.TestTools;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class GenerationResultIntrospectionTests {

    @Test
    void diag_generationresult_api() throws Exception {
        Class<?> gr = TestTools.mustClass("main.java.codegenerator.GenerationResult");

        StringBuilder sb = new StringBuilder();
        sb.append("\n===== GenerationResult API =====\n");
        sb.append("Class: ").append(gr.getName()).append("\n\n");

        sb.append("---- Public methods ----\n");
        for (Method m : gr.getMethods()) {
            if (m.getDeclaringClass() == Object.class) continue;
            sb.append(m.getReturnType().getSimpleName())
                    .append(" ")
                    .append(m.getName())
                    .append("(")
                    .append(Arrays.toString(m.getParameterTypes()))
                    .append(")\n");
        }

        sb.append("\n---- Declared fields ----\n");
        for (Field f : gr.getDeclaredFields()) {
            sb.append(f.getType().getSimpleName())
                    .append(" ")
                    .append(f.getName())
                    .append("\n");
        }

        System.out.println(sb);

        // On force le test à "passer" toujours.
        assertTrue(true);
    }
}
