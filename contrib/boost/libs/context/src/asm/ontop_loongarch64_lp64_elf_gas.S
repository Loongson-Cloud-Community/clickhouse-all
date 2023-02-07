/*
   Distributed under the Boost Software License, Version 1.0.
      (See accompanying file LICENSE_1_0.txt or copy at
          http://www.boost.org/LICENSE
*/

.file "ontop_loongarch64_lp64_elf_gas.S"
.text
.globl ontop_fcontext
.align 3
.type ontop_fcontext,@function
ontop_fcontext:
	# reserve space on stack
	addi.d	$sp, $sp, -160

	st.d	$s0, $sp, 64	# save S0
	st.d	$s1, $sp, 72	# save S1
	st.d	$s2, $sp, 80	# save S2
	st.d	$s3, $sp, 88	# save S3
	st.d	$s4, $sp, 96	# save S4
	st.d	$s5, $sp, 104	# save S5
	st.d	$s6, $sp, 112	# save S6
	st.d	$s7, $sp, 120	# save S7
	st.d	$s8, $sp, 128	# save S8
	st.d	$fp, $sp, 136	# save FP
	st.d	$ra, $sp, 144	# save RA
	st.d	$ra, $sp, 152	# save RA as PC


	fst.d	$fs0, $sp, 0		# save F24
	fst.d	$fs1, $sp, 8		# save F25
	fst.d	$fs2, $sp, 16	# save F26
	fst.d	$fs3, $sp, 24	# save F27
	fst.d	$fs4, $sp, 32	# save F28
	fst.d	$fs5, $sp, 40	# save F29
	fst.d	$fs6, $sp, 48	# save F30
	fst.d	$fs7, $sp, 56	# save F31

	# swap a0,sp
	xor	$a0, $a0, $sp
	xor	$sp, $a0, $sp
	xor	$a0, $a0, $sp

	fld.d	$fs0, $sp, 0	# restore F24
	fld.d	$fs1, $sp, 8	# restore F25
	fld.d	$fs2, $sp, 16	# restore F26
	fld.d	$fs3, $sp, 24	# restore F27
	fld.d	$fs4, $sp, 32	# restore F28
	fld.d	$fs5, $sp, 40	# restore F29
	fld.d	$fs6, $sp, 48	# restore F30
	fld.d	$fs7, $sp, 56	# restore F31

	ld.d	$s0, $sp, 64	# restore S0
	ld.d	$s1, $sp, 72	# restore S1
	ld.d	$s2, $sp, 80	# restore S2
	ld.d	$s3, $sp, 88	# restore S3
	ld.d	$s4, $sp, 96	# restore S4
	ld.d	$s5, $sp, 104	# restore S5
	ld.d	$s6, $sp, 112	# restore S6
	ld.d	$s7, $sp, 120	# restore S7
	ld.d	$s8, $sp, 128	# restore S8
	ld.d	$fp, $sp, 136	# restore FP
	ld.d	$ra, $sp, 144	# restore RA

	# load PC
	move	$t8, $a2

	# adjust stack
	addi.d	$sp, $sp, 160

	# jump to context
	jr	$t8
.size ontop_fcontext, .-ontop_fcontext

/* Mark that we don't need executable stack.  */
.section .note.GNU-stack,"",%progbits
