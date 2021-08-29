Class {
	#name : #PolyglotToolSet,
	#superclass : #StandardToolSet,
	#category : 'TruffleSqueak-Support'
}

{ #category : #inspecting }
PolyglotToolSet class >> basicInspect: anObject [
	^ super inspect: anObject
]

{ #category : #inspecting }
PolyglotToolSet class >> explore: anObject [

	^ PolyglotObjectExplorer openOn: anObject
]

{ #category : #inspecting }
PolyglotToolSet class >> explore: anObject label: label [

	^ PolyglotObjectExplorer openOn: anObject withLabel: label
]

{ #category : #inspecting }
PolyglotToolSet class >> inspect: anObject [

	^ PolyglotInspector openOn: anObject
]

{ #category : #inspecting }
PolyglotToolSet class >> inspect: anObject label: aString [

	^ PolyglotInspector openOn: anObject withLabel: aString
]

{ #category : #helpers }
PolyglotToolSet class >> openPolyglotEditor [
	PolyglotEditor open
]

{ #category : #helpers }
PolyglotToolSet class >> openPolyglotNotebook [
	PolyglotNotebook open
]

{ #category : #helpers }
PolyglotToolSet class >> openPolyglotWorkspace [
	PolyglotWorkspace open
]

{ #category : #setup }
PolyglotToolSet class >> register [
	ToolSet register: self
]
