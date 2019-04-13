as yet unclassified
styleold: aText language: aLanguageID
	"formatter ifNil: [ formatter := true. Polyglot eval: 'ruby' string: 'require ''rouge''']."
	aLanguageID = 'squeaksmalltalk'
		ifTrue: [ ^ SHTextStylerST80 new styledTextFor: aText ].
	^ (((Polyglot eval: 'ruby' string: ('
formatter = Rouge::Formatters::HTMLInline.new(Rouge::Themes::IgorPro.new)
lexer = Rouge::Lexers::{1}.new
formatter.format(lexer.lex("{2}"))' format: {self lexerNameFor: aLanguageID. aText asString copyReplaceAll: '"' with: '\"'})) asString) copyReplaceAll: String lf with: '<br/>') asTextFromHtml