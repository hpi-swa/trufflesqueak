accessing
instance
	"Answer the receiver's instance"
	^ Instance ifNil: [ Instance := self new ]