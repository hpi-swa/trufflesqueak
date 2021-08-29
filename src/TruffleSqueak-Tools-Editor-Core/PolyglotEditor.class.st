"
I am a polyglot code editor. I can display files of several languages (with syntax highlighting), run them, and display the result.
"
Class {
	#name : #PolyglotEditor,
	#superclass : #Model,
	#instVars : [
		'exportedVariables',
		'rootDirectories',
		'snippetDirectories',
		'panes'
	],
	#classVars : [
		'AbsolutePathsForSnippets',
		'EvaluateCodeInInnerContext',
		'EvaluateSmalltalkDirectly',
		'SmalltalkCodeBoxWarningShown'
	],
	#category : 'TruffleSqueak-Tools-Editor-Core'
}

{ #category : #helpers }
PolyglotEditor class >> defaultWindowColor [

	^ self userInterfaceTheme color ifNil: [Color veryVeryLightGray]
]

{ #category : #helpers }
PolyglotEditor class >> error: error withLanguage: language [

	self error: language, ': ', (error copyFrom: 'Error: ' size + 1 to: error size)
]

{ #category : #preferences }
PolyglotEditor class >> evaluateInInnerContext [

	<preference: 'Evaluate code in an inner context'
		category: 'Polyglot Editor'
		description: 'When on, code is evaluate in an inner context and therefore does not pollute the global scope'
		type: #Boolean>
	^ EvaluateCodeInInnerContext ifNil: [false]
]

{ #category : #preferences }
PolyglotEditor class >> evaluateSmalltalkDirectly [

	<preference: 'Evaluate Smalltalk directly'
		category: 'Polyglot Editor'
		description: 'When active, running a file will evaluate Smalltalk code using Compiler>>evaluate: instead of Polyglot>>evalFile:'
		type: #Boolean>
	^ EvaluateSmalltalkDirectly ifNil: [true]
]

{ #category : #'class initialization' }
PolyglotEditor class >> initialize [
	"self initialize"
	super initialize.
	self registerInWorldMenu
]

{ #category : #'instance creation' }
PolyglotEditor class >> open [

	^ ToolBuilder open: self new
]

{ #category : #'world menu' }
PolyglotEditor class >> registerInWorldMenu [
	self environment at: #TheWorldMenu ifPresent: [ :class |
		class registerOpenCommand: (Array 
			with: 'Polyglot Editor' 
			with: (Array
				with: self
				with: #open)) ].
]

{ #category : #preferences }
PolyglotEditor class >> showSmalltalkCodeBoxWarning [

	UIManager default inform: 'When using Smalltalk code boxes, it is your responsibility to predeclare all variables the code box _exports_ at the top of the enclosing file using | anExportedVariable |. You also must end the code inside the code box with a dot ".". This warning will only be shown once.'.
	SmalltalkCodeBoxWarningShown := true.
]

{ #category : #preferences }
PolyglotEditor class >> smalltalkCodeBoxWarningShown [

	^ SmalltalkCodeBoxWarningShown ifNil: [false]
]

{ #category : #preferences }
PolyglotEditor class >> useAbsolutePathsForSnippets [

	<preference: 'Use absolute paths for snippets'
		category: 'Polyglot Editor'
		description: 'When on, snippets will use absolute paths'
		type: #Boolean>
	^ AbsolutePathsForSnippets ifNil: [true]
]

{ #category : #preferences }
PolyglotEditor class >> useAbsolutePathsForSnippets: aBoolean [

	AbsolutePathsForSnippets := aBoolean
]

{ #category : #panes }
PolyglotEditor >> activeEditArea [

	^ self panes 
		ifEmpty: [PEEditAreaNullObject new]
		ifNotEmpty: [self activePane visibleEditArea model]
]

{ #category : #panes }
PolyglotEditor >> activePane [

	^ self panes detect: [:each | each isVisibleEditAreaActive]
]

{ #category : #panes }
PolyglotEditor >> addPane [

	self panes add: (PETabbedPane new yourself).
	self panes last
		when: #activeEditAreaContentChanged evaluate: [:editArea |
			self editAreaUpdated: editArea model];
		when: #focused evaluate: [:pane | 
			self panes do: [:each | each setAllTabsToNonActive] without: pane];
		when: #closed evaluate: [:pane |
			self panes remove: pane.
			self panesChanged];
		when: #activeEditAreaGeneratePolyglotCodeBox send: #polyglotCodeBox to: self;
		when: #activeEditAreaGeneratePolyglotEvaluate send: #polyglotEvaluate to: self;
		when: #activeEditAreaGeneratePolyglotImport send: #polyglotImport to: self;
		when: #activeEditAreaGeneratePolyglotExport send: #polyglotExport to: self;
		when: #activeEditAreaSearchForCode send: #openCodeReuseTool to: self;
		when: #newSnippetDirectory send: #addSnippetDirectory: to: self.
	
	self panesChanged
]

{ #category : #'snippet directories' }
PolyglotEditor >> addSnippetDirectory: aPCFileEntryDirectory [

	snippetDirectories add: aPCFileEntryDirectory
]

{ #category : #'bottom bar' }
PolyglotEditor >> bottomBarHeight [

	^ TextStyle defaultFont height
]

{ #category : #'bottom bar' }
PolyglotEditor >> bottomBarLanguage [

	^ Text string: self currentLanguage attribute: TextAlignment rightFlush
]

{ #category : #'bottom bar' }
PolyglotEditor >> bottomBarLastModified [

	^ self activeEditArea fileEntry
		ifNil: ['']
		ifNotNil: [ | lastModified |
			lastModified := self activeEditArea fileEntry lastModified.
			lastModified asDate yyyymmdd, ' ', lastModified asTime]
]

{ #category : #'bottom bar' }
PolyglotEditor >> bottomBarLineEndings [

	^ self activeEditArea lineEndingsName
]

{ #category : #building }
PolyglotEditor >> buildEditingAreaWith: aBuilder [

	^  aBuilder pluggablePanelSpec new
		frame:  (LayoutFrame new
			leftFraction: 0.25 offset: 0;
			topFraction: 0 offset: 0;
			rightFraction: 1 offset: 0;
			bottomFraction: 1 offset: 0);
		children: {
			self buildEditingBottomBarWith: aBuilder.
			PEPluggablePanelSpec new
				wantsResizeHandles: true;
				frame: (LayoutFrame new
					leftFraction: 0 offset: 0;
					topFraction: 0 offset: 25;
					rightFraction: 1 offset: 0;
					bottomFraction: 1 offset: self bottomBarHeight negated);
				model: self;
				children: #panes;
				yourself.
			self buildEditingTopBarWith: aBuilder}
]

{ #category : #building }
PolyglotEditor >> buildEditingBottomBarWith: aBuilder [

	^ aBuilder pluggablePanelSpec new
		children: {
			aBuilder pluggableButtonSpec new
				model: self;
				frame: (0@0 corner: 0.2@1);
				label: #bottomBarLineEndings;
				help: 'Line Endings';
				action: #convertLineEndings;
				yourself.
			PEPluggableLabelSpec new
				label: #bottomBarLastModified;
				model: self;
				help: 'Last saved at';
				frame: (0.2@0 corner: 0.6@1)
				yourself.
			PEPluggableLabelSpec new
				label: #bottomBarLanguage;
				model: self;
				help: 'Current language';
				frame: (0.6@0 corner: 1@1)
				yourself};
		frame:  (LayoutFrame new
					leftFraction: 0 offset: 0;
					topFraction: 1 offset: self bottomBarHeight negated;
					rightFraction: 1 offset: 0;
					bottomFraction: 1 offset: 0);
		yourself
]

{ #category : #building }
PolyglotEditor >> buildEditingTopBarWith: aBuilder [

	| buttons |
	buttons := {
		aBuilder pluggableActionButtonSpec new
			label: 'Split Pane';
			action: #splitPane;
			model: self;
			yourself.
		aBuilder pluggableActionButtonSpec new
			label: 'Run';
			action: #runSelected;
			model: self;
			yourself}.
	Smalltalk at: #CodeReuseBuilder ifPresent: [:codeReuse |
		buttons := buttons, {aBuilder pluggableActionButtonSpec new
			label: 'Search for code...';
			action: #openCodeReuseTool;
			help: 'Shortcut: CTRL-SHIFT-S';
			model: self;
			yourself}].
	buttons := buttons, {
		aBuilder pluggableActionButtonSpec new
			label: 'Export...';
			action: #polyglotExport;
			model: self;
			help: 'Shortcut: CTRL-SHIFT-X';
			yourself.
		aBuilder pluggableActionButtonSpec new
			label: 'Import...';
			action: #polyglotImport;
			help: 'Shortcut: CTRL-SHIFT-I';
			model: self;
			yourself.
		aBuilder pluggableActionButtonSpec new
			label: 'Evaluate...';
			action: #polyglotEvaluate;
			help: 'Shortcut: CTRL-SHIFT-E';
			model: self;
			yourself.
		aBuilder pluggableActionButtonSpec new
			label: 'Code Box';
			action: #polyglotCodeBox;
			help: 'Shortcut: CTRL-SHIFT-C';
			model: self;
			yourself}.
	
	^ aBuilder pluggablePanelSpec new
		layout: #horizontal;
		children: buttons;
		frame: (LayoutFrame new leftFraction: 0 offset: 0;
					 topFraction: 0 offset: 0;
					 rightFraction: 1 offset: 0;
					 bottomFraction: 0 offset: 25)
		yourself
]

{ #category : #building }
PolyglotEditor >> buildFileTreeTopBarWith: aBuilder [

	^ aBuilder pluggablePanelSpec new
		layout: #horizontal;
		children: {
			aBuilder pluggableActionButtonSpec new
				label: 'Choose new root directory...';
				action: [self chooseNewRootDirectory];
				model: self;
				yourself.
		};
		frame: (LayoutFrame new leftFraction: 0 offset: 0;
					 topFraction: 0 offset: 0;
					 rightFraction: 1 offset: 0;
					 bottomFraction: 0 offset: 25)
		yourself
]

{ #category : #building }
PolyglotEditor >> buildFileTreeWith: aBuilder [

	^ aBuilder pluggablePanelSpec new
		frame: (LayoutFrame new
			leftFraction: 0 offset: 0;
			topFraction: 0 offset: 0;
			rightFraction: 0.25 offset: 0;
			bottomFraction: 1 offset: 0);
		children: {
			PEFileExplorerSpec new
				model: self;
				getRootDirectories: #rootDirectories;
				setSelectedFileEntry: #openFile:;
				frame:  (LayoutFrame new
					leftFraction: 0 offset: 0;
					topFraction: 0 offset: 25;
					rightFraction: 1 offset: 0;
					bottomFraction: 1 offset: 0);
				yourself.
		 self buildFileTreeTopBarWith: aBuilder}
]

{ #category : #building }
PolyglotEditor >> buildWith: aBuilder [

	| window |
	window := aBuilder build: (aBuilder pluggableWindowSpec new
		model: self;
		label: 'Polyglot Editor';
		extent: self initialEditorExtent;
		children: {
			aBuilder pluggablePanelSpec new
				wantsResizeHandles: true;
				frame: (LayoutFrame new leftFraction: 0 offset: 0;
					 topFraction: 0 offset: 0;
					 rightFraction: 1 offset: 0;
					 bottomFraction: 1 offset: 0);
				children: {
					self buildFileTreeWith: aBuilder. 
					self buildEditingAreaWith: aBuilder};
				yourself};
		yourself).
	
	^ window
]

{ #category : #'editor functions' }
PolyglotEditor >> chooseNewRootDirectory [

	| folder currentRoot |
	currentRoot := self rootDirectories detect: [:each | each isKindOf: PEFileEntryFSDirectory]
		ifFound: [:directory | directory fileDirectory]
		ifNone: [FileDirectory default].
	folder := UIManager default chooseDirectory: 'Choose a new root directory' from: currentRoot.
	folder ifNotNil: [self setNewRootDirectory: folder]
]

{ #category : #'code generation' }
PolyglotEditor >> convertLineEndings [

	| newLineEndings |
	newLineEndings := UIManager default 
		chooseFrom: (PELineEndingManager stringToIdDictionary values collect: #asUppercase) 
		values: PELineEndingManager stringToIdDictionary keys.
	newLineEndings ifNil: [^ self].
	self activeEditArea convertLineEndings: newLineEndings
]

{ #category : #'current information' }
PolyglotEditor >> currentLanguage [

	^ self currentLanguageInfo 
		ifNotNil:[:languageInfo | languageInfo languageName] 
		ifNil: ['unknown language']
]

{ #category : #'current information' }
PolyglotEditor >> currentLanguageInfo [
	^ self activeEditArea fileEntry 
		ifNil: [nil]
		ifNotNil: [:entry | PELanguageInfo forExtension: entry extension ifNone: [nil]]
]

{ #category : #panes }
PolyglotEditor >> editAreaUpdated: aPCEditArea [

	aPCEditArea = self activeEditArea ifTrue: [
		self changed: #bottomBarLanguage.
		self changed: #bottomBarLastModified.
		self changed: #bottomBarLineEndings.
		self updateWindowColor]
]

{ #category : #accessing }
PolyglotEditor >> exportedVariables [

	^ exportedVariables
]

{ #category : #accessing }
PolyglotEditor >> exportedVariables: aCollection [

	exportedVariables := aCollection
]

{ #category : #accessing }
PolyglotEditor >> fileTreeModel [

	^ fileTreeModel
]

{ #category : #accessing }
PolyglotEditor >> fileTreeModel: aPCFileExplorerModel [

	fileTreeModel := aPCFileExplorerModel
]

{ #category : #'snippet directories' }
PolyglotEditor >> findSnippetSubDirectoriesIn: aPCFileEntryDirectory upToLevel: aNumber [

	| snippetDirectories |
	snippetDirectories := Set new.
	aPCFileEntryDirectory children 
		select: [:each | each isFile not]
		thenDo: [:each |
			each name = 'snippets'
				ifTrue: [snippetDirectories add: each]
				ifFalse: [aNumber > 0 ifTrue: [
					snippetDirectories addAll: (self findSnippetSubDirectoriesIn: each upToLevel: aNumber - 1)]]].
	^ snippetDirectories
]

{ #category : #'snippet directories' }
PolyglotEditor >> findSnippetSubDirectoriesUpToLevel: aNumber [

	| snippetDirectories |
	snippetDirectories := Set new.
	self rootDirectories do: [:each |
		snippetDirectories addAll: (self findSnippetSubDirectoriesIn: each upToLevel: aNumber - 1)].
	^ snippetDirectories
]

{ #category : #'initialize-release' }
PolyglotEditor >> gitDirectories [
	Smalltalk at: #SquotWorkingCopy ifPresent: [:workingCopy |
		^ (workingCopy registered	collect: [:each | [ | gitTree |
			gitTree := each repository gitRepository repository head tree 
				asTreeEntryNamed: 'Git: ', each asString.
			PEFileEntryGitDirectory on: gitTree path: FSRelativePath new] ifError: [nil]]
		thenSelect: #notNil)].
	^ {}
]

{ #category : #panes }
PolyglotEditor >> hasActiveEditArea [

	^ self panes anySatisfy: [:each | each isVisibleEditAreaActive]
]

{ #category : #'initialize-release' }
PolyglotEditor >> initialEditorExtent [

	^ 500@400
]

{ #category : #'initialize-release' }
PolyglotEditor >> initialize [

	super initialize.
	panes := OrderedCollection new.
	self	exportedVariables: OrderedCollection new.
	
	self rootDirectories: (OrderedCollection newFrom: {PEFileEntryFSDirectory on: FileDirectory default}, self gitDirectories)
]

{ #category : #'code generation' }
PolyglotEditor >> insertCodeBox: aPCCodeBox [

	self activeEditArea addCodeBoxAtSelection: aPCCodeBox.
	self activeEditArea ensurePolyglotIsImported
]

{ #category : #menu }
PolyglotEditor >> menuBar [

	| menu |
	menu := MenuMorph new
		layoutFrame: (LayoutFrame fractions: (0@0 extent: 1@1));
		layoutPolicy: TableLayout new;
		listDirection: #leftToRight;
		layoutInset: 3@0;
		removeDropShadow;
		"vResizing: #spaceFill;
		hResizing: #shrinkWrap;
		"stayUp: true.
	
	1 to: 3 do: [:i | menu add: 'Item ', i subMenu: (MenuMorph fromArray: {{'a'. []}. {'b'. []}})].

	^ {menu}
]

{ #category : #'editor functions' }
PolyglotEditor >> openCodeReuseTool [

	ToolBuilder open: ((Smalltalk at: #CodeReuseBuilder) 
		newFor: self
		withCallback: [:instance :language :code | 
			(self currentLanguageInfo isNil or: [self currentLanguageInfo polyglotId = language])
				ifTrue: [self activeEditArea replaceSelectionWith: code asString]
				ifFalse: [ | codeBox |
					codeBox := PECodeBox new
						code: code;
						language: language;
						yourself.
					self insertCodeBox: codeBox]]
		withLabel: 'Insert code')
]

{ #category : #'editor functions' }
PolyglotEditor >> openFile: aPCFileEntry [

	(aPCFileEntry notNil and: [aPCFileEntry isFile]) ifTrue: [
		self panes
			ifEmpty: [
				self addPane.
				self panes first openFile: aPCFileEntry]
			ifNotEmpty: [self activePane openFile: aPCFileEntry]]
]

{ #category : #panes }
PolyglotEditor >> panes [

	^ panes
]

{ #category : #panes }
PolyglotEditor >> panesChanged [

	self panes withIndexDo: [:each :i | each 
		frameFractions: ((i - 1 * (1 / panes size))@0 extent: (1 / panes size)@1)].
	self changed: #panes
]

{ #category : #'code generation' }
PolyglotEditor >> polyglotCodeBox [

	| codeBox snippetFile snippetFiles |
	snippetFiles := (snippetDirectories collect: #children) asArray flatten.
	self activeEditArea fileEntry parent children
		detect: [:each | each name = 'snippets']
		ifFound: [:snippets |
			snippetFile := UIManager default 
				chooseFrom: {'new'}, (snippetFiles collect: #name)
				values: {#new}, snippetFiles.
			snippetFile ifNil: [^ self].
			codeBox := snippetFile == #new
				ifTrue: [PECodeBox new]
				ifFalse: [(PECodeBoxReadWriter codeBoxForSnippet: snippetFile) first]]
		ifNone: [codeBox := PECodeBox new].
	self insertCodeBox: codeBox
]

{ #category : #'code generation' }
PolyglotEditor >> polyglotEvaluate [
	| evaluatedCode insertedCode id languageInfos |
	
	self activeEditArea fileEntry ifNil: [UIManager inform: 'No file selected'. ^ self].
	self currentLanguageInfo polyglotId ifNil: [UIManager inform: 'No language recognized'. ^ self].
	
	languageInfos := PELanguageInfo allWithPolyglotSupport.
	
	id := UIManager 
		default chooseFrom: (languageInfos collect: #languageName)
		values: (languageInfos collect: #polyglotId) 
		title: 'Choose the target language:'.
	evaluatedCode := UIManager default request: ('Type your code').
	evaluatedCode ifEmpty: [^ self].
	insertedCode := self currentLanguageInfo ifNil: [^ self] ifNotNil: [:info |
		(info codeForPolyglotEvaluate: evaluatedCode in: id)].
	self activeEditArea replaceSelectionWith: insertedCode.
	self activeEditArea ensurePolyglotIsImported
]

{ #category : #'running code' }
PolyglotEditor >> polyglotEvaluateCurrentFile [

	| polyglot |
	self activeEditArea saveUnsavedCode.
	polyglot := Smalltalk classNamed: #Polyglot.
	^ (polyglot notNil and: (self currentLanguageInfo isSmalltalk and: PolyglotEditor evaluateSmalltalkDirectly) not)
		ifTrue: [[ | id file |
				id := self currentLanguageInfo polyglotId.
				file := self activeEditArea fileEntry fullPath.
				self class evaluateInInnerContext
					ifTrue: [ polyglot evalInInnerContext: id file: file ]
					ifFalse: [ polyglot eval: id file: file ] ] ifError: [:error |
				self class error: error withLanguage: self currentLanguage]]
		ifFalse: [Compiler evaluate: self activeEditArea fileEntry read]
]

{ #category : #'code generation' }
PolyglotEditor >> polyglotExport [
	| name value code |
	self activeEditArea fileEntry ifNil: [UIManager inform: 'No file selected'. ^ self].
	self currentLanguageInfo polyglotId ifNil: [UIManager inform: 'No language recognized'. ^ self].
	
	name := UIManager default request: 'Choose the name of the variable to export'.
	name ifEmpty: [^ self].
	value := UIManager default request: ('Choose the value of ', name).
	value ifEmpty: [^ self].
	code := self currentLanguageInfo
		ifNil: [^ self]
		ifNotNil: [:info | info codeForPolyglotExportOf: value to: name].
	self activeEditArea replaceSelectionWith: code.
	self activeEditArea ensurePolyglotIsImported.
	self exportedVariables add: name
]

{ #category : #'code generation' }
PolyglotEditor >> polyglotImport [
	| name code |
	self activeEditArea fileEntry ifNil: [UIManager inform: 'No file selected'. ^ self].
	self currentLanguageInfo polyglotId ifNil: [UIManager inform: 'No language recognized'. ^ self].
	
	name := UIManager default
		chooseFromOrAddTo: self exportedVariables
		lines: OrderedCollection new
		title: 'Choose the name of the variable to import'.
	(name isNil or: [name isEmpty]) ifTrue: [^ self].
	code := self currentLanguageInfo ifNil: [^ self] ifNotNil: [:info | info codeForPolyglotImport: name].
	self activeEditArea replaceSelectionWith: code.
	self activeEditArea ensurePolyglotIsImported
]

{ #category : #accessing }
PolyglotEditor >> rootDirectories [

	^ rootDirectories
]

{ #category : #accessing }
PolyglotEditor >> rootDirectories: aCollection [

	rootDirectories := aCollection.
	snippetDirectories := self findSnippetSubDirectoriesUpToLevel: 3.
	self changed: #rootDirectories
]

{ #category : #'running code' }
PolyglotEditor >> runSelected [

	self activeEditArea fileEntry ifNil: [UIManager inform: 'No file opened'. ^ self].
	self activeEditArea isCodeEntry ifFalse: [UIManager inform: 'Not a valid code file, cannot be run'. ^ self].
	self currentLanguageInfo polyglotId ifNil: [UIManager inform: 'No Polyglot language recognized'. ^ self].

	self activeEditArea fileEntry ifNotNil: [
		self polyglotEvaluateCurrentFile inspect]
]

{ #category : #'editor functions' }
PolyglotEditor >> setNewRootDirectory: aDirectory [

	self rootDirectories: {(PEFileEntryFSDirectory on: aDirectory)}, self gitDirectories
]

{ #category : #panes }
PolyglotEditor >> splitPane [

	| fileEntry |
	self hasActiveEditArea ifFalse: [^ self].
	fileEntry := self activeEditArea fileEntry.
	self panes do: [:each | each setAllTabsToNonActive].
	
	self addPane.
	panes last openFile: fileEntry
]

{ #category : #'window color' }
PolyglotEditor >> updateWindowColor [

	self topView setWindowColor: (self currentLanguageInfo 
		ifNotNil: [:languageInfo | languageInfo color] 
		ifNil: [self class defaultWindowColor])
]
