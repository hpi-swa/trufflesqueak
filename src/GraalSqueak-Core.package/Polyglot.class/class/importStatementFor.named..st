helpers
importStatementFor: aLanguage named: aName
	"LanguageIDToPolyglotImport := nil"
	^ ((LanguageIDToPolyglotImport ifNil: [
		LanguageIDToPolyglotImport := Dictionary newFromPairs: {
			'js'. [:name | '{1} = Polyglot.import("bindings"); ' format: {name}].
			'llvm'. [:name | '{1} = polyglot_import("bindings"); ' format: {name}].
			'python'. [:name | 'import polyglot; {1} = polyglot.import_value("bindings"); ' format: {name}].
			'R'. [:name | '{1} <- import("bindings"); ' format: {name}].
			'ruby'. [:name | '${1} = Polyglot.import("bindings"); ' format: {name}].
			'squeaksmalltalk'. [:name | '{1} := Polyglot import: ''bindings''). ' format: name]}
		]) at: aLanguage ifAbsent: [^ nil]) value: aName