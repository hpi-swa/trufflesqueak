Class {
	#name : #PELanguageInfo,
	#superclass : #Object,
	#category : 'TruffleSqueak-Tools-Editor-Core'
}

{ #category : #querying }
PELanguageInfo class >> all [

	^ self allSubclasses copyWithout: PELanguageInfoPolyglot
]

{ #category : #querying }
PELanguageInfo class >> allWithPolyglotSupport [

	^ self all select: #hasPolyglotSupport
]

{ #category : #accessing }
PELanguageInfo class >> color [

	self subclassResponsibility
]

{ #category : #accessing }
PELanguageInfo class >> defaultExtension [

	^ self extensions first
]

{ #category : #accessing }
PELanguageInfo class >> defaultLineEndings [

	self subclassResponsibility
]

{ #category : #accessing }
PELanguageInfo class >> extensions [

	self subclassResponsibility
]

{ #category : #querying }
PELanguageInfo class >> forExtension: extension [

	^ self forExtension: extension ifNone: [self error]
]

{ #category : #querying }
PELanguageInfo class >> forExtension: extension ifNone: aBlock [

	^ self all detect: [:each | each extensions includes: extension] ifNone: aBlock
]

{ #category : #querying }
PELanguageInfo class >> forId: id [

	^ self all detect: [:each | each id = id]
]

{ #category : #accessing }
PELanguageInfo class >> hasPolyglotSupport [

	^ false
]

{ #category : #accessing }
PELanguageInfo class >> id [

	self subclassResponsibility
]

{ #category : #testing }
PELanguageInfo class >> isPython [

	^ false
]

{ #category : #testing }
PELanguageInfo class >> isSmalltalk [

	^ false
]

{ #category : #accessing }
PELanguageInfo class >> languageName [

	self subclassResponsibility
]

{ #category : #accessing }
PELanguageInfo class >> rougeLexerName [

	self subclassResponsibility
]
