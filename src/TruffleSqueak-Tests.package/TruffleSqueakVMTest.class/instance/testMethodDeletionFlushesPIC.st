testing
testMethodDeletionFlushesPIC
	| dummySuper dummySub instance |

	"Setup the class hierarchy dynamically"
	dummySuper := Object subclass: #TruffleSqueakPICDummySuper
		instanceVariableNames: ''
		classVariableNames: ''
		poolDictionaries: ''
		category: 'TruffleSqueak-Tests'.

	dummySub := dummySuper subclass: #TruffleSqueakPICDummySub
		instanceVariableNames: ''
		classVariableNames: ''
		poolDictionaries: ''
		category: 'TruffleSqueak-Tests'.

	[
		"Compile superclass fallback behavior"
		dummySuper compile: 'testMessage ^ ''super'''.

		"Compile subclass specific behavior"
		dummySub compile: 'testMessage ^ ''sub'''.

		"Compile the call site. The PIC will be built here."
		dummySuper compile: 'callTestMessage ^ self testMessage'.

		instance := dummySub new.

		"Run it 5,000 times to ensure Graal compiles the call site and builds the IC"
		1 to: 5000 do: [ :i |
			self assert: 'sub' equals: instance callTestMessage.
		].

		"Deleting this should signal the TruffleSqueak VM to invalidate the assumption protecting the inline cache at `callTestMessage`."
		dummySub removeSelector: #testMessage.

		"If the PIC was NOT flushed, this might still return 'sub' (stale cache).
		 If it WAS properly flushed, it will re-evaluate and find the superclass method."
		self assert: 'super' equals: instance callTestMessage.

	] ensure: [
		dummySub removeFromSystem.
		dummySuper removeFromSystem.
	]