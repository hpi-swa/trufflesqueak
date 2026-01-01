/*
 * Copyright (c) 2023-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2023-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */

#ifdef _WIN32
#	define EXPORT(returnType) extern __declspec(dllexport) returnType
#else
#	define EXPORT(returnType) extern returnType
#endif
