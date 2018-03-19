package joelbits.modules.analysis.visitors;

import joelbits.model.ast.*;
import joelbits.model.ast.types.ModifierType;

import java.util.List;

/**
 * Counts the number of occurrences of @Benchmark annotations on methods in a file.
 */
public class BenchmarkCountVisitor implements Visitor {
    private int sum;

    @Override
    public boolean visit(ASTNode node) {
        return false;
    }

    @Override
    public boolean visitEnter(ASTNode node) {
        if (node instanceof Declaration) {
            Declaration declaration = (Declaration) node;
            List<Method> methods = declaration.getMethods();
            for (Method method : methods) {
                for (Modifier modifier : method.getModifiers()) {
                    if (modifier.getType().equals(ModifierType.ANNOTATION) && modifier.getName().equals("Benchmark")) {
                        sum++;
                        break;
                    }
                }
            }
            return false;
        }

        return true;
    }

    @Override
    public boolean visitLeave(ASTNode node) {
        return !(node instanceof Variable);
    }

    public int getSum() {
        return sum;
    }

    public void resetSum() {
        sum = 0;
    }
}
