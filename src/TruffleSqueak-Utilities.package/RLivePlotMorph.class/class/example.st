examples
example
	"self example"
	^ (self on: ((0 to: 360 - 1)
				collect: [:x | (100.0 * x degreesToRadians sin) asInteger])) openInHand