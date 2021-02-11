fields - streaming
streamFieldsOn: aStream

	self
		streamBaseFieldsOn: aStream;
		streamOptionalFieldsOn: aStream;
		streamArrayElementsOn: aStream;
		streamCustomFieldsOn: aStream.