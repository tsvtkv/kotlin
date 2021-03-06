// !DIAGNOSTICS: -IMPLEMENTATION_WITHOUT_HEADER
// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

header class C1
header class C2<A>
header class C3<B>
header class C4<D, E>
header class C5<F, G>
header class C6<H>
header class C7<I>
header class C8<J>
header class C9<K>
header class C10<L>

// MODULE: m2-jvm(m1-common)
// FILE: jvm.kt

impl typealias C1 = String
<!IMPL_TYPE_ALIAS_TO_CLASS_WITH_DECLARATION_SITE_VARIANCE!>impl typealias C2<<!UNUSED_TYPEALIAS_PARAMETER!>A<!>> = List<String><!>
<!IMPL_TYPE_ALIAS_TO_CLASS_WITH_DECLARATION_SITE_VARIANCE!>impl typealias C3<B> = List<B><!>
impl typealias C4<D, E> = MutableMap<D, E>
<!IMPL_TYPE_ALIAS_WITH_COMPLEX_SUBSTITUTION!>impl typealias C5<F, G> = MutableMap<G, F><!>
impl typealias C6<H> = MutableList<H>
<!IMPL_TYPE_ALIAS_WITH_USE_SITE_VARIANCE!>impl typealias C7<I> = MutableList<out I><!>
<!IMPL_TYPE_ALIAS_WITH_USE_SITE_VARIANCE!>impl typealias C8<<!UNUSED_TYPEALIAS_PARAMETER!>J<!>> = MutableList<*><!>
<!IMPL_TYPE_ALIAS_WITH_USE_SITE_VARIANCE!>impl typealias C9<K> = MutableList<in K><!>

typealias Tmp<K> = MutableList<K>
<!IMPL_TYPE_ALIAS_NOT_TO_CLASS!>impl typealias C10<L> = Tmp<L><!>
