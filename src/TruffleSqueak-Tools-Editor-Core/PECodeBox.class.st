"
I am a codebox in a Polyglot Code Editor.
Visibly, I consists of imported variables, code, exported variables and a selected language (to whose color I change).
Internally, my code is written in special snippet file and evaluated as a file.
"
Class {
	#name : #PECodeBox,
	#superclass : #Model,
	#instVars : [
		'boxName',
		'importVariables',
		'exportVariables',
		'language',
		'code',
		'textStyler',
		'collapsed',
		'editArea',
		'selection',
		'fileEntry'
	],
	#category : 'TruffleSqueak-Tools-Editor-Core'
}

{ #category : #building }
PECodeBox >> addCodeBoxAtSelection: aPCCodeBox [

	| codeBoxText |
	codeBoxText := 
		self innerLanguageInfo defaultLineEndings asText, 
		(Text
			string: (String value: 1) 
			attribute: (TextAnchor new anchoredMorph: (ToolBuilder build: aPCCodeBox))), 
		self innerLanguageInfo defaultLineEndings.
	self replaceSelectionWith: codeBoxText.
	self triggerEvent: #codeBoxAdded with: aPCCodeBox
]

{ #category : #accessing }
PECodeBox >> boxName [

	^ boxName
]

{ #category : #accessing }
PECodeBox >> boxName: aText [

	boxName := aText asString.
	self changed: #boxName.
	self triggerEvent: #codeBoxChanged
]

{ #category : #building }
PECodeBox >> buildBodyWith: aBuilder [

	^ aBuilder pluggablePanelSpec new
		layout: #vertical;
		verticalResizing: #shrinkWrap;
		children: {self buildImportRowWith: aBuilder.
			self buildCodeAreaWith: aBuilder.
			self buildExportRowWith: aBuilder};
		yourself
]

{ #category : #accessing }
PECodeBox >> buildChildren [

	| builder |
	builder := ToolBuilder default.
	^ self collapsed
	ifTrue: [{(self buildHeaderWith: builder) buildWith: builder}]
	ifFalse: [{(self buildBodyWith: builder) buildWith: builder.
		(self buildHeaderWith: builder) buildWith: builder}]
]

{ #category : #building }
PECodeBox >> buildCodeAreaWith: aBuilder [

	| editArea |
	editArea := PEPluggableCodeSpec new
		model: self;
		autoHeight: true;
		setText: #code:;
		getText: #code;
		editText: #code:;
		setSelection: #privateSelection:;
		selection: #selection;
		styler: self textStyler;
		yourself.
	self editArea: editArea.
	^ editArea
]

{ #category : #building }
PECodeBox >> buildCollapseButtonWith: aBuilder [

	^{aBuilder pluggableButtonSpec new
		model: self;
		label: '^';
		action: #collapseCodeBox;
		verticalResizing: #spaceFill;
		horizontalResizing: #rigid;
		yourself}
]

{ #category : #building }
PECodeBox >> buildExportRowWith: aBuilder [

	^ aBuilder pluggablePanelSpec new
		layout: #horizontal;
		verticalResizing: #shrinkWrap;
		horizontalResizing: #spaceFill;
		minimumHeight: TextStyle defaultFont height + 2;
		children: {
			PEPluggableLabelSpec new
				label: 'Export: ';
				yourself.
			aBuilder pluggableInputFieldSpec new
				model: self;
				setText: #exportVariablesFromInput:;
				getText: #exportVariablesForInput;
				editText: #exportVariablesFromInput:;
				indicateUnacceptedChanges: false;
				yourself};
		yourself
]

{ #category : #building }
PECodeBox >> buildHeaderWith: aBuilder [

	^ aBuilder pluggablePanelSpec new
		layout: #horizontal;
		"verticalResizing: #shrinkWrap;"
		horizontalResizing: #spaceFill;
		minimumHeight: TextStyle defaultFont height + 2;
		children: ((self buildNameRowWith: aBuilder), 
			(self buildLanguageButtonWith: aBuilder),
			(self buildCollapseButtonWith: aBuilder));
		yourself
]

{ #category : #building }
PECodeBox >> buildImportRowWith: aBuilder [

	^ aBuilder pluggablePanelSpec new
		layout: #horizontal;
		verticalResizing: #shrinkWrap;
		horizontalResizing: #spaceFill;
		minimumHeight: TextStyle defaultFont height + 2;
		children: {
			PEPluggableLabelSpec new
				label: 'Imports: ';
				yourself.
			aBuilder pluggableInputFieldSpec new
				model: self;
				setText: #importVariablesFromInput:;
				getText: #importVariablesForInput;
				editText: #importVariablesFromInput:;
				indicateUnacceptedChanges: false;
				yourself.
			aBuilder pluggableButtonSpec new
				model: self;
				label: 'Code Box';
				action: #nestedCodeBox;
				verticalResizing: #spaceFill;
				horizontalResizing: #rigid;
				yourself};
		yourself
]

{ #category : #building }
PECodeBox >> buildLanguageButtonWith: aBuilder [

	^{aBuilder pluggableButtonSpec new
		model: self;
		label: #languageButtonLabel;
		action: #chooseLanguage;
		verticalResizing: #spaceFill;
		yourself}
]

{ #category : #building }
PECodeBox >> buildNameRowWith: aBuilder [

	^ {PEPluggableLabelSpec new
			label: 'Name: ';
			yourself.
		aBuilder pluggableInputFieldSpec new
			model: self;
			setText: #boxName:;
			getText: #boxName;
			editText: #boxName:;
			yourself}
]

{ #category : #building }
PECodeBox >> buildWith: aBuilder [

	self collapsed: false.	
	^ aBuilder build: (PEPluggablePanelSpec new
		layout: #vertical;
		horizontalResizing: #rigid;
		verticalResizing: #shrinkWrap;
		color: #color;
		model: self;
		padding: 3;
		spacing: 3;
		children: #buildChildren;
		yourself)
]

{ #category : #'UI functions' }
PECodeBox >> chooseLanguage [

	| choice languageInfos |
	languageInfos := PELanguageInfo allWithPolyglotSupport.
	choice := UIManager default 
		chooseFrom: (languageInfos collect: #languageName) 
		values: (languageInfos collect: #polyglotId).
	choice ifNil: [^ self].
	self language: choice
]

{ #category : #accessing }
PECodeBox >> code [

	^ code
]

{ #category : #accessing }
PECodeBox >> code: aText [

	code := aText.
	self changed: #code.
	self triggerEvent: #codeBoxChanged
]

{ #category : #accessing }
PECodeBox >> collapseCodeBox [

	"| snippets snippetFile |"
	self collapsed: self collapsed not.
	"snippets := self fileEntry parent children detect: [:each | each name = 'snippets'].
	snippetFile := (snippets children detect: [:each | each name = self fileName])."
	"self code: snippetFile read"
	"self code: (self addCodeBoxes: self fileEntry read)"
	
]

{ #category : #accessing }
PECodeBox >> collapsed [

	^ collapsed
]

{ #category : #accessing }
PECodeBox >> collapsed: aBoolean [

	collapsed := aBoolean.
	self changed: #buildChildren
]

{ #category : #accessing }
PECodeBox >> color [

	^ self language 
		ifNotNil: [self innerLanguageInfo color]
		ifNil: [self windowColorToUse]
]

{ #category : #accessing }
PECodeBox >> editArea [

	^ editArea
]

{ #category : #accessing }
PECodeBox >> editArea: aCodeMorph [

	editArea := aCodeMorph
]

{ #category : #'code-generation' }
PECodeBox >> ensurePolyglotIsImported [

	| header |
	header := self innerLanguageInfo codeForPolyglotHeader.
	(self unsavedCode beginsWith: header) ifFalse: [ | str |
		str := header, self lineEndings.
		self unsavedCode prepend: str.
		self changed: #unsavedCode.
		self selection: self selection + str size]
]

{ #category : #accessing }
PECodeBox >> exportVariables [

	^ exportVariables
]

{ #category : #accessing }
PECodeBox >> exportVariables: anObject [

	exportVariables := anObject.
	self changed: #exportVariablesForInput.
	self triggerEvent: #codeBoxChanged
]

{ #category : #accessing }
PECodeBox >> exportVariablesForInput [

	^ self exportVariables joinSeparatedBy: ', '
]

{ #category : #accessing }
PECodeBox >> exportVariablesFromInput: aText [

	| newExportVariables |
	newExportVariables := (aText asString findBetweenSubStrs: ', ').
	newExportVariables ~= self exportVariables ifTrue: [self exportVariables: newExportVariables]
]

{ #category : #accessing }
PECodeBox >> fileEntry [

	"The file displayed inside this code box. Can be nil for new code boxes"
	^ fileEntry
]

{ #category : #accessing }
PECodeBox >> fileEntry: aPCFileEntry [

	fileEntry := aPCFileEntry
]

{ #category : #'code-generation' }
PECodeBox >> fileName [

	"If this is a new code box, generate a file name based on the box name. If it is an old code box, use the existing file name"
	^ self fileEntry ifNotNil: [self fileEntry name] ifNil: ['{1}.{2}' format: {
		(FileDirectory checkName: self boxName fixErrors: true).
		self innerLanguageInfo defaultExtension}]
]

{ #category : #accessing }
PECodeBox >> importVariables [

	^ importVariables
]

{ #category : #accessing }
PECodeBox >> importVariables: anObject [

	importVariables := anObject.
	self changed: #importVariablesForInput.
	self triggerEvent: #codeBoxChanged
]

{ #category : #accessing }
PECodeBox >> importVariablesForInput [

	^ self importVariables joinSeparatedBy: ', '
]

{ #category : #accessing }
PECodeBox >> importVariablesFromInput: aText [

	| newImportVariables |
	newImportVariables := (aText asString findBetweenSubStrs: ', ').
	newImportVariables ~= self importVariables ifTrue: [self importVariables: newImportVariables]
]

{ #category : #initialization }
PECodeBox >> initialize [

	super initialize.
	
	self
		textStyler: PETextStyler new;
		language: nil;
		boxName: '';
		code: '';
		importVariables: {};
		exportVariables: {}
]

{ #category : #accessing }
PECodeBox >> innerLanguageInfo [

	^ PELanguageInfo forId: self language
]

{ #category : #building }
PECodeBox >> insertCodeBox: aPCCodeBox [

	self addCodeBoxAtSelection: aPCCodeBox
]

{ #category : #accessing }
PECodeBox >> language [

	^ language
]

{ #category : #accessing }
PECodeBox >> language: aSymbol [

	language := aSymbol.	
	language ifNotNil: [
		self code: (PELineEndingManager 
			with: self innerLanguageInfo defaultLineEndings string: self code).
		self textStyler languageInfo: (PELanguageInfo forId: aSymbol).
		(self innerLanguageInfo isSmalltalk and: [PolyglotEditor smalltalkCodeBoxWarningShown not]) ifTrue: [
			PolyglotEditor showSmalltalkCodeBoxWarning]].
	self changed: #languageButtonLabel.
	self changed: #color.
	self triggerEvent: #codeBoxChanged
]

{ #category : #accessing }
PECodeBox >> languageButtonLabel [

	^  self language ifNotNil: [self innerLanguageInfo languageName] ifNil: ['Choose Language']
]

{ #category : #building }
PECodeBox >> nestedCodeBox [
	
	| codeBox |
	self language ifNil: [self error: 'You need to select a language first!'] ifNotNil: [
		codeBox := PECodeBox new.
		self insertCodeBox: codeBox]

	"| codeBox file |
	self editArea fileEntry parent children
		detect: [:each | each name = 'snippets']
		ifFound: [:snippets |
			file := UIManager default 
				chooseFrom: {'new'}, (snippets children collect: #name)
				values: {#new}, snippets children.
			file ifNil: [^ self].
			codeBox := file = #new
				ifTrue: [PCCodeBox new]
				ifFalse: [PCCodeBoxReadWriter codeBoxWithFileName: file name
					referencedIn: self activeEditArea fileEntry]]
		ifNone: [codeBox := PCCodeBox new].
	
	self insertCodeBox: codeBox"
	
]

{ #category : #accessing }
PECodeBox >> privateSelection: anInterval. [

	"This is only used by ToolBuilder. If we were to use >>selection:, we'd cause an infinite loop"
	selection := anInterval
]

{ #category : #accessing }
PECodeBox >> replaceSelectionWith: aText [

	self code: (self code asText
		copyReplaceFrom: self selection start 
		to: self selection stop 
		with: aText).
	self selection: (self selection start to: (self selection stop + aText size))
]

{ #category : #accessing }
PECodeBox >> selection [

	^ selection
]

{ #category : #accessing }
PECodeBox >> selection: anInterval [

	self privateSelection: anInterval.
	self changed: #selection
]

{ #category : #accessing }
PECodeBox >> textStyler [

	^ textStyler
]

{ #category : #accessing }
PECodeBox >> textStyler: anil [

	textStyler := anil
]
