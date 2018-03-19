package joelbits.modules.analysis.visitors;

import joelbits.model.ast.*;

/**
 * The visitEnter(ASTNode node) method determines if the node should be entered, and visitLeave(ASTNode node)
 * determines if the node's sibling nodes should be visited (returning false stops traversal of all siblings).
 *
 * The visit(ASTNode) method is implemented by leaf nodes since they only process themselves.
 */
public interface Visitor {
    boolean visit(ASTNode node);
    boolean visitEnter(ASTNode node);
    boolean visitLeave(ASTNode node);
}
