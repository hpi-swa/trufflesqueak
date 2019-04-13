serialization
asJson
	^ result isMorph 
		ifTrue: [JsonObject newFrom: {
			'output_type' -> 'display_data'.
			'metadata' -> JsonObject new.
			'data' -> (JsonObject newFrom: {'image/png' -> (self morphToPNGBase64: result)})
		}] 
		ifFalse: [JsonObject newFrom: {
			'output_type' -> 'execute_result'.
			'metadata' -> JsonObject new.
			'data' -> (JsonObject newFrom: {'text/plain' -> result asString})
		}] 