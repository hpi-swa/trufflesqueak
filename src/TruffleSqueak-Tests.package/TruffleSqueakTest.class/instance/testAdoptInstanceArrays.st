testing
testAdoptInstanceArrays
	{ByteArray. DoubleByteArray. WordArray. DoubleWordArray}
		combinations: 2 atATimeDo: [ :each | | instance |
			instance := each second withAll: (1 to: 8).
			each first adoptInstance: instance.
			self deny: instance size = 8.
			each second adoptInstance: instance.
			self assert: ((1 to: 8) hasEqualElements: instance) ]