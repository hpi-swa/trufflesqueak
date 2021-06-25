rouge
newLexerFor: languageID
	^ Polyglot eval: #ruby string: ('Rouge::Lexers::{1}.new' format: {self lexerNameFor: languageID})