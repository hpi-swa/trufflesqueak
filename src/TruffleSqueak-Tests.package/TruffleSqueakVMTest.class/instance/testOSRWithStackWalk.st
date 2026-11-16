testing
testOSRWithStackWalk
	"The first stack walk for an activation happens while its OSR code runs, after frames of the method were materialized (Truffle then runs OSR code on the parent frame and restores it from the OSR arguments during stack walks)."
	<timeout: 60>
	| dummy |
	dummy := Object subclass: #TruffleSqueakOSRDummy
		instanceVariableNames: ''
		classVariableNames: ''
		poolDictionaries: ''
		category: 'TruffleSqueak-Tests'.
	[
		dummy compile: 'loop: n materialize: m walk: w
	| s |
	s := 0.
	m ifTrue: [ ^ [ :x | x ] value: 0 ].
	1 to: n do: [ :i |
		s := s + (i \\ 7).
		i = n ifTrue: [ s := s + (self senderNumArgs: w) ] ].
	^ s'.
		dummy compile: 'senderNumArgs: w
	^ w ifTrue: [ thisContext sender method numArgs ] ifFalse: [ 0 ]'.
		"Materialize a frame of #loop:materialize:walk: (creates a closure)."
		dummy new loop: 1 materialize: true walk: false.
		"Execute the send of #senderNumArgs: without walking the stack."
		3 timesRepeat: [ self assert: 1 equals: (dummy new loop: 1 materialize: false walk: false) ].
		"Long enough to enter OSR code before the stack walk at the end of the loop."
		self assert: 29999997 + 3 equals: (dummy new loop: 10000000 materialize: false walk: true)
	] ensure: [ dummy removeFromSystem ]
