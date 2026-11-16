private
osrNonLocalReturnFromBlock: n
	[ 1 to: n do: [ :i | i = n ifTrue: [ ^ i ] ] ] value.
	^ 0
