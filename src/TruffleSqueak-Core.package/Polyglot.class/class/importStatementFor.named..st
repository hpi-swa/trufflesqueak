helpers
importStatementFor: aLanguage named: aName
	"LanguageIDToPolyglotImport := nil"
	^ ((LanguageIDToPolyglotImport ifNil: [
		LanguageIDToPolyglotImport := Dictionary newFromPairs: {
			self languageIdJS. [:name | '{1} = Polyglot.import("bindings");' format: {name}].
			self languageIdLLVM. [:name | '{1} = polyglot_import("bindings")' format: {name}].
			self languageIdPython. [:name | 'import polyglot; {1} = polyglot.import_value("bindings")' format: {name}].
			self languageIdR. [:name | '{1} <- import("bindings")' format: {name}].
			self languageIdRuby. [:name | '{1} = Polyglot.import("bindings")' format: {name}].
			self languageIdSmalltalk. [:name | '{1} := Polyglot import: ''bindings''.' format: {name}]}
		]) at: aLanguage ifAbsent: [self error: 'unsupported language: ', aLanguage]) value: aName