Class {
	#name : #PEEditAreaText,
	#superclass : #PEEditAreaAbstract,
	#instVars : [
		'textStyler',
		'lineEndings',
		'lineEndingsSymbol',
		'code',
		'unsavedCode',
		'selection',
		'languageInfo'
	],
	#category : 'TruffleSqueak-Tools-Editor-Core'
}

{ #category : #public }
PEEditAreaText class >> supports: aFileEntry [

	| mimeType |
	mimeType := MIMEDocument guessTypeFromExtension: aFileEntry extension.
	(mimeType beginsWith: 'text/') ifTrue: [^ 10].
	^ [aFileEntry read. 3] ifError: [0]
]

{ #category : #'code actions' }
PEEditAreaText >> addCodeBoxAtSelection: aPCCodeBox [

	| codeBoxText |
	codeBoxText := 
		self lineEndings asText,
		(Text
			string: (String value: 1) 
			attribute: (TextAnchor new anchoredMorph: (ToolBuilder build: aPCCodeBox))), 
		self lineEndings.
	self replaceSelectionWith: codeBoxText.
	self setupCodeBoxEventsFor: aPCCodeBox
]

{ #category : #'code actions' }
PEEditAreaText >> addCodeBoxes: aString [

	| result |
	result := PECodeBoxReadWriter addCodeBoxesTo: aString file: self fileEntry.
	result second do: [:each | self setupCodeBoxEventsFor: each].
	^ result first
]

{ #category : #building }
PEEditAreaText >> buildChildrenWith: aBuilder [

	^ {PEPluggableCodeSpec new
		model: self;
		setText: #codeFromEdit:;
		getText: #code;
		setEditText: #unsavedCode;
		editText: #unsavedCode:;
		setSelection: #privateSelection:;
		setLineEndings: #lineEndings;
		selection: #selection;
		onFocus: #triggerFocusEvent;
		onKeyStroke: #keyStroke:;
		styler: self textStyler;
		frame: (0@0 extent: 1@1);
		yourself}
]

{ #category : #accessing }
PEEditAreaText >> code [

	^ code
]

{ #category : #accessing }
PEEditAreaText >> code: aText [

	| text |
	text := aText asText.
	code := text.
	unsavedCode := text.
	self changed: #code.
	self extractLineEndings
]

{ #category : #accessing }
PEEditAreaText >> codeFromEdit: aText [

	self code: aText.
	self save
]

{ #category : #accessing }
PEEditAreaText >> codeMorph [

	^ self myDependents first findDeepSubmorphThat: [:m | m class = PEPluggableCodeMorph] ifAbsent: [self error: 'no code morph'].
]

{ #category : #public }
PEEditAreaText >> convertLineEndings: newLineEndings [

	| selection lineEndingsBeforeSelection lineEndingsInSelection sizeDifference |
	lineEndingsBeforeSelection := 0.
	lineEndingsInSelection := 0.
	self unsavedCode asString lineIndicesDo: [:start :end :endWithLineEnding |
		(endWithLineEnding < self selection start and: endWithLineEnding < self selection stop)
			ifTrue: [lineEndingsBeforeSelection := lineEndingsBeforeSelection + 1].
		(self selection includes: end + 1)
			ifTrue: [lineEndingsInSelection := lineEndingsInSelection + 1]].
	
	sizeDifference := newLineEndings size - self lineEndings size.

	selection := self selection.
	selection := (selection start + (sizeDifference * lineEndingsBeforeSelection)) 
		to: selection stop + (sizeDifference * (lineEndingsBeforeSelection + lineEndingsInSelection)).
	self unsavedCode: (PELineEndingManager with: newLineEndings string: self unsavedCode).
	self changed: #unsavedCode.
	self selection: selection.
]

{ #category : #public }
PEEditAreaText >> ensurePolyglotIsImported [

	| header |
	header := self languageInfo codeForPolyglotHeader.
	(self unsavedCode beginsWith: header) ifFalse: [ | str |
		str := header, self lineEndings.
		self unsavedCode prepend: str.
		self changed: #unsavedCode.
		self selection: self selection + str size]
]

{ #category : #private }
PEEditAreaText >> evaluateExpression: selection [

	| polyglot |
	polyglot := Smalltalk classNamed: #Polyglot.
	^ (polyglot notNil and: [polyglot isPolyglotEvalAllowed])
		ifTrue: [
			[PolyglotEditor evaluateInInnerContext
				ifTrue: [ polyglot evalInInnerContext: self languageInfo polyglotId string: selection asString ]
				ifFalse: [ polyglot eval: self languageInfo polyglotId string: selection asString ]] ifError: [:error |
			PolyglotEditor error: error withLanguage: self languageInfo languageName]]
		ifFalse: [Compiler evaluate: selection asString]
]

{ #category : #'code actions' }
PEEditAreaText >> extractLineEndings [

	lineEndingsSymbol := PELineEndingManager detectLineEndings: self unsavedCode.
	lineEndingsSymbol = #unknown ifTrue: [lineEndingsSymbol := self languageInfo 
		ifNotNil: [:info | PELineEndingManager stringToId: info defaultLineEndings] 
		ifNil: [#lf]].
	(#(cr lf crlf) includes: lineEndingsSymbol)
		ifTrue: [self lineEndings: (String perform: lineEndingsSymbol)].
	
	self changed: #lineEndings.
	self triggerEvent: #lineEndingsChanged
]

{ #category : #'initialize-release' }
PEEditAreaText >> initialize [

	super initialize.
	self
		code: '-- no file selected --';
		lineEndings: String lf;
		textStyler: PETextStyler new.
	lineEndingsSymbol := #lf
]

{ #category : #public }
PEEditAreaText >> isCodeEntry [

	^ self languageInfo isNil not
]

{ #category : #'keyboard shortcuts' }
PEEditAreaText >> keyStroke: aKeyboardEvent [

	| key shortcutMap |
	(aKeyboardEvent controlKeyPressed and: [aKeyboardEvent shiftPressed]) ifFalse: [^ false].
	key := aKeyboardEvent keyCharacter.
	shortcutMap := (Dictionary newFrom: {
		$C -> #generatePolyglotCodeBox.
		$E -> #generatePolyglotEvaluate.
		$X -> #generatePolyglotExport.
		$I -> #generatePolyglotImport.
		$S -> #searchForCode}).
	shortcutMap keysAndValuesDo: [:character :symbol |
		"The second condition is here to support Squeak on Windows and GraalSqueak (on all platforms). For some reason, the keyCharacter corresponds to the position in the alphabet when pressing shift and control on these platforms."
		(key = character or: [key asInteger = (character digitValue - 9)])
			ifTrue: [self triggerEvent: symbol. ^ true]].
	
	^ false
]

{ #category : #accessing }
PEEditAreaText >> languageInfo [

	^ languageInfo
]

{ #category : #accessing }
PEEditAreaText >> languageInfo: aDictionary [

	languageInfo := aDictionary.
	self textStyler languageInfo: languageInfo
]

{ #category : #accessing }
PEEditAreaText >> lineEndings [

	^ lineEndings
]

{ #category : #accessing }
PEEditAreaText >> lineEndings: aString [

	lineEndings := aString
]

{ #category : #public }
PEEditAreaText >> lineEndingsName [

	^ (#(multiple unknown) includes: lineEndingsSymbol) 
		ifFalse: [lineEndingsSymbol asUppercase]
		ifTrue: [lineEndingsSymbol]
]

{ #category : #public }
PEEditAreaText >> openFile [

	self fileEntry size > 1000000 ifTrue: [UIManager inform: 'The file is too big to open it.'. ^ self].
	
	self languageInfo: (PELanguageInfo forExtension: self fileEntry extension ifNone: [nil]).
	self selection: nil.
	self code: (self addCodeBoxes: self fileEntry read)
]

{ #category : #accessing }
PEEditAreaText >> privateSelection: anInterval. [

	"This is only used by ToolBuilder. If we were to use >>selection:, we'd cause an infinite loop"
	selection := anInterval
]

{ #category : #public }
PEEditAreaText >> replaceSelectionWith: aText [
	"replaces the currently selected text by aText. If nothing is selected, then the text is inserted at the cursor"

	self unsavedCode: (self unsavedCode asText
		copyReplaceFrom: self selection start 
		to: self selection stop 
		with: aText).
	self changed: #unsavedCode.
	self selection: (self selection start to: (self selection stop + aText size))
]

{ #category : #'code actions' }
PEEditAreaText >> save [

	| entry files |
	entry := self fileEntry.
	entry ifNil: [^ self].
		
	entry supportsPolyglot
		ifTrue: [
			files := PECodeBoxReadWriter
				getFilesToSaveFrom: self code
				path: entry fullPath
				id: self languageInfo id
				lineEndings: self lineEndings.
	
			files keysAndValuesDo: [:path :content | | file |
				[file := FileDirectory default forceNewFileNamed: path] ifError: [ | snippetDirectoryPath snippetDirectoryFileEntryDirectory |
					"containing folder does not exist. This happens when the snippet directory is newly created."
					snippetDirectoryPath := FileDirectory dirPathFor: path.
					FileDirectory splitName: snippetDirectoryPath to: [:existingDirectoryPath :snippetFolderName |
						(FileDirectory on: existingDirectoryPath) createDirectory: snippetFolderName].
					file := FileDirectory default forceNewFileNamed: path.
					snippetDirectoryFileEntryDirectory := PEFileEntryFSDirectory forPath: snippetDirectoryPath.
					self triggerEvent: #newSnippetDirectory with: snippetDirectoryFileEntryDirectory.]. 
				file truncate;
					nextPutAll: content]]
		ifFalse: [entry write: self code].
	
	self triggerEvent: #save
]

{ #category : #'code actions' }
PEEditAreaText >> saveUnsavedCode [

	self code: self unsavedCode.
	self save
]

{ #category : #accessing }
PEEditAreaText >> selection [

	^ selection
]

{ #category : #accessing }
PEEditAreaText >> selection: anInterval [

	self privateSelection: anInterval.
	self changed: #selection
]

{ #category : #'code actions' }
PEEditAreaText >> setupCodeBoxEventsFor: aPCCodeBox [

	aPCCodeBox
		"Triggering the change event activates the hasUnacceptedEdits marker"
		when: #codeBoxChanged
		send: #changed:
		to: self
		with: #unsavedCode;
		
		"If another code box is added within this code box, setup events for that code box as well"
		when: #codeBoxAdded
		send: #setupCodeBoxEventsFor:
		to: self
]

{ #category : #accessing }
PEEditAreaText >> textStyler [

	^ textStyler
]

{ #category : #accessing }
PEEditAreaText >> textStyler: aSHTextStyler [

	textStyler := aSHTextStyler
]

{ #category : #private }
PEEditAreaText >> triggerFocusEvent [

	self triggerEvent: #focus
]

{ #category : #accessing }
PEEditAreaText >> unsavedCode [

	^ unsavedCode
]

{ #category : #accessing }
PEEditAreaText >> unsavedCode: aText [

	unsavedCode := aText asText.
	self extractLineEndings
]
