package joelbits.modules.analysis.plugins.visitors;

import joelbits.model.ast.*;
import joelbits.model.ast.types.ModifierType;
import joelbits.modules.analysis.visitors.Visitor;

import java.util.*;

public final class BenchmarkCountVisitor implements Visitor {
    private int sum;
    private final Map<String, Integer> changes = new HashMap<>();
    private final Map<String, Method> currentImplementation = new HashMap<>();

    @Override
    public boolean visit(ASTNode node) {
        return false;
    }

    @Override
    public boolean visitEnter(ASTNode node) {
        sum = 0;
        if (node instanceof Declaration) {
            Declaration declaration = (Declaration) node;
            List<Method> methods = declaration.getMethods();
            for (Method method : methods) {
                for (Modifier modifier : method.getModifiers()) {
                    if (modifier.getType().equals(ModifierType.ANNOTATION) && modifier.getName().equals("Benchmark")) {
                        sum++;
                        if (changes.containsKey(method.getName())) {
                            boolean changed = hasChanged(method);
                            if (changed) {
                                int modifications = changes.get(method.getName());
                                changes.put(method.getName(), ++modifications);
                                currentImplementation.put(method.getName(), method);
                            }
                        } else {
                            changes.put(method.getName(), 0);
                            currentImplementation.put(method.getName(), method);
                            break;
                        }
                    }
                }
            }
            return false;
        }

        return true;
    }

    private boolean hasChanged(Method method) {
        Method oldMethod = currentImplementation.get(method.getName());
        if (oldMethod.getStatements().size() != method.getStatements().size() ||
                oldMethod.getBodyContent().size() != method.getBodyContent().size() ||
                oldMethod.getModifiers().size() != method.getModifiers().size() ||
                oldMethod.getArguments().size() != method.getArguments().size()) {
            return true;
        }

        if (!oldMethod.getReturnType().getName().equals(method.getReturnType().getName()) ||
                !oldMethod.getReturnType().getType().equals(method.getReturnType().getType())) {
            return true;
        }
        for (Variable variable : method.getArguments()) {
            Optional<Variable> oldVariable = oldMethod.getArguments().stream().filter(o -> o.getName().equals(variable.getName())).findFirst();
            if (!oldVariable.isPresent()) {
                return true;
            } else {
                if (variableChanged(oldVariable.get(), variable)) {
                    return true;
                }
            }
        }

        for (Modifier modifier : method.getModifiers()) {
            Optional<Modifier> oldModifier = oldMethod.getModifiers().stream().filter(o -> o.getName().equals(modifier.getName())).findFirst();
            if (!oldModifier.isPresent()) {
                return true;
            } else {
                if (modifierChanged(oldModifier.get(), modifier)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean modifierChanged(Modifier old, Modifier current) {
        if (!old.getType().equals(current.getType()) || !old.getVisibility().equals(current.getVisibility())
                || !old.getOther().equals(current.getOther()) ||
                old.getMembersAndValues().size() != current.getMembersAndValues().size()) {
            return true;
        }
        for (String value : old.getMembersAndValues()) {
            if (!current.getMembersAndValues().contains(value)) {
                return true;
            }
        }

        return false;
    }

    private boolean variableChanged(Variable old, Variable current) {
        if (!current.getType().getType().equals(old.getType().getType()) ||
                !current.getType().getName().equals(old.getType().getName())) {
            return true;
        }
        if (current.getModifiers().size() != old.getModifiers().size()) {
            return true;
        }
        for (Modifier modifier : current.getModifiers()) {
            Optional<Modifier> oldModifier = old.getModifiers().stream().filter(o -> o.getName().equals(modifier.getName())).findFirst();
            if (!oldModifier.isPresent()) {
                return true;
            } else {
                if (modifierChanged(oldModifier.get(), modifier)) {
                    return true;
                }
            }
        }

        return expressionChanged(old.getInitializer(), current.getInitializer());
    }

    private boolean expressionChanged(Expression old, Expression current) {
        if (!current.getType().equals(old.getType())) {
            return true;
        }
        if (old.getExpressions().size() != current.getExpressions().size() ||
                old.getMethodArguments().size() != current.getMethodArguments().size() ||
                old.getVariableDeclarations().size() != current.getVariableDeclarations().size()) {
            return true;
        }
        if (!current.getNewType().getName().equals(old.getNewType().getName()) ||
                !current.getNewType().getType().equals(old.getNewType().getType())) {
            return true;
        }
        if (!current.getVariable().equals(old.getVariable()) || !current.getMethod().equals(old.getMethod())
                || current.getLiteral().equals(old.getLiteral())) {
            return true;
        }

        return false;
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

    public Map<String, Integer> getChanges() { return changes; }
}
