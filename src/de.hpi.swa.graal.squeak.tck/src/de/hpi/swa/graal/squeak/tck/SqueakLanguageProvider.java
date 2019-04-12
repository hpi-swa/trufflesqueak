package de.hpi.swa.graal.squeak.tck;

import static org.graalvm.polyglot.tck.TypeDescriptor.ANY;
import static org.graalvm.polyglot.tck.TypeDescriptor.BOOLEAN;
import static org.graalvm.polyglot.tck.TypeDescriptor.NULL;
import static org.graalvm.polyglot.tck.TypeDescriptor.NUMBER;
import static org.graalvm.polyglot.tck.TypeDescriptor.OBJECT;
import static org.graalvm.polyglot.tck.TypeDescriptor.STRING;
import static org.graalvm.polyglot.tck.TypeDescriptor.array;
import static org.graalvm.polyglot.tck.TypeDescriptor.union;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.tck.LanguageProvider;
import org.graalvm.polyglot.tck.ResultVerifier;
import org.graalvm.polyglot.tck.Snippet;
import org.graalvm.polyglot.tck.TypeDescriptor;

import de.hpi.swa.graal.squeak.shared.SqueakLanguageConfig;

public final class SqueakLanguageProvider implements LanguageProvider {
    private static final TypeDescriptor COLLECTION = TypeDescriptor.union(array(ANY), STRING);
    private static final TypeDescriptor NUMBER_AND_STRING = TypeDescriptor.union(NUMBER, STRING);

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
        final Collection<Snippet> snippets = new ArrayList<>();
        addValueConstructor(context, snippets, "true", BOOLEAN, "[ true ]");
        addValueConstructor(context, snippets, "false", BOOLEAN, "[ false ]");
        addValueConstructor(context, snippets, "nil", NULL, "[ nil ]");
        addValueConstructor(context, snippets, "SmallInteger", NUMBER, "[ 24 ]");
        addValueConstructor(context, snippets, "SmallFloat", NUMBER, "[ 1.23 ]");
        addValueConstructor(context, snippets, "ByteString", STRING, "[ 'spam' ]");
        addValueConstructor(context, snippets, "ByteSymbol", STRING, "[ #bar ]");
        addValueConstructor(context, snippets, "Array:Empty", array(ANY), "[ #() ]");
        addValueConstructor(context, snippets, "Array:SmallIntegers", array(NUMBER), "[ #(8 2 4) ]");
        addValueConstructor(context, snippets, "Array:SmallFloats", array(NUMBER), "[ #(2.3 1.2 3.4) ]");
        addValueConstructor(context, snippets, "Array:ByteStrings", array(STRING), "[ #('foo' 'bar') ]");
        addValueConstructor(context, snippets, "Array:Mixed", array(union(NUMBER, STRING)), "[ #(24 'bar') ]");
        addValueConstructor(context, snippets, "Object:Empty", OBJECT, "[ Object new ]");
        return snippets;
    }

    @Override
    public Collection<? extends Snippet> createExpressions(final Context context) {
        final Collection<Snippet> snippets = new ArrayList<>();
        // addition
        addExpressionSnippet(context, snippets, "+", "[ :x :y | x + y ]", NUMBER, ComparisonVerifier.INSTANCE, NUMBER_AND_STRING, NUMBER_AND_STRING);

        // subtraction
        addExpressionSnippet(context, snippets, "-", "[ :x :y | x - y ]", NUMBER, ComparisonVerifier.INSTANCE, NUMBER_AND_STRING, NUMBER_AND_STRING);

        // multiplication
        addExpressionSnippet(context, snippets, "*", "[ :x :y | x * y ]", NUMBER, ComparisonVerifier.INSTANCE, NUMBER_AND_STRING, NUMBER_AND_STRING);

        // division
        addExpressionSnippet(context, snippets, "/", "[ :x :y | x / y ]", NUMBER, ComparisonVerifier.INSTANCE, NUMBER_AND_STRING, NUMBER_AND_STRING);

        // comparison
        addExpressionSnippet(context, snippets, ">", "[ :x :y | x > y ]", BOOLEAN, ComparisonVerifier.INSTANCE, NUMBER_AND_STRING, NUMBER_AND_STRING);
        addExpressionSnippet(context, snippets, ">=", "[ :x :y | x >= y ]", BOOLEAN, ComparisonVerifier.INSTANCE, NUMBER_AND_STRING, NUMBER_AND_STRING);
        addExpressionSnippet(context, snippets, "<", "[ :x :y | x < y ]", BOOLEAN, ComparisonVerifier.INSTANCE, NUMBER_AND_STRING, NUMBER_AND_STRING);
        addExpressionSnippet(context, snippets, "<=", "[ :x :y | x <= y ]", BOOLEAN, ComparisonVerifier.INSTANCE, NUMBER_AND_STRING, NUMBER_AND_STRING);

        return snippets;
    }

    @Override
    public Collection<? extends Snippet> createStatements(final Context context) {
        final Collection<Snippet> statements = new ArrayList<>();
        addStatementSnippet(context, statements, "IdentityFunction", "[ :p | p ]", TypeDescriptor.ANY, TypeDescriptor.ANY);
        addStatementSnippet(context, statements, "class", "[ :p | p class ]", TypeDescriptor.OBJECT, TypeDescriptor.ANY);
        addStatementSnippet(context, statements, "size", "[ :p | p size ]", TypeDescriptor.NUMBER, TypeDescriptor.ANY);
        addStatementSnippet(context, statements, "hash", "[ :p | p hash ]", TypeDescriptor.NUMBER, TypeDescriptor.ANY);

        addStatementSnippet(context, statements, "ifTrue:ifFalse:", "[ :p | p ifTrue: [ true ] ifFalse: [ false ] ]", TypeDescriptor.BOOLEAN, TypeDescriptor.BOOLEAN);
        addStatementSnippet(context, statements, "ifNil:ifNotNil:", "[ :p | p ifNil: [ true ] ifNotNil: [ false ] ]", TypeDescriptor.BOOLEAN, TypeDescriptor.ANY);

        addStatementSnippet(context, statements, "sorted", "[ :p | p sorted ]", COLLECTION, COLLECTION);

        return statements;
    }

    @Override
    public Collection<? extends Snippet> createScripts(final Context context) {
        final Collection<Snippet> snippets = new ArrayList<>();
        return snippets;
    }

    @Override
    public Collection<? extends Source> createInvalidSyntaxScripts(final Context context) {
        final Collection<Source> scripts = new ArrayList<>();
        addSource(scripts, "MissingBracket", "[ :p | p");
        addSource(scripts, "Invalid", "$#%^&*");
        return scripts;
    }

    private static void addValueConstructor(final Context context, final Collection<Snippet> snippets, final String id, final TypeDescriptor returnType, final String code) {
        snippets.add(Snippet.newBuilder(id, context.eval(SqueakLanguageConfig.ID, code), returnType).build());
    }

    private static void addExpressionSnippet(final Context context, final Collection<Snippet> snippets, final String id, final String code, final TypeDescriptor returnType,
                    final TypeDescriptor... parameterTypes) {
        snippets.add(Snippet.newBuilder(id, context.eval(SqueakLanguageConfig.ID, code), returnType).parameterTypes(parameterTypes).build());
    }

    private static void addExpressionSnippet(final Context context, final Collection<Snippet> snippets, final String id, final String code, final TypeDescriptor returnType, final ResultVerifier rv,
                    final TypeDescriptor... parameterTypes) {
        snippets.add(Snippet.newBuilder(id, context.eval(SqueakLanguageConfig.ID, code), returnType).resultVerifier(rv).parameterTypes(parameterTypes).build());
    }

    private static void addSource(final Collection<Source> scripts, final String name, final CharSequence code) {
        try {
            scripts.add(Source.newBuilder(SqueakLanguageConfig.ID, code, name).build());
        } catch (final IOException e) {
            throw new AssertionError("IOException while creating a test source.", e);
        }
    }

    private static void addStatementSnippet(final Context context, final Collection<Snippet> snippets, final String id, final String code, final TypeDescriptor returnType,
                    final TypeDescriptor... parameterTypes) {
        snippets.add(Snippet.newBuilder(id, context.eval(SqueakLanguageConfig.ID, code), returnType).parameterTypes(parameterTypes).build());
    }

    private static class ComparisonVerifier implements ResultVerifier {
        private static final ComparisonVerifier INSTANCE = new ComparisonVerifier();

        @Override
        public void accept(final SnippetRun snippetRun) throws PolyglotException {
            final List<? extends Value> parameters = snippetRun.getParameters();
            assert parameters.size() == 2;

            final Value par0 = parameters.get(0);
            final Value par1 = parameters.get(1);

            if (isSqueakNumberOrCharacter(par0) && isSqueakNumberOrCharacter(par1)) {
                if (snippetRun.getException() != null) {
                    throw new AssertionError("Squeak Numbers and Characters should not throw exception: " + snippetRun.getException());
                }
            } else if (isSqueakString(par0) && isSqueakString(par1)) {
                if (snippetRun.getException() != null) {
                    throw new AssertionError("Squeak Strings and Strings should not throw exception: " + snippetRun.getException());
                }
            } else if (isSqueakString(par0) || isSqueakString(par1)) {
                if (snippetRun.getException() == null) {
                    throw new AssertionError("Squeak Strings and Numbers should raise an exception, result: " + snippetRun.getResult());
                }
            } else {
                ResultVerifier.getDefaultResultVerifier().accept(snippetRun);
            }
        }

        private static boolean isSqueakNumberOrCharacter(final Value val) {
            return val.isNumber() || isSqueakCharacter(val);
        }

        private static boolean isSqueakCharacter(final Value val) {
            return val.isString() && val.asString().length() == 1;
        }

        private static boolean isSqueakString(final Value val) {
            return val.isString() && val.asString().length() != 1;
        }
    }
}
