Class {
	#name : #PNBCodeCell,
	#superclass : #Morph,
	#instVars : [
		'cellContainer',
		'languageID',
		'label',
		'editor',
		'output',
		'executionCount'
	],
	#category : 'TruffleSqueak-Tools-Notebook-Core'
}

{ #category : #'instance creation' }
PNBCodeCell class >> for: aCodeContainer [
	^ self new cellContainer: aCodeContainer; yourself
]

{ #category : #serialization }
PNBCodeCell >> asCodeJson [

	^ JsonObject new
		at: 'cell_type' put: 'code';
		at: 'metadata' put: (JsonObject new
			at: 'polyjus_language' put: self languageID asString;
			yourself);
		at: 'execution_count' put: (executionCount ifNil: [0]);
		at: 'outputs' put: self outputsAsJson;
		at: 'source' put: {self editor text};
		yourself
]

{ #category : #serialization }
PNBCodeCell >> asJson [

	self isMarkdown
		ifTrue: [^ self asMarkdownJson]
		ifFalse: [^ self asCodeJson]
]

{ #category : #serialization }
PNBCodeCell >> asMarkdownJson [

	^ JsonObject new
		at: 'cell_type' put: 'markdown';
		at: 'metadata' put: JsonObject new;
		at: 'source' put: {self editor text};
		yourself
]

{ #category : #accessing }
PNBCodeCell >> cellContainer [
	^ cellContainer
]

{ #category : #accessing }
PNBCodeCell >> cellContainer: anObject [
	cellContainer := anObject
]

{ #category : #actions }
PNBCodeCell >> down [
	| index otherCell |
	index := self cellContainer submorphs indexOf: self.
	index >= self cellContainer submorphs size ifTrue: [ ^ self ].
	self remove.
	otherCell := self cellContainer submorphs at: index.
	self cellContainer addMorph: self behind: otherCell.
	self cellContainer changed: #submorphs
]

{ #category : #accessing }
PNBCodeCell >> editor [
	^ editor ifNil: [ editor := PNBCodeCellEditor for: self ]
]

{ #category : #'initialize-release' }
PNBCodeCell >> initialize [
	super initialize.
	self
		color: Color white;
		layoutPolicy: TableLayout new;
	  	listDirection: #topToBottom;
	  	hResizing: #spaceFill;
	  	vResizing: #shrinkWrap;
		borderWidth: 2;
	  	layoutInset: 0.
	self addMorph: self editor.
	self addMorph: self label.
]

{ #category : #testing }
PNBCodeCell >> isMarkdown [
	^ self languageID = Polyglot languageIdMarkdown
]

{ #category : #accessing }
PNBCodeCell >> label [
	^ label ifNil: [ label := PNBCodeCellLabel for: self ]
]

{ #category : #accessing }
PNBCodeCell >> label: anObject [
	label := anObject
]

{ #category : #accessing }
PNBCodeCell >> languageID [
	^ languageID ifNil: [ languageID := Polyglot languageIdMarkdown ]
]

{ #category : #accessing }
PNBCodeCell >> languageID: anObject [
	languageID := anObject
]

{ #category : #accessing }
PNBCodeCell >> output [

	^ output ifNil: [
		output := PNBCodeCellOutput new.
		self addMorphBack: output.
		self changed.
		output ]
]

{ #category : #accessing }
PNBCodeCell >> outputs: outputs [
	outputs ifNotNil: [
		outputs ifNotEmpty: [ self output displayResult: outputs first ]]
]

{ #category : #serialization }
PNBCodeCell >> outputsAsJson [
	^ output
		ifNil: [ #() ]
		ifNotNil: [ {output asJson} ]
]

{ #category : #actions }
PNBCodeCell >> remove [
	cellContainer removeMorph: self.
]

{ #category : #actions }
PNBCodeCell >> run [
	| result |
	self isMarkdown ifTrue: [ ^ self ].
	result := self editor evaluate.
	executionCount := self cellContainer incrementExecutionCounter.
	result ifNotNil: [self output displayResult: result.
		self cellContainer changed: #submorphs].
		self cellContainer notebook changed: #getList.
		self cellContainer notebook showLanguageScopes ifFalse: [
			self cellContainer notebook changed: #expandRootsRequested ]
]

{ #category : #actions }
PNBCodeCell >> up [
	| index otherCell |
	index := self cellContainer submorphs indexOf: self.
	index <= 1 ifTrue: [ ^ self ].
	self remove.
	otherCell := self cellContainer submorphs at: index - 1.
	self cellContainer addMorph: self inFrontOf: otherCell.
	self cellContainer changed: #submorphs
]
