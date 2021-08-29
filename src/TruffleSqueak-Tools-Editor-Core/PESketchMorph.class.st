"
I am just like the SketchMorph except when I am asked to keep my aspect ratio: In contrast to a SketchMorph, I will keep my contents within my bounds.
"
Class {
	#name : #PESketchMorph,
	#superclass : #SketchMorph,
	#category : 'TruffleSqueak-Tools-Editor-Core'
}

{ #category : #accessing }
PESketchMorph >> scalePoint: aPoint [

	scalePoint := self keepAspectRatio
		ifTrue: [aPoint min: aPoint transposed]
		ifFalse: [aPoint].
	self layoutChanged.
]
