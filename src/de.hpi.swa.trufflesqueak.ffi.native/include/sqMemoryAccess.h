/*
 * Copyright (c) 2023-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2023-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
#define SIZEOF_VOID_P 8

//////////// from here on: copied from
//////////// https://github.com/OpenSmalltalk/opensmalltalk-vm/blob/ec421b99cf41fc5f2f5fb734b536d6233cdde809/platforms/Cross/vm/sqMemoryAccess.h

#ifndef SIZEOF_LONG
#  if LLP64
#    define SIZEOF_LONG 4
#  else
#    define SIZEOF_LONG SIZEOF_VOID_P /* default is sizeof(long)==sizeof(void *) */
#  endif
#endif

#if (SQ_VI_BYTES_PER_WORD == 4)
# define SQ_IMAGE32 1
# define SQ_IMAGE64 0
#else
# define SQ_IMAGE64 1
# define SQ_IMAGE32 0
#endif

#if (SQ_IMAGE64 || SPURVM)
# define OBJECTS_64BIT_ALIGNED 1
# define OBJECTS_32BIT_ALIGNED 0
#else
# define OBJECTS_32BIT_ALIGNED 1
# define OBJECTS_64BIT_ALIGNED 0
#endif

#if (SIZEOF_VOID_P == 4)
# define SQ_HOST32 1
#elif (SIZEOF_VOID_P == 8)
# define SQ_HOST64 1
#else
# error host is neither 32- nor 64-bit?
#endif

/* sqInt is a signed integer with size adequate for holding an Object Oriented Pointer (or immediate value)
  - that is 32bits long on a 32bits image or 64bits long on a 64bits image
  we could use C99 int32_t and int64_t once retiring legacy compiler support this time has not yet come
  usqInt is the unsigned flavour
  SQABS is a macro for taking absolute value of an sqInt */
#if SQ_IMAGE32
  typedef int		sqInt;
  typedef unsigned int	usqInt;
#define PRIdSQINT "d"
#define PRIuSQINT "u"
#define PRIxSQINT "x"
#define PRIXSQINT "X"
# define SQABS abs
#elif SQ_HOST64 && (SIZEOF_LONG == 8)
  typedef long		sqInt;
  typedef unsigned long	usqInt;
#define PRIdSQINT "ld"
#define PRIuSQINT "lu"
#define PRIxSQINT "lx"
#define PRIXSQINT "lX"
# define SQABS labs
#elif (SIZEOF_LONG_LONG != 8)
#   error long long integers are not 64-bits wide?
#else
  typedef long long		sqInt;
  typedef unsigned long long	usqInt;
#define PRIdSQINT "lld"
#define PRIuSQINT "llu"
#define PRIxSQINT "llx"
#define PRIXSQINT "llX"
# define SQABS llabs
#endif

/* sqLong is a signed integer with at least 64bits on both 32 and 64 bits images
   usqLong is the unsigned flavour
   SQLABS is a macro for taking absolute value of a sqLong */
#if !defined(sqLong)
#  if SIZEOF_LONG == 8
#     define sqLong long
#     define usqLong unsigned long
#     define SQLABS labs
#  elif _MSC_VER
#     define sqLong __int64
#     define usqLong unsigned __int64
#     define SQLABS llabs
#  else
#     define sqLong long long
#     define usqLong unsigned long long
#     define SQLABS llabs
#  endif
#endif /* !defined(sqLong) */

/* sqIntptr_t is a signed integer with enough bits to hold a pointer
   usqIntptr_t is the unsigned flavour
   this is essentially C99 intptr_t and uintptr_t but we support legacy compilers
   the C99 printf formats macros are also defined with SQ prefix */
#if SIZEOF_LONG == SIZEOF_VOID_P
typedef long sqIntptr_t;
typedef unsigned long usqIntptr_t;
#define PRIdSQPTR "ld"
#define PRIuSQPTR "lu"
#define PRIxSQPTR "lx"
#define PRIXSQPTR "lX"
#else
typedef long long sqIntptr_t;
typedef unsigned long long usqIntptr_t;
#define PRIdSQPTR "lld"
#define PRIuSQPTR "llu"
#define PRIxSQPTR "llx"
#define PRIXSQPTR "llX"
#endif
