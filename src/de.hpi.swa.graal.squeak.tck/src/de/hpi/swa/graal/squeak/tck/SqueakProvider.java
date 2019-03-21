package de.hpi.swa.graal.squeak.tck;

import static org.graalvm.polyglot.tck.TypeDescriptor.ANY;
import static org.graalvm.polyglot.tck.TypeDescriptor.BOOLEAN;
import static org.graalvm.polyglot.tck.TypeDescriptor.NULL;
import static org.graalvm.polyglot.tck.TypeDescriptor.NUMBER;
import static org.graalvm.polyglot.tck.TypeDescriptor.OBJECT;
import static org.graalvm.polyglot.tck.TypeDescriptor.STRING;
import static org.graalvm.polyglot.tck.TypeDescriptor.array;
import static org.graalvm.polyglot.tck.TypeDescriptor.union;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.tck.LanguageProvider;
import org.graalvm.polyglot.tck.ResultVerifier;
import org.graalvm.polyglot.tck.Snippet;
import org.graalvm.polyglot.tck.TypeDescriptor;

import de.hpi.swa.graal.squeak.shared.SqueakLanguageConfig;

public final class SqueakProvider implements LanguageProvider {
    @Override
    public String getId() {
        return SqueakLanguageConfig.ID;
    }

    @Override
    public Value createIdentityFunction(final Context context) {
        return context.eval(SqueakLanguageConfig.ID, "[ :x | x ]");
    }

    @Override
    public Collection<? extends Snippet> createValueConstructors(final Context context) {
        final List<Snippet> snippets = new ArrayList<>();
        addValueConstructor(context, snippets, "true", BOOLEAN, "[ true ]");
        addValueConstructor(context, snippets, "false", BOOLEAN, "[ false ]");
        addValueConstructor(context, snippets, "nil", NULL, "[ nil ]");
        addValueConstructor(context, snippets, "SmallInteger", NUMBER, "[ 24 ]");
        addValueConstructor(context, snippets, "SmallFloat", NUMBER, "[ 1.23 ]");
        addValueConstructor(context, snippets, "ByteString", STRING, "[ 'spam' ]");
        addValueConstructor(context, snippets, "ByteSymbol", STRING, "[ #bar ]");
        addValueConstructor(context, snippets, "Array:Empty", array(ANY), "[ #() ]");
        addValueConstructor(context, snippets, "Array:SmallIntegers", array(NUMBER), "[ #(1 2 3) ]");
        addValueConstructor(context, snippets, "Array:SmallFloats", array(NUMBER), "[ #(1.2 2.3 3.4) ]");
        addValueConstructor(context, snippets, "Array:ByteStrings", array(STRING), "[ #('foo' 'bar') ]");
        addValueConstructor(context, snippets, "Array:Mixed", array(union(NUMBER, STRING)), "[ #(24 'bar') ]");
        addValueConstructor(context, snippets, "Object:Empty", OBJECT, "[ Object new ]");
        return snippets;
    }

    @Override
    public Collection<? extends Snippet> createExpressions(final Context context) {
        final List<Snippet> snippets = new ArrayList<>();
        // addition
        addExpressionSnippet(context, snippets, "+", "[ :x :y | x + y ]", NUMBER, NUMBER, NUMBER);

        // subtraction
        addExpressionSnippet(context, snippets, "-", "[ :x :y | x - y ]", NUMBER, NUMBER, NUMBER);

        // multiplication
// addExpressionSnippet(context, snippets, "*", "[ :x :y | x * y ]", NUMBER,
// PNoArrayVerifier.INSTANCE, NUMBER, NUMBER);

        // division
// addExpressionSnippet(context, snippets, "/", "[ :x :y | x / y ]", NUMBER,
// PDivByZeroVerifier.INSTANCE, NUMBER, NUMBER);

        // comparison
// addExpressionSnippet(context, snippets, ">", "[ :x :y | x > y ]", BOOLEAN, NUMBER, NUMBER);
// addExpressionSnippet(context, snippets, ">=", "[ :x :y | x >= y ]", BOOLEAN, NUMBER, NUMBER);
// addExpressionSnippet(context, snippets, "<", "[ :x :y | x < y ]", BOOLEAN, NUMBER, NUMBER);
// addExpressionSnippet(context, snippets, "<=", "[ :x :y | x <= y ]", BOOLEAN, NUMBER, NUMBER);

        // dictionaries
// addExpressionSnippet(context, snippets, "dict", "lambda: { 'x': 4 }", OBJECT);
// addExpressionSnippet(context, snippets, "dict", "lambda: { 'a': 1, 'b': 2, 'c': 3 }", OBJECT, new
// PDictMemberVerifier(arr("get", "keys", "update"), arr("a", "b", "c")));
        return snippets;
    }

    @Override
    public Collection<? extends Snippet> createStatements(final Context context) {
        final List<Snippet> snippets = new ArrayList<>();
        return snippets;
    }

    @Override
    public Collection<? extends Snippet> createScripts(final Context context) {
        final List<Snippet> snippets = new ArrayList<>();
        return snippets;
    }

    @Override
    public Collection<? extends Source> createInvalidSyntaxScripts(final Context context) {
        final List<Source> sources = new ArrayList<>();
        return sources;
    }

    private static void addValueConstructor(final Context context, final Collection<Snippet> snippets, final String id, final TypeDescriptor returnType, final String code) {
        snippets.add(Snippet.newBuilder(id, context.eval(SqueakLanguageConfig.ID, code), returnType).build());
    }

    private static void addExpressionSnippet(final Context context, final List<Snippet> snippets, final String id, final String code, final TypeDescriptor returnType,
                    final TypeDescriptor... parameterTypes) {
        snippets.add(Snippet.newBuilder(id, context.eval(SqueakLanguageConfig.ID, code), returnType).parameterTypes(parameterTypes).build());
    }

    private static void addExpressionSnippet(final Context context, final List<Snippet> snippets, final String id, final String code, final TypeDescriptor returnType, final ResultVerifier rv,
                    final TypeDescriptor... parameterTypes) {
        snippets.add(Snippet.newBuilder(id, context.eval(SqueakLanguageConfig.ID, code), returnType).resultVerifier(rv).parameterTypes(parameterTypes).build());
    }

    private static void addStatementSnippet(final Context context, final List<Snippet> snippets, final String id, final String code, final TypeDescriptor returnType,
                    final TypeDescriptor... parameterTypes) {
        snippets.add(Snippet.newBuilder(id, context.eval(SqueakLanguageConfig.ID, code), returnType).parameterTypes(parameterTypes).build());
    }
}
