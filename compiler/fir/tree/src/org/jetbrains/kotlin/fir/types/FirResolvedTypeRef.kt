/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirAbstractElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.VisitedSupertype
import org.jetbrains.kotlin.fir.visitors.FirVisitor

abstract class FirResolvedTypeRef(
    session: FirSession,
    psi: PsiElement?,
    val type: ConeKotlinType
) : FirTypeRef, FirAbstractElement(session, psi) {

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitResolvedTypeRef(this, data)
}

abstract class FirResolvedFunctionTypeRef(
    session: FirSession,
    psi: PsiElement?,
    type: ConeKotlinType
) : @VisitedSupertype FirResolvedTypeRef(session, psi, type), FirFunctionTypeRef {
    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitResolvedFunctionTypeRef(this, data)
}

inline fun <reified T : ConeKotlinType> FirTypeRef.coneTypeUnsafe() = (this as FirResolvedTypeRef).type as T
inline fun <reified T : ConeKotlinType> FirTypeRef.coneTypeSafe() = (this as? FirResolvedTypeRef)?.type as? T
