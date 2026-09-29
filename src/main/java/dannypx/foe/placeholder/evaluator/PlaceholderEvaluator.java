package dannypx.foe.placeholder.evaluator;

import dannypx.foe.placeholder.functions.PlaceholderValue;
import dannypx.foe.placeholder.parser.ast.*;
import dannypx.foe.placeholder.registry.PlaceholderTreeNode;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;

public class PlaceholderEvaluator {
    public PlaceholderResult eval(Group group) {
        boolean[] success = { true, false };
        List<String> errors = new ArrayList<>();
        MutableComponent combined = Component.empty();
        PlaceholderColorCodes.Tracker colorCodesTracker = new PlaceholderColorCodes.Tracker();
        int[] emptyAllowedDepth = { 0 };

        for (Node child : group.children()) {
            if(child instanceof Literal(String text)) {
                combined.append(colorCodesTracker.consumeLiteral(text));
            } else {
                MutableComponent resolved;
                try {
                    resolved = this.evalNode(child, success, errors, emptyAllowedDepth).toComponent();
                } catch (RuntimeException e) {
                    success[0] = false;
                    String msg = "Unresolved error: " + e;
                    errors.add(msg);
                    resolved = Component.literal(msg).withStyle(ChatFormatting.RED);
                }
                combined.append(colorCodesTracker.applyActiveStyle(resolved));
            }
        }

        if(!errors.isEmpty()) {
            combined = Component.literal(errors.getFirst()).withStyle(ChatFormatting.RED);
        }

        return new PlaceholderResult(combined, success, errors);
    }

    public String evalToPlainText(Group group) {
        return this.eval(group).text().getString();
    }

    private PlaceholderValue evalNode(Node node, boolean[] successAcc, List<String> errors, int[] emptyAllowedDepth) {
        return switch (node) {
            case Literal l -> PlaceholderValue.text(l.text());
            case AstError e -> {
                successAcc[0] = false;
                errors.add(e.message());
                yield PlaceholderValue.component(Component.literal(e.message()).withStyle(ChatFormatting.RED));
            }
            case PlaceholderReference p -> {
                PlaceholderTreeNode treeNode = p.resolved();
                PlaceholderValue result;

                try {
                    result = treeNode.resolveValue(p.indices());
                } catch (PlaceholderEvaluationException e) {
                    yield this.trackedError("'" + treeNode.key() + "' " +  e.getMessage(), successAcc, errors);
                }

                if(!this.isSuccess(treeNode, result, emptyAllowedDepth)) {
                    successAcc[0] = false;
                }

                successAcc[1] = successAcc[1] || result.isForcedFailure();

                yield result.isNull() ? PlaceholderValue.text("") : result;
            }
            case FunctionCall f -> {
                PlaceholderTreeNode treeNode = f.resolved();
                List<PlaceholderValue> evaluatedArgs = new ArrayList<>(f.args().size());

                boolean opensEmptyContext = treeNode.allowsEmpty();
                if(opensEmptyContext) emptyAllowedDepth[0]++;
                try {
                    for(Node argNode : f.args()) {
                        evaluatedArgs.add(this.evalNode(argNode, successAcc, errors, emptyAllowedDepth));
                    }
                } finally {
                    if(opensEmptyContext) emptyAllowedDepth[0]--;
                }

                PlaceholderValue result;
                try {
                    result = treeNode.resolveEval(evaluatedArgs);;
                } catch (PlaceholderEvaluationException e) {
                    yield this.trackedError("'" + treeNode.key() + "' " +  e.getMessage(), successAcc, errors);
                }

                if(!isSuccess(treeNode, result, emptyAllowedDepth)) {
                    successAcc[0] = false;
                }

                successAcc[1] = successAcc[1] || result.isForcedFailure();

                yield result.isNull() ? PlaceholderValue.text("") : result;
            }
            case BinaryOp b -> {
                PlaceholderValue left = this.evalNode(b.left(), successAcc, errors, emptyAllowedDepth);
                PlaceholderValue right = this.evalNode(b.right(), successAcc, errors, emptyAllowedDepth);
                yield this.applyBinary(b.op(), left, right, successAcc, errors);
            }
            case UnaryOp u -> {
                PlaceholderValue operand = this.evalNode(u.operand(), successAcc, errors, emptyAllowedDepth);
                yield this.applyUnary(u.op(), operand, successAcc, errors);
            }
            case Group g -> {
                MutableComponent combined = Component.empty();

                for(Node c : g.children()) {
                    combined.append(this.evalNode(c, successAcc, errors, emptyAllowedDepth).toComponent());
                }
                yield PlaceholderValue.component(combined);
            }
        };
    }

    private boolean isSuccess(PlaceholderTreeNode node, PlaceholderValue result, int[] emptyAllowedDepth) {
        if(result.isNull()) return false;
        return !result.isEmpty() || node.allowsEmpty() || emptyAllowedDepth[0] > 0;
    }

    /// Binary/Unary evaluation

    private PlaceholderValue applyBinary(String op, PlaceholderValue leftValue, PlaceholderValue rightValue, boolean[] successAcc, List<String> errors) {
        if(op.equals("==") || op.equals("!=")) {
            boolean equal = (leftValue.isValidNumber() && rightValue.isValidNumber())
                    ? leftValue.toDouble() == rightValue.toDouble()
                    : leftValue.toString().equals(rightValue.toString());
            boolean result = op.equals("==") == equal;
            return PlaceholderValue.text(String.valueOf(result));
        }

        if(!leftValue.isValidNumber() || !rightValue.isValidNumber()) {
            PlaceholderValue badOperand = leftValue.isValidNumber() ? rightValue : leftValue;
            return this.trackedError(
                    "Non-numeric operand for '" + op + "': '" + badOperand.toString() + "'",
                    successAcc, errors
            );
        }

        double left = leftValue.toDouble();
        double right = rightValue.toDouble();
        return switch (op) {
            case "<" -> PlaceholderValue.text(String.valueOf(left < right));
            case ">" -> PlaceholderValue.text(String.valueOf(left > right));
            case "<=" -> PlaceholderValue.text(String.valueOf(left <= right));
            case ">=" -> PlaceholderValue.text(String.valueOf(left >= right));
            case "+" -> PlaceholderValue.number(left + right);
            case "-" -> PlaceholderValue.number(left - right);
            case "*" -> PlaceholderValue.number(left * right);
            case "/" -> PlaceholderValue.number(left / right);
            default -> this.trackedError("Unknown operator: " + op, successAcc, errors);
        };
    }

    private PlaceholderValue applyUnary(String op, PlaceholderValue operand, boolean[] successAcc, List<String> errors) {
        return switch (op) {
            case "-" -> {
                if(!operand.isValidNumber()) {
                    yield this.trackedError(
                            "Non-numeric operand for unary '-': '" + operand.toString() + "'",
                            successAcc, errors
                    );
                }
                yield PlaceholderValue.number(-operand.toDouble());
            }
            default -> this.trackedError("Unknown unary operator: " + op, successAcc, errors);
        };
    }

    private PlaceholderValue trackedError(String message, boolean[] successAcc, List<String> errors) {
        successAcc[0] = false;
        errors.add(message);
        return PlaceholderValue.component(Component.literal(message).withStyle(ChatFormatting.RED));
    }
}