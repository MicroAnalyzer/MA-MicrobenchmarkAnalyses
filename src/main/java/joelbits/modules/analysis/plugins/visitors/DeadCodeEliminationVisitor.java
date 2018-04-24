package joelbits.modules.analysis.plugins.visitors;

import joelbits.model.ast.*;
import joelbits.model.ast.types.ModifierType;
import joelbits.modules.analysis.visitors.Visitor;

import java.util.*;

public final class DeadCodeEliminationVisitor implements Visitor {
    private int allowDCE;
    private int nrBenchmarks;
    private final Set<String> variableOccurrences = new HashSet<>();
    private final Set<String> variableDeclarations = new HashSet<>();

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
                    if (isBenchmark(modifier)) {
                        nrBenchmarks++;
                        checkDCE(method);
                    }
                }
            }

            return false;
        }
        return !(node instanceof Variable) && !(node instanceof Method);
    }

    /**
     * Check if there are any dead variables in the method body. All variables that are assigned values or
     * are declared in the method body are stored in a list. All variables used in expressions, as arguments,
     * and so on are stored in another list. The variables in the first list which occur in the other list are
     * removed and if any variables is left in the first list it is considered a dead variable.
     *
     * @param method        the method for investigation
     */
    private void checkDCE(Method method) {
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

        allowsDCE();
        variableDeclarations.clear();
        variableOccurrences.clear();
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
        if (expression.getType().equals(joelbits.model.ast.types.ExpressionType.VARIABLE_DECLARATION)) {
            variableDeclarations.add(expression.getVariable());
        }
        else {
            variableOccurrences.add(expression.getVariable());
        }
        variableOccurrences.add(expression.getLiteral());
        variableOccurrences.add(expression.getMethod());
        variableOccurrences.add(expression.getNewType().getName());
    }

    private void checkVariable(Variable variable) {
        checkExpression(variable.getInitializer());
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
     * Look for dead variables.
     */
    private void allowsDCE() {
        List<String> variables = new ArrayList<>(variableDeclarations);

        for (String occurrence : variableOccurrences) {
            for (String declaration : variableDeclarations) {
                if (occurrence.contains(declaration)) {
                    variables.remove(declaration);
                }
            }
        }
        if (!variables.isEmpty()) {
            allowDCE++;
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

    public int getAllowDCE() {
        return allowDCE;
    }

    public void resetAllowDCE() {
        allowDCE = 0;
    }

    public void resetBenchmarks() {
        nrBenchmarks = 0;
    }

    public int getNrBenchmarks() {
        return nrBenchmarks;
    }
}
