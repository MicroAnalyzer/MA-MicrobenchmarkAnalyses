package joelbits.modules.analysis.plugins.visitors;

import joelbits.model.ast.*;
import joelbits.model.ast.types.ExpressionType;
import joelbits.model.ast.types.ModifierType;
import joelbits.modules.analysis.visitors.Visitor;

import java.util.*;

public final class ConstantFoldingSourceCodeVisitor implements Visitor {
    private int allowCF;
    private int nrBenchmarks;
    private final Set<String> expressions = new HashSet<>();
    private final Set<String> finalFields = new HashSet<>();
    private final Collection<String> types = Arrays.asList("int", "String", "long");

    @Override
    public boolean visit(ASTNode node) {
        return false;
    }

    @Override
    public boolean visitEnter(ASTNode node) {
        if (node instanceof Declaration) {
            Declaration declaration = (Declaration) node;
            for (Variable field : declaration.getFields()) {
                for (Modifier modifier : field.getModifiers()) {
                    if (modifier.getType().equals(ModifierType.FINAL)) {
                        if (field.getInitializer().getType().equals(ExpressionType.LITERAL) && types.contains((field.getType().getName()))) {
                            finalFields.add(field.getName());
                        }
                    }
                }
            }
            for (Method method : declaration.getMethods()) {
                for (Modifier modifier : method.getModifiers()) {
                    if (!isBenchmark(modifier)) {
                        nrBenchmarks++;
                        checkCF(method);
                    }
                }
            }

            return false;
        }
        return !(node instanceof Variable) && !(node instanceof Method);
    }

    /**
     * Check if there are any final fields with predictable values used inside method.
     *
     * @param method        the method for investigation
     */
    private void checkCF(Method method) {
        for (Expression expression : method.getBodyContent()) {
            checkExpression(expression);
        }

        for (Statement methodBody : method.getStatements()) {
            checkExpressions(methodBody.getExpressions());
            checkStatements(methodBody.getStatements());
            checkExpressions(methodBody.getInitializations());
            checkExpressions(methodBody.getUpdates());
            checkExpression(methodBody.getCondition());
        }

        allowsCF();
        finalFields.clear();
        expressions.clear();
    }

    private void checkExpressions(List<Expression> expressions) {
        for (Expression expression : expressions) {
            checkExpression(expression);
        }
    }

    private void checkExpression(Expression expression) {
        for (Expression exp : expression.getExpressions()) {
            checkExpression(exp);
        }
        for (Expression exp : expression.getMethodArguments()) {
            checkExpression(exp);
        }
        for(Variable variable : expression.getVariableDeclarations()) {
            checkVariable(variable);
        }
        expressions.add(expression.getVariable());
        expressions.add(expression.getLiteral());
        expressions.add(expression.getMethod());
        expressions.add(expression.getNewType().getName());
    }

    private void checkVariable(Variable variable) {
        checkExpression(variable.getInitializer());
        expressions.add(variable.getName());
    }

    private void checkStatements(List<Statement> statements) {
        for (Statement statement : statements) {
            checkStatement(statement);
        }
    }

    private void checkStatement(Statement statement) {
        for (Statement stmt : statement.getStatements()) {
            checkStatement(stmt);
        }
        for (Expression expression : statement.getExpressions()) {
            checkExpression(expression);
        }
        for (Expression expression : statement.getInitializations()) {
            checkExpression(expression);
        }
        for (Expression expression : statement.getUpdates()) {
            checkExpression(expression);
        }
        checkExpression(statement.getCondition());
    }

    /**
     * Look for potential CF optimizations.
     */
    private void allowsCF() {
        for (String expression : expressions) {
            for (String declaration : finalFields) {
                if (expression.contains(declaration)) {
                    allowCF++;
                    return;
                }
            }
        }
    }

    /**
     * Check if a method is a benchmark method.
     *
     * @param modifier      a method modifier
     * @return              true if benchmark method, otherwise false
     */
    private boolean isBenchmark(Modifier modifier) {
        return modifier.getName().equals("Benchmark") && modifier.getType().equals(ModifierType.ANNOTATION);
    }

    @Override
    public boolean visitLeave(ASTNode node) {
        return !(node instanceof Variable);
    }

    public int getAllowCF() {
        return allowCF;
    }

    public void resetAllowCF() { allowCF = 0; }

    public void resetBenchmarks() { nrBenchmarks = 0; }

    public int getNrBenchmarks() {
        return nrBenchmarks;
    }
}
