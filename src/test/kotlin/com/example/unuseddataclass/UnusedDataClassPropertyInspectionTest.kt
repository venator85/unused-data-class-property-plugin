package com.example.unuseddataclass

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class UnusedDataClassPropertyInspectionTest : BasePlatformTestCase() {

    private fun check(@org.intellij.lang.annotations.Language("kotlin") text: String) {
        myFixture.enableInspections(UnusedDataClassPropertyInspection::class.java)
        myFixture.configureByText("a.kt", text)
        myFixture.checkHighlighting()
    }

    // 'id' read only by the custom serializer -> flagged; 'name' read by Consumer -> ok.
    fun testUnusedFlaggedUsedNot() = check(
        """
        interface KSerializer<T>

        data class User(
            val <warning descr="Property 'id' is never read">id</warning>: Int,
            val name: String,
        )

        class Consumer(private val u: User) {
            fun label() = u.name
        }

        class UserSerializer : KSerializer<User> {
            fun serialize(value: User) {
                println(value.id) // read by the serializer: must NOT count
            }
        }
        """.trimIndent()
    )

    // override read only through the interface (foo.periodo): resolves to the base member,
    // so the base-hierarchy search must find it and NOT flag the override.
    fun testOverrideUsedViaSupertypeNotFlagged() = check(
        """
        interface Foo {
            val periodo: String
        }

        data class FooData(override val periodo: String = "") : Foo

        fun use(foo: Foo) {
            println(foo.periodo)
        }
        """.trimIndent()
    )

    // override of a member nobody reads anywhere -> genuinely dead -> flagged.
    fun testDeadOverrideFlagged() = check(
        """
        interface Foo {
            val periodo: String
        }

        data class FooData(
            override val <warning descr="Property 'periodo' is never read">periodo</warning>: String = "",
        ) : Foo
        """.trimIndent()
    )

    // var written by another class -> used; var never touched -> flagged.
    fun testVarWriteCounts() = check(
        """
        data class Box(
            var used: Int,
            var <warning descr="Property 'unused' is never read or written">unused</warning>: Int,
        )

        fun mutate(b: Box) { b.used = 1 }
        """.trimIndent()
    )

    // read only inside the declaring class -> still used, must NOT be flagged.
    fun testInternalUseNotFlagged() = check(
        """
        data class Bar(val codice: String) {
            fun a() {
                println(codice)
            }
        }
        """.trimIndent()
    )
}
