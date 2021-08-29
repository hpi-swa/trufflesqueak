Class {
	#name : #PEPluggableStringMorph,
	#superclass : #StringMorph,
	#instVars : [
		'labelSelector',
		'model'
	],
	#category : 'TruffleSqueak-Tools-Editor-Support'
}

{ #category : #accessing }
PEPluggableStringMorph >> getLabelSelector: aSymbol [

	labelSelector := aSymbol
]

{ #category : #accessing }
PEPluggableStringMorph >> model [
	^ model
]

{ #category : #accessing }
PEPluggableStringMorph >> model: aModel [
	
	model ifNotNil:[model removeDependent: self].
	model := aModel.
	model ifNotNil:[model addDependent: self].
]

{ #category : #updating }
PEPluggableStringMorph >> update: aSymbol [

	(labelSelector notNil and: [labelSelector == aSymbol]) 
		ifTrue: [^ self contents: (self model perform: labelSelector)].
	
	^ super update: aSymbol
]
