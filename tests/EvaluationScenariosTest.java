import app.EvaluationScenarios;

/** Ensures every documented evaluator scenario remains executable. */
public final class EvaluationScenariosTest {
    public static void main(String[] args) throws Exception {
        EvaluationScenarios.main(new String[0]);
        System.out.println("EvaluationScenariosTest passed");
    }
}
