as yet unclassified
lexerFor: languageID
	^ (lexers ifNil: [ lexers := Dictionary new]) at: languageID ifAbsentPut: [
		Polyglot eval: 'ruby' string: ('Rouge::Lexers::{1}.new' format: {self lexerNameFor: languageID})]