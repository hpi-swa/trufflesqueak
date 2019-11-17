# Polyglot API Reference

GraalSqueak provides access to all languages supported by GraalVM through its
Polyglot API.
This document describes how this API can be used to interact with different
languages from Smalltalk.

## Evaluating code written in foreign languages

```smalltalk
Polyglot eval: 'id' file: 'path'.    "Evaluates a file with code of a language, identified by its ID."
Polyglot eval: 'id' string: 'code'.  "Evaluates code of a language, identified by its ID."
```

## Sharing objects between languages

```smalltalk
Polyglot import: 'name'.                "Imports and returns a value with a given name"
Polyglot export: 'name' value: aValue.  "Exports a value with a given name"
Polyglot bindings.                      "Returns the polyglot bindings object"
```

## Accessing language information

```smalltalk
Polyglot availableLanguages.               "Returns a list of supported language IDs"
Polyglot languageDefaultMimeTypeOf: 'id'.  "Returns the default mime type for a given language ID"
Polyglot languageMimeTypesOf: 'id'.        "Returns all mime types for a given language ID"
Polyglot languageNameOf: 'id'.             "Returns the language name for a given language ID"
Polyglot languageVersionOf: 'id'.          "Returns the language version for a given language ID"
```

## Accessing Java

```smalltalk
Java type: 'name'.                        "Looks up and returns a Java class object"

"Examples"
(Java type: 'int[]') new: 2.              "Equivalent to `new int[2]`"
(Java type: 'java.lang.System') exit: 0.  "Equivalent to `System.exit(0)`"
```

## Internal API

[`Polyglot`][polyglot_class] exposes additional methods for internal use that
provide further functionality. It is not recommended to rely on them as they may
change without notice.
All foreign objects are represented by the
[`TruffleObject`][truffle_object_class] class.
The [`Interop`][interop_class] class exposes the underlying [Truffle API for
language interoperability][truffle_interop_library].

*Please note that the Polyglot API may change with newer releases of
GraalSqueak.*

[interop_class]: https://github.com/hpi-swa/graalsqueak/tree/image/src/GraalSqueak-Core.package/Interop.class
[polyglot_class]: https://github.com/hpi-swa/graalsqueak/tree/image/src/GraalSqueak-Core.package/Polyglot.class
[truffle_interop_library]: https://www.graalvm.org/truffle/javadoc/com/oracle/truffle/api/interop/InteropLibrary.html
[truffle_object_class]: https://github.com/hpi-swa/graalsqueak/tree/image/src/GraalSqueak-Core.package/TruffleObject.class
