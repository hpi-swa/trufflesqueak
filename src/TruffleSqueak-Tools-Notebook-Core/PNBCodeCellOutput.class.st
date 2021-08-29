Class {
	#name : #PNBCodeCellOutput,
	#superclass : #BorderedMorph,
	#instVars : [
		'codeCell',
		'result'
	],
	#category : 'TruffleSqueak-Tools-Notebook-Core'
}

{ #category : #serialization }
PNBCodeCellOutput >> asJson [
	^ result isMorph 
		ifTrue: [JsonObject newFrom: {
			'output_type' -> 'display_data'.
			'metadata' -> JsonObject new.
			'data' -> (JsonObject newFrom: {'image/png' -> (self morphToPNGBase64: result)})
		}] 
		ifFalse: [JsonObject newFrom: {
			'output_type' -> 'execute_result'.
			'metadata' -> JsonObject new.
			'data' -> (JsonObject newFrom: {'text/plain' -> result asString})
		}] 
]

{ #category : #accessing }
PNBCodeCellOutput >> codeCell [

	^ codeCell
]

{ #category : #accessing }
PNBCodeCellOutput >> codeCell: anObject [

	codeCell := anObject.
]

{ #category : #initialization }
PNBCodeCellOutput >> defaultColor [
	^ Color transparent
]

{ #category : #actions }
PNBCodeCellOutput >> displayResult: anObject [
	self removeAllMorphs.
	self
		vResizing: #rigid;
		layoutPolicy: ProportionalLayout new.
	result := anObject.
	anObject isString ifTrue: [ | morph |
		morph := anObject asString asText asMorph readOnly: true; yourself.
		self addMorph: morph fullFrame: (LayoutFrame fractions: (0@0 corner: 1@1)).
		self addCornerGrips.
		^ self ].
	anObject isMorph ifTrue: [
		self
			vResizing: #shrinkWrap;
			layoutPolicy: TableLayout new.
		(anObject isKindOf: RPlotMorph)
			ifTrue: [
				"RPlotMorph does not need flex shell, it redraws itself on resize"
				self addMorph: (anObject hResizing: #spaceFill; yourself) ]
			ifFalse: [ self addMorph: (anObject addFlexShell hResizing: #spaceFill; yourself) ].
		^ self ].
	"Add support for images, ..."
	self addMorph: (PolyglotNotebook explorerMorphFor: anObject) fullFrame: (LayoutFrame fractions: (0@0 corner: 1@1)).
	self addCornerGrips.
	
]

{ #category : #initialization }
PNBCodeCellOutput >> initialize [
	super initialize.
	self layoutPolicy: ProportionalLayout new;
		hResizing: #spaceFill;
		vResizing: #rigid;
		borderWidth: 4;
		borderColor: (Color fromString: '#F5F5F5')
]

{ #category : #actions }
PNBCodeCellOutput >> morphToPNGBase64: aMorph [
	| stream |
	(aMorph isKindOf: PNBImageMorph)
		ifTrue: [ stream := (FileStream readOnlyFileNamed: aMorph fileName) binary ];
		ifFalse: [
			stream := ReadWriteStream with: #().	
			PNGReadWriter putForm: aMorph imageForm onStream: stream].
	^ (Base64MimeConverter mimeEncode: stream multiLine: false) contents
]
