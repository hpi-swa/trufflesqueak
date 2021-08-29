Class {
	#name : #PNBCodeCellContainer,
	#superclass : #Morph,
	#instVars : [
		'executionCounter',
		'notebook',
		'bindings'
	],
	#classInstVars : [
		'validateNBJson'
	],
	#category : #'TruffleSqueak-Tools-Notebook-Core'
}

{ #category : #'instance creation' }
PNBCodeCellContainer class >> for: aNotebook [
	^ self new notebook: aNotebook; yourself
]

{ #category : #nbformat }
PNBCodeCellContainer class >> isValidNBJson: aJsonString [
	validateNBJson ifNil: [
		[ {[ validateNBJson := (Polyglot eval: #python string: 'import nbformat
def is_valid_nb_json(nb_json):
  try:
    nbformat.validate(nbformat.reads(nb_json, 4)) # use version 4
    return True
  except:
    return False
is_valid_nb_json') ]} do: [ :block | block value ]
				displayingProgress: [ :block | 'Initializing nbformat in Python...' ]
				every: 100 ] on: Error do: [ :e |
					self error: 'nbformat does not seem to be installed (`pip install nbformat`).'.
					^ false ] ].
	^ validateNBJson value: aJsonString interopAsString "interopAsString hack: this might not be needed after 21.2"
]

{ #category : #nbformat }
PNBCodeCellContainer class >> offerToValidateOutput: aJsonString [
	(self confirm: 'Would you like to validate the output with nbformat?') ifTrue: [
		(self isValidNBJson: aJsonString)
			ifTrue: [ self inform: 'Validation with nbformat succeeded!' ]
			ifFalse: [ self error: 'Validation with nbformat failed!' ] ]
]

{ #category : #helpers }
PNBCodeCellContainer >> addCodeCell [
	| codeCell  |
	codeCell := PNBCodeCell for: self.
	self addMorphBack: codeCell.
	self changed: #submorphs
]

{ #category : #helpers }
PNBCodeCellContainer >> addCodeCellFor: cell [
	| codeCell |
	codeCell := (PNBCodeCell for: self)
		languageID: (cell metadata polyjus_language ifNil: [nil] ifNotNil: [:a | a asSymbol]) ;
		outputs: cell outputs;
		yourself.
	codeCell editor contents: cell source join.
	codeCell label refresh.
	self addMorphBack: codeCell.
	self changed: #submorphs
]

{ #category : #serialization }
PNBCodeCellContainer >> asJson [

	| json  kernelspec languageInfo metadata cellsJson |
	json := JsonObject new.

	cellsJson := self submorphs collect: [:cell | cell asJson].
	
	languageInfo := JsonObject new
			at: 'codemirror_mode' put: (JsonObject new
				at: 'name' put: 'ipython';
				at: 'version' put: 3;
				yourself);
			at: 'file_extension' put: '.py';
			at: 'mimetype' put: 'text/x-python';
			at: 'name' put: 'python';
			at: 'nbconvert_exporter' put: 'python';
			at: 'pygments_lexer' put: 'ipython3';
			at: 'version' put: '3.5.2';
			yourself.
	
	kernelspec := JsonObject new
			at: 'display_name' put: 'Python 3';
			at: 'language' put: 'python';
			at: 'name' put: 'python3';
			yourself.
	
	metadata := JsonObject new
		at: 'kernelspec' put: kernelspec;
		at: 'language_info' put: languageInfo;
		yourself.
	
	json at: 'cells' put: cellsJson.
	json
		at: 'metadata' put: metadata;
		at: 'nbformat' put: 4;
		at: 'nbformat_minor' put: 2.
	
	^ json
]

{ #category : #accessing }
PNBCodeCellContainer >> bindings [

	^ bindings ifNil: [ bindings := PNBKeyValueStore new ]
]

{ #category : #initialization }
PNBCodeCellContainer >> defaultBorderColor [
	^ Color fromString: '#CCCCCC'
]

{ #category : #initialization }
PNBCodeCellContainer >> defaultBorderWidth [
	^ 0 "1"
]

{ #category : #initialization }
PNBCodeCellContainer >> defaultBounds [
	^ 0 @ 0 corner: 600 @ 300
]

{ #category : #initialization }
PNBCodeCellContainer >> defaultColor [
	^ Color white
]

{ #category : #helpers }
PNBCodeCellContainer >> incrementExecutionCounter [
	^ executionCounter := (executionCounter ifNil: [ executionCounter := 0 ]) + 1
]

{ #category : #testing }
PNBCodeCellContainer >> indicateKeyboardFocus [
	^ false
]

{ #category : #initialization }
PNBCodeCellContainer >> initialize [
	super initialize.
	self layoutPolicy: TableLayout new;
	  listDirection: #topToBottom;
	  hResizing: #spaceFill;
	  vResizing: #shrinkWrap;
        cellInset: 4;
	  layoutInset: 0.
	  "rubberBandCells: true".
	self addCodeCell

]

{ #category : #actions }
PNBCodeCellContainer >> load [

	| fileChooser stream |
	fileChooser := FileChooser new.
	fileChooser initializeAsSystemWindowWithCaptionPane.
	fileChooser setCaption: 'Select a Juypter notebook file'.
	fileChooser setSuffixes: {'ipynb'}.
	stream := fileChooser open.
	stream ifNotNil: [self loadFromJson: stream]
]

{ #category : #serialization }
PNBCodeCellContainer >> loadFromJson: aJsonReadStream [
	self resetCells.
	(Json readFrom: aJsonReadStream) cells do: [:cell | self addCodeCellFor: cell ]
]

{ #category : #accessing }
PNBCodeCellContainer >> notebook [

	^ notebook
]

{ #category : #accessing }
PNBCodeCellContainer >> notebook: anObject [

	notebook := anObject.
]

{ #category : #helpers }
PNBCodeCellContainer >> resetCells [
	self removeAllMorphs
]

{ #category : #helpers }
PNBCodeCellContainer >> resetExecutionCounter [
	^ executionCounter := 0
]

{ #category : #actions }
PNBCodeCellContainer >> runAll [
	self submorphs do: [ :cell | cell run ]  displayingProgress: [ :cell | 'Executing cells...' ]
]

{ #category : #actions }
PNBCodeCellContainer >> save [
	| stream fName json |
	json := self asJson.
	fName := UIManager default saveFilenameRequest:'Please enter the name' translated initialAnswer: 'polyglot-notebook.ipynb'.
	(Polyglot isPermitted: #python) ifTrue: [ self class offerToValidateOutput: json asJsonString ].
	stream := FileDirectory default forceNewFileNamed: fName.
	json jsonWriteOn: stream
]

{ #category : #'stepping and presenter' }
PNBCodeCellContainer >> step [
	self width: self owner width
]

{ #category : #testing }
PNBCodeCellContainer >> stepTime [
	^ 10
]
