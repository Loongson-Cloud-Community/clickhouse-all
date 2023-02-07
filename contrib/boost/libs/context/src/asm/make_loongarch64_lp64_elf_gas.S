/*
   Distributed under the Boost Software License, Version 1.0.
      (See accompanying file LICENSE_1_0.txt or copy at
          http://www.boost.org/LICENSE_1_0.txt)
*/

.file "make_loongarch64_lp64_elf_gas.S"
.text
.globl make_fcontext
.align 3
.type make_fcontext,@function
make_fcontext:
	# shift address in A0 to lower 16 byte boundary
    bstrins.d $a0, $r0, 3, 0

	# reserve space for context-data on context-stack
	addi.d	$a0, $a0, -160

	# third arg of make_fcontext() == address of context-function
	st.d	$a2, $a0, 152

	# save address of finish as return-address for context-function
	# will be entered after context-function returns
	la.local	$t8, finish	
	st.d	$t8, $a0, 144

	# return pointer to context-data
	jr	$ra 

finish:
	# reload our gp register (needed for la)
	addi.d	$t0, $sp, -160

	# call _exit(0)
	#  the previous function should have left the 16 bytes incoming argument
	#  area on the stack which we reuse for calling _exit
	la.global	$t8, _exit
	move	$a0, $zero
	jr	$t8
.size make_fcontext, .-make_fcontext

/* Mark that we don't need executable stack.  */
.section .note.GNU-stack,"",%progbits
