serialization
outputsAsJson
	^ output
		ifNil: [ #() ]
		ifNotNil: [ {output asJson} ]