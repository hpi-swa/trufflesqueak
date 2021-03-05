*TruffleSqueak-Core-arrays
interopReadArrayElement: index
	| value |
	value := self byteAt: index + 1.
	^ value <= 128
		ifTrue: [ value ]
		ifFalse: [ value - 256 ]