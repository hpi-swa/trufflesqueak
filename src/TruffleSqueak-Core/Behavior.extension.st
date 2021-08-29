Extension { #name : #Behavior }

{ #category : #'*TruffleSqueak-Core-meta-object' }
Behavior >> interopGetMetaQualifiedName [
	^ self asString
]

{ #category : #'*TruffleSqueak-Core-meta-object' }
Behavior >> interopGetMetaSimpleName [
	^ self asString
]

{ #category : #'*TruffleSqueak-Core-instantiable' }
Behavior >> interopInstantiate: arguments [
	^ arguments isEmpty
		ifTrue: [ self new ]
		ifFalse: [ | arg |
			arg := arguments first.
			(arg isInteger and: [ arg isLarge not ])
				ifTrue: [ self new: arg ]
				ifFalse: [ self new ]]
]

{ #category : #'*TruffleSqueak-Core-instantiable' }
Behavior >> interopIsInstantiable [
	^ self isImmediateClass not
]

{ #category : #'*TruffleSqueak-Core-meta-object' }
Behavior >> interopIsMetaInstance: instance [
	^ instance isKindOf: self
]

{ #category : #'*TruffleSqueak-Core-meta-object' }
Behavior >> interopIsMetaObject [
	^ true
]
