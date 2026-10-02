package com.example.unuseddataclass

import com.intellij.codeInsight.highlighting.ReadWriteAccessDetector
import com.intellij.codeInsight.highlighting.ReadWriteAccessDetector.Access
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtVisitorVoid

class UnusedDataClassPropertyInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : KtVisitorVoid() {
            override fun visitClass(klass: KtClass) {
                if (!klass.isData()) return
                for (param in klass.primaryConstructorParameters) {
                    if (!param.hasValOrVar()) continue
                    val isVar = param.valOrVarKeyword?.node?.elementType == KtTokens.VAR_KEYWORD
                    if (isUsed(param, isVar)) continue
                    val id = param.nameIdentifier ?: continue
                    val what = if (isVar) "read or written" else "read"
                    holder.registerProblem(
                        id,
                        "Property '${param.name}' is never $what",
                        ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                    )
                }
            }
        }

    private fun isUsed(param: KtParameter, isVar: Boolean): Boolean {
        // Search the property itself plus every base member it overrides: a use through a
        // supertype (foo.periodo via the interface) resolves to the base declaration, not the
        // override, so we must count those too. If none is referenced, the override is truly dead.
        // Any reference counts (including inside the declaring class); only serializer accesses are ignored.
        for (target in declarationsToSearch(param)) {
            val detector = ReadWriteAccessDetector.findDetector(target)
            for (ref in ReferencesSearch.search(target).findAll()) {
                if (isInsideSerializer(ref.element)) continue
                when (detector?.getReferenceAccess(target, ref) ?: Access.Read) {
                    Access.Read, Access.ReadWrite -> return true
                    Access.Write -> if (isVar) return true
                }
            }
        }
        return false
    }

    /** The property, plus (for overrides) all overridden base declarations, resolved via the K2 Analysis API. */
    private fun declarationsToSearch(param: KtParameter): List<PsiElement> {
        if (!param.hasModifier(KtTokens.OVERRIDE_KEYWORD)) return listOf(param)
        val targets = mutableListOf<PsiElement>(param)
        analyze(param) {
            val property = (param.symbol as? KaValueParameterSymbol)?.generatedPrimaryConstructorProperty
            property?.allOverriddenSymbols?.forEach { it.psi?.let(targets::add) }
        }
        return targets
    }

    // ponytail: text heuristic on "KSerializer" — covers `class FooSerializer : KSerializer<Foo>`.
    // Ceiling: renamed typealias/import escape it; upgrade path = resolve the supertype via Analysis API.
    private fun isInsideSerializer(el: PsiElement): Boolean {
        var cls = PsiTreeUtil.getParentOfType(el, KtClassOrObject::class.java)
        while (cls != null) {
            if (cls.superTypeListEntries.any { it.text.contains("KSerializer") }) return true
            cls = PsiTreeUtil.getParentOfType(cls, KtClassOrObject::class.java)
        }
        return false
    }
}
