testing
testOSRInBlock
	"Loops in blocks that are activated only once must enter OSR code and still be able to access the closure, the receiver, and the home context (for non-local returns)."
	<timeout: 60>
	| outer block |
	outer := 0.
	block := [ :n :k | | s |
		s := 0.
		1 to: n do: [ :i |
			s := s + (i \\ k).
			outer := outer + 1 ].
		s ].
	self assert: 29999997 equals: (block value: 10000000 value: 7).
	self assert: 10000000 equals: outer.
	self assert: self equals: ([ :n | | r | 1 to: n do: [ :i | r := self ]. r ] value: 10000000).
	self assert: 10000000 equals: (self osrNonLocalReturnFromBlock: 10000000)
