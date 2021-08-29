Class {
	#name : #PEMockUIManager,
	#superclass : #MorphicUIManager,
	#instVars : [
		'informed'
	],
	#classVars : [
		'Active'
	],
	#category : 'TruffleSqueak-Tools-Editor-Tests'
}

{ #category : #'as yet unclassified' }
PEMockUIManager class >> initialize [

	super initialize.
	Active := false.
]

{ #category : #'as yet unclassified' }
PEMockUIManager class >> isActiveManager [
	
	^ Active
]

{ #category : #'as yet unclassified' }
PEMockUIManager class >> useDuring: aBlock [

	"Use myself as UIManager for the duration of aBlock. For the duration, set myself as active via Active and >>isActiveManager (used by UIManager getDefault). Also set the Project's cached UIManger instance to nil so that I am picked up."
	| currentUIManager |
	Active := true.
	currentUIManager := Project uiManager.
	Project current instVarNamed: 'uiManager' put: nil.
	^ [aBlock cull: UIManager default] ensure: [
		Active := false.
		Project current instVarNamed: 'uiManager' put: currentUIManager].
]

{ #category : #'as yet unclassified' }
PEMockUIManager >> hasInformed [

	^ informed
]

{ #category : #'as yet unclassified' }
PEMockUIManager >> inform: aString [

	informed := true
]

{ #category : #'as yet unclassified' }
PEMockUIManager >> initialize [

	super initialize.
	informed := false.
]
