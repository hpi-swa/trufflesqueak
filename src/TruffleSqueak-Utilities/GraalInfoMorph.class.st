Class {
	#name : #GraalInfoMorph,
	#superclass : #StringMorph,
	#instVars : [
		'samples'
	],
	#category : 'TruffleSqueak-Utilities'
}

{ #category : #'class initialization' }
GraalInfoMorph class >> initialize [
	"self initialize"
	super initialize.
	self registerInWorldMenu
]

{ #category : #'instance creation' }
GraalInfoMorph class >> openInHand [
	^ self new openInHand
]

{ #category : #'world menu' }
GraalInfoMorph class >> registerInWorldMenu [
	self environment at: #TheWorldMenu ifPresent: [ :class |
		class registerOpenCommand: (Array 
			with: 'Graal Info Morph'
			with: (Array
				with: self
				with: #openInHand)) ].
]

{ #category : #'event handling' }
GraalInfoMorph >> handlesMouseDown: anEvent [
	^ true
]

{ #category : #initialization }
GraalInfoMorph >> initialize [
	super initialize.
	self contents: 'Graal Queue: n/a'
]

{ #category : #'event handling' }
GraalInfoMorph >> mouseDown: anEvent [
	anEvent yellowButtonPressed ifTrue: [
		(self confirm: 'Reset samples?') ifTrue: [ samples := nil ].
		^ self ].
	(Polyglot isPermitted: #R) ifFalse: [ ^self ].
	samples ifNotNil: [ :s |
		(RLivePlotMorph on: s with: 'ggplot(values, aes(x=x, y=y, color=y)) +
		ggtitle("Graal Compilation Queue") + labs(x="time", y="size") +
		theme(axis.text.x=element_blank()) +
		geom_point(size = .5, show.legend = FALSE) +
		scale_color_gradient(low = "#0091ff", high = "#f0650e")')
			openInHand ]
]

{ #category : #accessing }
GraalInfoMorph >> samples [
	^ samples ifNil: [ samples := OrderedCollection new ]
]

{ #category : #'stepping and presenter' }
GraalInfoMorph >> step [
	| queueSize |
	queueSize := Graal runtime
		ifNil: [^ self] ifNotNil: [ :r | r getCompileQueue getQueueSize asNumber ].
	self samples add: queueSize.
	self color: (queueSize < 100 ifTrue: [ Color gray ] ifFalse: [
		queueSize > 500 ifFalse: [ Color orange thriceDarker ] ifTrue: [ Color red darker ] ]).
	self contents: 'Graal Queue: ', queueSize
]

{ #category : #testing }
GraalInfoMorph >> stepTime [
	^ 2000
]
