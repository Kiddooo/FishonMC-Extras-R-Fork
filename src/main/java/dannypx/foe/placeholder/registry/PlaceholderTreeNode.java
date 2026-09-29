package dannypx.foe.placeholder.registry;

import dannypx.foe.placeholder.functions.*;
import net.minecraft.network.chat.MutableComponent;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

public class PlaceholderTreeNode {
    private enum WildcardType { NONE, INDEX, STRING, STRING_ARRAY }

    public enum ValueKind { NONE, STRING, COMPONENT, NUMBER, BOOLEAN, VALUE }
    public enum EvalKind { NONE, STRING, COMPONENT, NUMBER, BOOLEAN, VALUE }
    public enum DocTypeKind { STRING, COMPONENT, NUMBER, BOOLEAN, VALUE }

    public record Param(String name, boolean optional, boolean variadic, DocTypeKind ...type) {
        public String getType() {
            StringBuilder str = new StringBuilder();
            for (DocTypeKind typeKind : type) {
                switch (typeKind) {
                    case STRING -> {
                        if (str.isEmpty()) str.append("string");
                        else str.append("|string");
                    }
                    case COMPONENT -> {
                        if (str.isEmpty()) str.append("component");
                        else str.append("|component");
                    }
                    case NUMBER -> {
                        if (str.isEmpty()) str.append("number");
                        else str.append("|number");
                    }
                    case BOOLEAN -> {
                        if (str.isEmpty()) str.append("boolean");
                        else str.append("|boolean");
                    }
                    case VALUE -> {
                        if (str.isEmpty()) str.append("dynamic");
                        else str.append("|dynamic");
                    }
                }
            }
            return str.toString();
        }
    }

    private final String key;
    private final WildcardType wildcardType;
    private final Map<String, PlaceholderTreeNode> children = new HashMap<>();

    private PlaceholderTreeNode indexChild;
    private PlaceholderTreeNode stringChild;
    private PlaceholderTreeNode stringArrayChild;

    private Function<List<String>, PlaceholderValue> resolver;
    private Function<List<PlaceholderValue>, PlaceholderValue> evalFunction;

    private boolean allowEmpty = false;

    private ValueKind valueKind = ValueKind.NONE;
    private EvalKind evalKind = EvalKind.NONE;
    private final List<Param> params = new ArrayList<>();
    private String description = null;

    private PlaceholderTreeNode(@Nullable String key, WildcardType wildcardType) {
        this.key = key;
        this.wildcardType = wildcardType;
    }

    /// Tree builder

    public static PlaceholderTreeNode node(String key) {
        return new PlaceholderTreeNode(key, WildcardType.NONE);
    }

    public static PlaceholderTreeNode nodeIndex() {
        return new PlaceholderTreeNode(null, WildcardType.INDEX);
    }

    public static PlaceholderTreeNode nodeString() {
        return new PlaceholderTreeNode(null, WildcardType.STRING);
    }

    public static PlaceholderTreeNode nodeStringArray() {
        return new PlaceholderTreeNode(null, WildcardType.STRING_ARRAY);
    }

    public PlaceholderTreeNode branch(PlaceholderTreeNode child) {
        switch (child.wildcardType) {
            case NONE -> children.put(child.key, child);
            case INDEX -> this.indexChild = child;
            case STRING -> this.stringChild = child;
            case STRING_ARRAY -> this.stringArrayChild = child;
        }
        return this;
    }

    /// .value resolvers

    public PlaceholderTreeNode value(Supplier<PlaceholderValue> supplier) {
        this.resolver = args -> supplier.get();
        this.valueKind = ValueKind.VALUE;
        return this;
    }

    public PlaceholderTreeNode value(PlaceholderValueFunction function) {
        this.resolver = function::resolve;
        this.valueKind = ValueKind.VALUE;
        return this;
    }

    public PlaceholderTreeNode valueString(Supplier<String> supplier) {
        this.resolver = args -> PlaceholderValue.text(supplier.get());
        this.valueKind = ValueKind.STRING;
        return this;
    }

    public PlaceholderTreeNode valueString(PlaceholderStringFunction function) {
        this.resolver = args -> PlaceholderValue.text(function.resolve(args));
        this.valueKind = ValueKind.STRING;
        return this;
    }

    public PlaceholderTreeNode valueComponent(Supplier<MutableComponent> supplier) {
        this.resolver = args -> PlaceholderValue.component(supplier.get());
        this.valueKind = ValueKind.COMPONENT;
        return this;
    }

    public PlaceholderTreeNode valueComponent(PlaceholderComponentFunction function) {
        this.resolver = args -> PlaceholderValue.component(function.resolve(args));
        this.valueKind = ValueKind.COMPONENT;
        return this;
    }

    public PlaceholderTreeNode valueNumber(Supplier<Number> supplier) {
        this.resolver = args -> PlaceholderValue.number(supplier.get());
        this.valueKind = ValueKind.NUMBER;
        return this;
    }

    public PlaceholderTreeNode valueNumber(PlaceholderNumberFunction function) {
        this.resolver = args -> PlaceholderValue.number(function.resolve(args));
        this.valueKind = ValueKind.NUMBER;
        return this;
    }

    public PlaceholderTreeNode valueBoolean(Supplier<Boolean> supplier) {
        this.resolver = args -> PlaceholderValue.bool(supplier.get());
        this.valueKind = ValueKind.BOOLEAN;
        return this;
    }

    public PlaceholderTreeNode valueBoolean(PlaceholderBooleanFunction function) {
        this.resolver = args -> PlaceholderValue.bool(function.resolve(args));
        this.valueKind = ValueKind.BOOLEAN;
        return this;
    }

    public PlaceholderTreeNode evalString(PlaceholderEvalStringFunction function) {
        this.evalFunction = args -> PlaceholderValue.text(function.resolve(args));
        this.evalKind = EvalKind.STRING;
        return this;
    }

    public PlaceholderTreeNode evalComponent(PlaceholderEvalComponentFunction function) {
        this.evalFunction = args -> PlaceholderValue.component(function.resolve(args));
        this.evalKind = EvalKind.COMPONENT;
        return this;
    }

    public PlaceholderTreeNode evalNumber(PlaceholderEvalNumberFunction function) {
        this.evalFunction = args -> PlaceholderValue.number(function.resolve(args));
        this.evalKind = EvalKind.NUMBER;
        return this;
    }

    public PlaceholderTreeNode evalBoolean(PlaceholderEvalBooleanFunction function) {
        this.evalFunction = args -> PlaceholderValue.bool(function.resolve(args));
        this.evalKind = EvalKind.BOOLEAN;
        return this;
    }

    public PlaceholderTreeNode evalValue(PlaceholderEvalValueFunction function) {
        this.evalFunction = function::resolve;
        this.evalKind = EvalKind.VALUE;
        return this;
    }

    /// Documentation

    public PlaceholderTreeNode param(String name, DocTypeKind ...type) {
        params.add(new Param(name, false, false, type));
        return this;
    }

    public PlaceholderTreeNode paramOptional(String name, DocTypeKind ...type) {
        params.add(new Param(name, true, false, type));
        return this;
    }

    public PlaceholderTreeNode paramVariadic(String name, DocTypeKind ...type) {
        params.add(new Param(name, false, true, type));
        return this;
    }

    public PlaceholderTreeNode description(String text) {
        this.description = text;
        return this;
    }

    ///

    public PlaceholderTreeNode allowEmpty() {
        this.allowEmpty = true;
        return this;
    }

    public boolean allowsEmpty() {
        return allowEmpty;
    }

    public String key() {
        return key;
    }

    public boolean hasNamedChild(String key) {
        return children.containsKey(key);
    }

    public boolean hasIndexChild() {
        return indexChild != null;
    }

    public boolean hasStringWildcard() {
        return stringChild != null;
    }

    public boolean hasStringArrayWildcard() {
        return stringArrayChild != null;
    }

    public Map<String, PlaceholderTreeNode> getChildren() {
        return Collections.unmodifiableMap(children);
    }

    public PlaceholderTreeNode getIndexChild() {
        return indexChild;
    }

    public PlaceholderTreeNode getStringChild() {
        return stringChild;
    }

    public PlaceholderTreeNode getStringArrayChild() {
        return stringArrayChild;
    }

    public ValueKind getValueKind() {
        return valueKind;
    }

    public EvalKind getEvalKind() {
        return evalKind;
    }

    public List<Param> getParams() {
        return Collections.unmodifiableList(params);
    }

    public String getDescription() {
        return description;
    }

    public PlaceholderTreeNode resolveChild(String segment, List<String> captured) {
        PlaceholderTreeNode named = children.get(segment);

        if(named != null) {
            return named;
        }

        if(indexChild != null && PlaceholderTreeNode.isNumeric(segment)) {
            captured.add(segment);
            return indexChild;
        }

        if(stringChild != null) {
            captured.add(segment);
            return stringChild;
        }

        if(stringArrayChild != null) {
            captured.add(segment);
            return stringArrayChild;
        }

        if(wildcardType == WildcardType.STRING_ARRAY) {
            captured.add(segment);
            return this;
        }

        return null;
    }

    private static boolean isNumeric(String s) {
        if(s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            if(!Character.isDigit(s.charAt(i))) return false;
        }
        return true;
    }

    public boolean hasResolver() {
        return resolver != null;
    }

    public boolean hasEval() {
        return evalFunction != null;
    }

    public PlaceholderValue resolveValue(List<String> indices) {
        return resolver.apply(indices);
    }

    public PlaceholderValue resolveEval(List<PlaceholderValue> args) {
        return evalFunction.apply(args);
    }
}
