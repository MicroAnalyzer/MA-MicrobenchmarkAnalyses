package joelbits.modules.analysis.visitors;

import joelbits.model.ast.*;
import joelbits.model.ast.types.ModifierType;

import java.util.ArrayList;
import java.util.List;

public class BenchmarkEvolutionVisitor implements Visitor {
    private final List<String> benchmarkChanges = new ArrayList<>();

    @Override
    public boolean visit(ASTNode node) {
        return false;
    }

    @Override
    public boolean visitEnter(ASTNode node) {
        if (node instanceof Declaration) {
            Declaration declaration = (Declaration) node;
            for (Method method : declaration.getMethods()) {
                for (Modifier modifier : method.getModifiers()) {
                    if (modifier.getType().equals(ModifierType.ANNOTATION) && modifier.getName().equals("Benchmark")) {
                        benchmarkChanges.add(method.getName());
                        break;
                    }
                }
            }

            return false;
        }

        return !(node instanceof Variable) && !(node instanceof Method);
    }

    @Override
    public boolean visitLeave(ASTNode node) {
        return !(node instanceof Variable);
    }

    public List<String> getBenchmarkChanges() {
        return benchmarkChanges;
    }
}
