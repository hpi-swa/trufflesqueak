Extension { #name : #ByteArray }

{ #category : #'*TruffleSqueak-Core-arrays' }
ByteArray >> interopReadArrayElement: index [
	| value |
	value := self byteAt: index + 1.
	^ value <= 128
		ifTrue: [ value ]
		ifFalse: [ value - 256 ]
]
