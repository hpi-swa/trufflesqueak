/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.tck;

import static org.graalvm.polyglot.tck.TypeDescriptor.ANY;
import static org.graalvm.polyglot.tck.TypeDescriptor.BOOLEAN;
import static org.graalvm.polyglot.tck.TypeDescriptor.EXECUTABLE;
import static org.graalvm.polyglot.tck.TypeDescriptor.INSTANTIABLE;
import static org.graalvm.polyglot.tck.TypeDescriptor.NULL;
import static org.graalvm.polyglot.tck.TypeDescriptor.NUMBER;
import static org.graalvm.polyglot.tck.TypeDescriptor.OBJECT;
import static org.graalvm.polyglot.tck.TypeDescriptor.STRING;
import static org.graalvm.polyglot.tck.TypeDescriptor.array;
import static org.graalvm.polyglot.tck.TypeDescriptor.union;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.tck.LanguageProvider;
import org.graalvm.polyglot.tck.Snippet;
import org.graalvm.polyglot.tck.TypeDescriptor;

import de.hpi.swa.trufflesqueak.shared.SqueakLanguageConfig;

public final class SqueakTCKLanguageProvider implements LanguageProvider {
    private static final TypeDescriptor NUMBER_AND_OBJECT = union(NUMBER, OBJECT);

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
        addValueConstructor(context, snippets, "Array:Mixed", array(union(BOOLEAN, NULL, NUMBER, STRING)), "[ #(true false nil 24 'bar') ]");
        addValueConstructor(context, snippets, "Object:Empty", OBJECT, "[ Object new ]");
        addValueConstructor(context, snippets, "Object:Smalltalk", OBJECT, "[ Smalltalk ]");
        addValueConstructor(context, snippets, "Object:SpecialObjectsArray", OBJECT, "[ Smalltalk specialObjectsArray ]");
        addValueConstructor(context, snippets, "Class:Object", INSTANTIABLE, "[ Object ]");
        addValueConstructor(context, snippets, "CompiledMethod:Object>>#hash", EXECUTABLE, "[ Object>>#hash ]");
        addValueConstructor(context, snippets, "BlockClosure", EXECUTABLE, "[ [ 42 ] ]");
        return snippets;
    }

    @Override
    public Collection<? extends Snippet> createExpressions(final Context context) {
        final Collection<Snippet> snippets = new ArrayList<>();

        // arithmetic
        addExpressionSnippet(context, snippets, "*", "[ :x :y | x * y ]", NUMBER_AND_OBJECT, NUMBER, NUMBER);
        addExpressionSnippet(context, snippets, "+", "[ :x :y | x + y ]", NUMBER_AND_OBJECT, NUMBER, NUMBER);
        addExpressionSnippet(context, snippets, "-", "[ :x :y | x - y ]", NUMBER_AND_OBJECT, NUMBER, NUMBER);
        addExpressionSnippet(context, snippets, "/", "[ :x :y | x / y ]", NUMBER_AND_OBJECT, NUMBER, NUMBER);

        // comparison
        addExpressionSnippet(context, snippets, "<", "[ :x :y | x < y ]", BOOLEAN, NUMBER, NUMBER);
        addExpressionSnippet(context, snippets, "<=", "[ :x :y | x <= y ]", BOOLEAN, NUMBER, NUMBER);
        addExpressionSnippet(context, snippets, "=", "[ :x :y | x = y ]", BOOLEAN, ANY, ANY);
        addExpressionSnippet(context, snippets, ">", "[ :x :y | x > y ]", BOOLEAN, NUMBER, NUMBER);
        addExpressionSnippet(context, snippets, ">=", "[ :x :y | x >= y ]", BOOLEAN, NUMBER, NUMBER);

        addExpressionSnippet(context, snippets, "[ = ]", "[ [ :x :y | x = y ] ]", TypeDescriptor.executable(ANY, ANY, ANY));

        return snippets;
    }

    @Override
    public Collection<? extends Snippet> createStatements(final Context context) {
        final Collection<Snippet> statements = new ArrayList<>();
        addStatementSnippet(context, statements, "IdentityBlock", "[ :p | p ]", ANY, ANY);
        addStatementSnippet(context, statements, "class", "[ :p | p class ]", OBJECT, ANY);
        addStatementSnippet(context, statements, "basicSize", "[ :p | p basicSize ]", NUMBER, ANY);
        addStatementSnippet(context, statements, "hash", "[ :p | p hash ]", NUMBER, ANY);
        addStatementSnippet(context, statements, "isNil", "[ :p | p isNil ]", BOOLEAN, ANY);
        addStatementSnippet(context, statements, "notNil", "[ :p | p notNil ]", BOOLEAN, ANY);
        addStatementSnippet(context, statements, "printString", "[ :p | p printString ]", STRING, ANY);

        addStatementSnippet(context, statements, "ifTrue:", "[ :p | | x | x := false. p ifTrue: [ true ] ]", ANY, BOOLEAN);
        addStatementSnippet(context, statements, "ifTrue:ifFalse:", "[ :p | p ifTrue: [ true ] ifFalse: [ false ] ]", BOOLEAN, BOOLEAN);
        addStatementSnippet(context, statements, "ifFalse:", "[ :p | p ifFalse: [ true ] ]", ANY, BOOLEAN);
        addStatementSnippet(context, statements, "ifFalse:ifTrue:", "[ :p | p ifFalse: [ -1 ] ifTrue: [ 1 ] ]", NUMBER, BOOLEAN);

        addStatementSnippet(context, statements, "and:", "[ :p1 :p2 | p1 and: p2 ]", ANY, BOOLEAN, ANY);
        addStatementSnippet(context, statements, "or:", "[ :p1 :p2 | p1 or: p2 ]", ANY, BOOLEAN, ANY);

        addStatementSnippet(context, statements, "ifNil:", "[ :p | | x | x := false. p ifNil: [ x := true ]. x ]", BOOLEAN, ANY);
        addStatementSnippet(context, statements, "ifNil:ifNotNil:", "[ :p | p ifNil: [ true ] ifNotNil: [ false ] ]", BOOLEAN, ANY);
        addStatementSnippet(context, statements, "ifNotNil:", "[ :p | | x | x := -1. p ifNotNil: [ x := 1 ]. x ]", NUMBER, ANY);
        addStatementSnippet(context, statements, "ifNotNil:ifNil:", "[ :p | p ifNotNil: [ 1 ] ifNil: [ -1 ] ]", NUMBER, ANY);

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
}
