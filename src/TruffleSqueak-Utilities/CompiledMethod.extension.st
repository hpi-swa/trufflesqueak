Extension { #name : #CompiledMethod }

{ #category : #'*TruffleSqueak-Utilities' }
CompiledMethod >> callTarget [
	^ self vmObject ifNotNil: [ :c | c callTarget ]
]
