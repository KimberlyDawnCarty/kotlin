/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirResolvedImport
import org.jetbrains.kotlin.fir.declarations.expandedConeType
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.calls.TowerScopeLevel
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.cast

abstract class FirAbstractImportingScope(
    session: FirSession,
    protected val scopeSession: ScopeSession,
    lookupInFir: Boolean
) : FirAbstractProviderBasedScope(session, lookupInFir) {

    private fun getStaticsScope(classId: ClassId): FirScope? {
        provider.getClassUseSiteMemberScope(classId, session, scopeSession)?.let { return it }
        val symbol = provider.getClassLikeSymbolByFqName(classId) ?: return null
        if (symbol is FirTypeAliasSymbol) {
            val expansionSymbol = symbol.fir.expandedConeType?.lookupTag?.toSymbol(session)
            if (expansionSymbol != null) {
                return getStaticsScope(expansionSymbol.classId)
            }
        }

        return null
    }

    protected fun <T : FirCallableSymbol<*>> processCallables(
        import: FirResolvedImport,
        name: Name,
        token: TowerScopeLevel.Token<T>,
        processor: (FirCallableSymbol<*>) -> ProcessorAction
    ): ProcessorAction {
        val callableId = CallableId(import.packageFqName, import.relativeClassName, name)

        val classId = import.resolvedClassId
        if (classId != null) {
            val scope = getStaticsScope(classId) ?: return ProcessorAction.NEXT


            val action = when (token) {
                TowerScopeLevel.Token.Functions -> scope.processFunctionsByName(
                    callableId.callableName,
                    processor.cast()
                )
                TowerScopeLevel.Token.Properties -> scope.processPropertiesByName(
                    callableId.callableName,
                    processor.cast()
                )
                else -> ProcessorAction.NEXT
            }
            if (action.stop()) {
                return ProcessorAction.STOP
            }
        } else if (name.isSpecial || name.identifier.isNotEmpty()) {
            val symbols = provider.getTopLevelCallableSymbols(callableId.packageName, callableId.callableName)
            if (symbols.isEmpty()) {
                return ProcessorAction.NONE
            }

            for (symbol in symbols) {
                if (processor(symbol).stop()) {
                    return ProcessorAction.STOP
                }
            }
        }

        return ProcessorAction.NEXT
    }

    abstract fun <T : FirCallableSymbol<*>> processCallables(
        name: Name,
        token: TowerScopeLevel.Token<T>,
        processor: (FirCallableSymbol<*>) -> ProcessorAction
    ): ProcessorAction

    final override fun processFunctionsByName(name: Name, processor: (FirFunctionSymbol<*>) -> ProcessorAction): ProcessorAction {
        return processCallables(
            name,
            TowerScopeLevel.Token.Functions
        ) { if (it is FirFunctionSymbol<*>) processor(it) else ProcessorAction.NEXT }
    }

    final override fun processPropertiesByName(name: Name, processor: (FirCallableSymbol<*>) -> ProcessorAction): ProcessorAction {
        return processCallables(
            name,
            TowerScopeLevel.Token.Properties
        ) { if (it is FirVariableSymbol<*>) processor(it) else ProcessorAction.NEXT }
    }

}
