Class {
	#name : #RougeStylerEnabled,
	#superclass : #RougeStyler,
	#instVars : [
		'formatter',
		'lexers',
		'rubyUnbox'
	],
	#category : #'TruffleSqueak-Support'
}

{ #category : #private }
RougeStylerEnabled >> initialize [
	super initialize.
	[ {[ Polyglot eval: #ruby string: 'require "rouge"' ]} do: [ :block | block value ]
			displayingProgress: [ :block | 'Initializing RougeStyler in Ruby...' ]
			every: 100 ] on: Error do: [ :e |
				self error: 'Failed to load rouge gem. Please make sure it is installed (via `gem install rouge`)'.
				^ RougeStylerDisabled basicNew initialize ].
	formatter := Polyglot eval: #ruby string: 'Rouge::Formatters::HTMLInline.new(Rouge::Themes::IgorPro.new)'.
	rubyUnbox := Polyglot eval: #ruby string: 'require "rouge"; Truffle::Interop.method(:unbox)'
]

{ #category : #rouge }
RougeStylerEnabled >> lexerFor: languageID [
	| lexer |
	"Lexers are Ruby objects and are therefore replaced with nil when the image is saved."
	lexer := (lexers ifNil: [ lexers := Dictionary new]) at: languageID ifAbsent: [ nil ].
	lexer ifNil: [ lexer := lexers at: languageID put: (self newLexerFor: languageID) ].
	^ lexer
]

{ #category : #rouge }
RougeStylerEnabled >> lexerNameFor: languageID [
	languageID = Polyglot languageIdJS ifTrue: [ ^ 'Javascript' ].
	languageID = Polyglot languageIdRuby ifTrue: [ ^ 'Ruby' ].
	languageID = Polyglot languageIdPython ifTrue: [ ^ 'Python' ].
	languageID = Polyglot languageIdR ifTrue: [ ^ 'R' ].
	languageID = Polyglot languageIdSmalltalk ifTrue: [ ^ 'Smalltalk' ].
	^ 'Markdown'
]

{ #category : #rouge }
RougeStylerEnabled >> newLexerFor: languageID [
	^ Polyglot eval: #ruby string: ('Rouge::Lexers::{1}.new' format: {self lexerNameFor: languageID})
]

{ #category : #'public-api' }
RougeStylerEnabled >> style: aText language: aLanguageID [
	| rubyString newText |
	rubyString := rubyUnbox value: aText asString withUnixLineEndings.
	newText := ((formatter format: ((self lexerFor: aLanguageID) lex: rubyString))
		asString copyReplaceAll: String lf with: '<br/>') asTextFromHtml.
	aText setString: newText string setRuns: newText runs
]

{ #category : #'public-api' }
RougeStylerEnabled >> style: aText language: aLanguageID ifFail: aBlock [
	self style: aText language: aLanguageID
]
