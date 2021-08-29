Class {
	#name : #JavaImageMorph,
	#superclass : #Morph,
	#instVars : [
		'bufferedImage',
		'graphics2D',
		'form'
	],
	#category : 'TruffleSqueak-Utilities'
}

{ #category : #accessing }
JavaImageMorph >> bufferedImage [

	^ bufferedImage ifNil: [
		bufferedImage := TruffleSqueakUtilities primitiveFormToBufferedImage: self form ]
]

{ #category : #accessing }
JavaImageMorph >> bufferedImage: anObject [

	bufferedImage := anObject.
]

{ #category : #drawing }
JavaImageMorph >> drawOn: aCanvas [
	aCanvas translucentImage: self form at: self innerBounds origin
]

{ #category : #initialization }
JavaImageMorph >> form [
	^ form ifNil: [ form := self newForm ]
]

{ #category : #accessing }
JavaImageMorph >> graphics2D [

	^ graphics2D ifNil: [
		graphics2D := self bufferedImage getGraphics.
		graphics2D setBackground: (Java type: 'java.awt.Color') white.
		graphics2D ]
]

{ #category : #layout }
JavaImageMorph >> layoutChanged [
	self releaseCachedState.
	super layoutChanged.
]

{ #category : #initialization }
JavaImageMorph >> newForm [
	^ Form extent: self extent depth: 32
]

{ #category : #layout }
JavaImageMorph >> releaseCachedState [

	super releaseCachedState.
	form := nil.
	bufferedImage := nil.
	graphics2D := nil
]
