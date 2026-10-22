package com.borc.takip.ui

import com.borc.takip.calc.DebtView
import com.borc.takip.data.DebtType

fun installmentWord(v: DebtView): String =
    if (DebtType.valueOf(v.debt.type) == DebtType.CARD) "ekstre" else "taksit"
