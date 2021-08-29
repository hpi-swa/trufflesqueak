"
I am a TextStyler. I use polyglot-ruby-rouge to highlight code and add helpful bobbles for python code.
"
Class {
	#name : #PETextStyler,
	#superclass : #SHTextStyler,
	#instVars : [
		'languageInfo'
	],
	#category : 'TruffleSqueak-Tools-Editor-Core'
}

{ #category : #accessing }
PETextStyler >> changeLanguage: id [

	self languageInfo: (PELanguageInfo forId: id)
]

{ #category : #private }
PETextStyler >> createAnnotationMorph: aString [

	^ Text
		string: (String value: 1) 
		attribute: (TextAnchor new anchoredMorph: (CircleMorph new
			changeTableLayout;
			hResizing: #rigid;
			vResizing: #rigid;
			extent: 10@10;
			color: Color red;
			balloonText: aString))
]

{ #category : #private }
PETextStyler >> createSemicolonAnnotationMorph [

	^ self createAnnotationMorph: 'There are usually no semicolons in python code'
]

{ #category : #private }
PETextStyler >> createWrongForAnnotationMorph [

	^ self createAnnotationMorph: 'A for-loop in python does not need {}'
]

{ #category : #accessing }
PETextStyler >> languageInfo [

	^ languageInfo
]

{ #category : #accessing }
PETextStyler >> languageInfo: aDictionary [

	languageInfo := aDictionary
]

{ #category : #private }
PETextStyler >> privateAddPythonHelp: aText [
	
	| styledText textString offset |
	self languageInfo ifNil: [^ aText].
	self languageInfo isPython ifFalse: [^ aText].

	self removePreviousAnnotationMorphs: aText.

	textString := aText asString.
	styledText := aText.
	offset := 0.
	textString doWithIndex: [:each :index |
		each = $;
			ifTrue: [
				styledText := styledText
					replaceFrom: index + offset
					to: index +offset
					with: (Text withAll: ';') , self createSemicolonAnnotationMorph.
				offset := offset + 1]].
		
	textString := styledText asString.
	offset := 0.
	textString doWithIndex: [:each :index |
		each = ${
			ifTrue: [
				styledText := styledText
					replaceFrom: index + offset
					to: index +offset
					with: (Text withAll: '{') , self createWrongForAnnotationMorph.
				offset := offset + 1]].
	
	^ styledText
]

{ #category : #private }
PETextStyler >> privateFormat: aText [

	^ self privateAddPythonHelp: aText
]

{ #category : #private }
PETextStyler >> privateStyle: aText [
	
	^ self privateStyleHighlighting: aText
]

{ #category : #private }
PETextStyler >> privateStyleHighlighting: aText [

	| tokens polyglot |
	self languageInfo ifNil: [^ aText].
	polyglot := Smalltalk at: #Polyglot ifAbsent: [^ aText].
	polyglot primitiveIsPolyglotBindingsAccessAllowed ifFalse: [^ aText].
	
	polyglot export: 'polyglot-editor:code' value: aText asString.
	polyglot export: 'polyglot-editor:lexerName' value: self languageInfo rougeLexerName.
	
	aText 
		removeAttributesThat: [:attribute | attribute class = TextColor] 
		replaceAttributesThat: [:attribute | false] 
		by: [].
	tokens := ([polyglot eval: polyglot languageIdRuby string: self rubyHighlightCode] ifError: [^ aText]) asCollection.
	tokens do: [:ea | | each |
		each := ea asCollection. 
		aText 
			addAttribute: (self toTextColor: each third asString)
			from: each first 
			to: each second - 1].
	
	^ aText
]

{ #category : #private }
PETextStyler >> removePreviousAnnotationMorphs: aText [

	| offset cleanedText |
	cleanedText := aText.
	offset := 0.
	aText runs withStartStopAndValueDo: [:start :stop :attributes | attributes
		select: [:each | each isKindOf: TextAnchor]
		thenDo: [:each | (each anchoredMorph model isKindOf: PECodeBox) ifFalse: [
				cleanedText := cleanedText replaceFrom: start + offset to: stop + offset with: ''.
				offset := offset - 1.]]]
]

{ #category : #accessing }
PETextStyler >> reset [
]

{ #category : #private }
PETextStyler >> rubyHighlightCode [

	^ '
require "rouge"
source = Polyglot.import("polyglot-editor:code")
lexerName = Polyglot.import("polyglot-editor:lexerName")

lexer = Rouge::Lexer.find(lexerName).new
theme = Rouge::Theme.find("base16.solarized")

pos = 1
lexer.lex(source.to_s).map { |token, text| 
	style = theme.get_style(token)
	[pos, pos += text.size, style.fg()]
}.reject { | triple | triple[2].nil? }'
]

{ #category : #private }
PETextStyler >> toTextColor: aString [

	| hex |
	hex := aString.
	hex size = 4 ifTrue: [
		hex := '#', (hex at: 2), (hex at: 2), (hex at: 3), (hex at: 3), (hex at: 4), (hex at: 4)].
	^ TextColor color: (Color fromString: hex) 
]

{ #category : #private }
PETextStyler >> unstyledTextFrom: aText [
	"Re-implemented so that TextAnchors are not removed from aText"
	| answer |	
	answer := super unstyledTextFrom: aText.
	"answer := answer copyReplaceAll: (String value: 1) with: ''."
	aText runs withStartStopAndValueDo: [:start :stop :attributes | attributes
		select: [:each | each isKindOf: TextAnchor] 
		thenDo: [:each | answer addAttribute: each from: start to: stop]].
	^answer
]
