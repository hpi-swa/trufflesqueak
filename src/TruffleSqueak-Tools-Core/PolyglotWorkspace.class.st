Class {
	#name : #PolyglotWorkspace,
	#superclass : #Workspace,
	#instVars : [
		'languageInfo'
	],
	#category : 'TruffleSqueak-Tools-Core'
}

{ #category : #'class initialization' }
PolyglotWorkspace class >> initialize [
	"self initialize"
	super initialize.
	self registerInWorldMenu
]

{ #category : #overrides }
PolyglotWorkspace class >> open [
	^ self new open
]

{ #category : #'world menu' }
PolyglotWorkspace class >> registerInWorldMenu [
	self environment at: #TheWorldMenu ifPresent: [ :class |
		class registerOpenCommand: (Array 
			with: 'Polyglot Workspace' 
			with: (Array
				with: self
				with: #open)) ].
]

{ #category : #preferences }
PolyglotWorkspace class >> shouldStyle [
	^ true
]

{ #category : #'code pane menu' }
PolyglotWorkspace >> addCustomCodePaneMenuItems: aMenu shifted: aBoolean [
	<codePaneMenu>
	<menuPriority: 400>
	
	aMenu
		add: 'select language... (L)'
			target: self
			selector: #selectLanguage;
		add: 'set default language... (h)'
			target: self
			selector: #setDefaultLanguage;
		add: 'set syntax highlighter... (h)'
			target: self
			selector: #setSyntaxHighlighter.

	Smalltalk at: #PolyglotEditor ifPresent: [ :unused |
		aMenu addLine.
		aMenu
			add: 'open in Polyglot Editor'
			target: self
			selector: #openInPolyglotEditor ].

	^ aMenu
]

{ #category : #'user interface' }
PolyglotWorkspace >> addModelItemsToWindowMenu: aMenu [
	aMenu addLine.
	Polyglot availableLanguages do: [:ea |
		aMenu
			add: 'select ', ea getName ,'...'
			target: self
			selector: #languageInfo:
			argument: ea ].
	aMenu addLine.

	Smalltalk at: #PolyglotEditor ifPresent: [ :unused |
		aMenu
			add: 'open in Polyglot Editor'
			target: self
			selector: #openInPolyglotEditor.
		aMenu addLine ].

	super addModelItemsToWindowMenu: aMenu.
	
	aMenu add: 'select syntax highlighter'
			target: Polyglot
			selector: #askForRougeStyler.
]

{ #category : #toolbuilder }
PolyglotWorkspace >> buildCodePaneWith: builder [
	^ (super buildCodePaneWith: builder)
		stylerClass: PolyglotTextStyler;
		help: #helpText;
		yourself
]

{ #category : #evaluation }
PolyglotWorkspace >> evaluateExpression: selection [
	^ Polyglot isPolyglotEvalAllowed
		ifTrue: [ Polyglot
					evalWithBindings: self languageInfo getId asString
					string: selection asString ]
		ifFalse: [ Compiler evaluate: selection ]
]

{ #category : #accessing }
PolyglotWorkspace >> helpText [
	^ 'Evaluate {1} expressions here. Switch the language in the window menu.'
		format: { self languageInfo getName }
]

{ #category : #'initialize-release' }
PolyglotWorkspace >> initialize [
	super initialize.
	languageInfo := Polyglot defaultLanguageInfo
]

{ #category : #binding }
PolyglotWorkspace >> initializeBindings [
	"pretend bindings is already available"
	bindings := Dictionary new at: 'bindings' put: nil; yourself
]

{ #category : #toolbuilder }
PolyglotWorkspace >> labelString [
	^ self languageInfo getName, ' Workspace'
]

{ #category : #language }
PolyglotWorkspace >> languageId [
	^ self languageInfo getId asSymbol
]

{ #category : #language }
PolyglotWorkspace >> languageInfo [
	^ languageInfo
]

{ #category : #language }
PolyglotWorkspace >> languageInfo: aLanguageInfo [
	languageInfo == aLanguageInfo ifTrue: [ ^ self ].
	languageInfo := aLanguageInfo.
	self changed: #style.
	self changed: #relabel
]

{ #category : #'initialize-release' }
PolyglotWorkspace >> open [
	^ self openLabel: self labelString
]

{ #category : #language }
PolyglotWorkspace >> selectLanguage [
	self languageInfo: Polyglot chooseLanguageInfo
]

{ #category : #language }
PolyglotWorkspace >> setDefaultLanguage [
	Polyglot askForDefaultLanguageInfo.
	self languageInfo: Polyglot defaultLanguageInfo
]

{ #category : #'code pane menu' }
PolyglotWorkspace >> setSyntaxHighlighter [
	Polyglot askForRougeStyler.
	self changed: #style
]
